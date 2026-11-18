package com.framelab.app.render

import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.framelab.app.data.model.Design
import com.framelab.app.data.model.Edge
import com.framelab.app.data.model.El
import com.framelab.app.data.model.Fill
import com.framelab.app.data.model.FontKind
import com.framelab.app.data.model.PhotoFit
import com.framelab.app.data.model.SLIDE_W
import com.framelab.app.data.model.SpotShape
import com.framelab.app.data.model.TextAlignKind
import com.framelab.app.ui.theme.DmMono
import com.framelab.app.ui.theme.Inter
import com.framelab.app.ui.theme.TemplateSerif
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** How empty photo spots look. */
enum class EmptyStyle {
    /** Pastel placeholder (thumbnails, previews). */
    Pastel,
    /** Diagonal hatch + dashed outline (editor). */
    Hatch,
    /** Plain paper (export: "Empty spots save as plain paper"). */
    Paper,
}

data class RenderOptions(
    val empty: EmptyStyle = EmptyStyle.Pastel,
    /** Draw the ink "+" buttons on empty spots (editor). */
    val addButtons: Boolean = false,
    /** Draw photo-order number badges (E6 / Settings › Always show numbers). */
    val numbers: Boolean = false,
    /** Element ids not to draw (e.g. text being edited live elsewhere). */
    val hidden: Set<String> = emptySet(),
    /** Opacity applied to a locked photo's frame etc. is handled by overlays, not here. */
    val dimAlpha: Float = 1f,
)

fun Long.c(): Color = Color(this.toInt())

private val HatchA = Color(0x0D16130F)   // ink 5%
private val HatchB = Color(0x1316130F)   // composites to ink 12% over the 5% base
private val DashInk = Color(0x6116130F)  // ink 38%
private val InkC = Color(0xFF16130F)

/**
 * Draws a whole carousel in design units at the current transform (1 unit = 1 px of the scope).
 * Callers scale/translate to show one slide (see [drawSlide]).
 *
 * @param images loaded photos keyed by spot uri.
 * @param art bundled sticker bitmaps keyed by art name.
 */
fun DrawScope.drawDesign(
    design: Design,
    measurer: TextMeasurer,
    images: (String) -> ImageBitmap? = { null },
    art: (String) -> ImageBitmap? = { null },
    options: RenderOptions = RenderOptions(),
) {
    drawRect(design.background.c(), Offset.Zero, Size(design.canvasW, design.slideH))
    val order = if (options.numbers) design.spotsInFillOrder.mapIndexed { i, s -> s.id to i + 1 }.toMap() else emptyMap()
    for (el in design.elements) {
        if (el.id in options.hidden) continue
        withTransform({ rotate(el.rotation, Offset(el.x + el.w / 2f, el.y + el.h / 2f)) }) {
            when (el) {
                is El.Box -> drawBox(el)
                is El.Text -> drawTextEl(el, measurer)
                is El.Spot -> drawSpot(el, images(el.uri ?: ""), options, order[el.id], design, measurer)
                is El.Sticker -> drawSticker(el, art(el.art))
                is El.Lines -> drawLines(el)
            }
        }
    }
}

/** Draws slide [index] (0-based) of [design] scaled to fill this scope. */
fun DrawScope.drawSlide(
    design: Design,
    index: Int,
    measurer: TextMeasurer,
    images: (String) -> ImageBitmap? = { null },
    art: (String) -> ImageBitmap? = { null },
    options: RenderOptions = RenderOptions(),
) {
    val s = size.width / SLIDE_W
    clipRect(0f, 0f, size.width, size.height) {
        scale(s, s, Offset.Zero) {
            translate(-index * SLIDE_W, 0f) { drawDesign(design, measurer, images, art, options) }
        }
    }
}

/** Shape path of an element in its own unrotated local rect. */
fun spotPath(s: El.Spot, x: Float = s.x, y: Float = s.y, w: Float = s.w, h: Float = s.h): Path = Path().apply {
    when (s.shape) {
        SpotShape.Original ->
            if (s.radius > 0f) addRoundRect(RoundRect(Rect(x, y, x + w, y + h), CornerRadius(min(s.radius, min(w, h) / 2))))
            else addRect(Rect(x, y, x + w, y + h))
        SpotShape.Square -> addRect(Rect(x, y, x + w, y + h))
        SpotShape.Rounded -> addRoundRect(RoundRect(Rect(x, y, x + w, y + h), CornerRadius(min(w, h) * 0.16f)))
        SpotShape.Circle -> {
            val d = min(w, h); addOval(Rect(x + (w - d) / 2, y + (h - d) / 2, x + (w + d) / 2, y + (h + d) / 2))
        }
        SpotShape.Arch -> {
            val r = w / 2f
            moveTo(x, y + h)
            lineTo(x, y + min(r, h))
            arcTo(Rect(x, y, x + w, y + 2 * r), 180f, 180f, false)
            lineTo(x + w, y + h)
            close()
        }
    }
}

private fun boxPath(b: El.Box): Path = Path().apply {
    val r = Rect(b.x, b.y, b.x + b.w, b.y + b.h)
    when {
        b.polygon != null && b.polygon.size >= 6 -> {
            val p = b.polygon
            moveTo(b.x + p[0] * b.w, b.y + p[1] * b.h)
            var i = 2
            while (i + 1 < p.size) { lineTo(b.x + p[i] * b.w, b.y + p[i + 1] * b.h); i += 2 }
            close()
        }
        b.oval -> addOval(r)
        b.edge == Edge.Perforated -> {
            addRect(r)
            val hole = Path()
            val pitch = 10f; val rad = 2.8f
            var t = 0f
            while (t <= b.w) { hole.addOval(Rect(Offset(b.x + t, b.y), rad)); hole.addOval(Rect(Offset(b.x + t, b.y + b.h), rad)); t += pitch }
            t = 0f
            while (t <= b.h) { hole.addOval(Rect(Offset(b.x, b.y + t), rad)); hole.addOval(Rect(Offset(b.x + b.w, b.y + t), rad)); t += pitch }
            op(Path().apply { addRect(r) }, hole, PathOperation.Difference)
        }
        b.radius > 0f -> addRoundRect(RoundRect(r, CornerRadius(b.radius)))
        else -> addRect(r)
    }
}

private fun DrawScope.drawShadow(path: Path, shadow: com.framelab.app.data.model.Shadow, alpha: Float) {
    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            color = shadow.color.c().copy(alpha = shadow.color.c().alpha * alpha).toArgb()
            if (shadow.blur > 0f) maskFilter = BlurMaskFilter(shadow.blur, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.nativeCanvas.save()
        canvas.nativeCanvas.translate(shadow.dx, shadow.dy)
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
        canvas.nativeCanvas.restore()
    }
}

private fun DrawScope.drawFill(fill: Fill, path: Path, bounds: Rect, alpha: Float) {
    clipPath(path) {
        when (fill) {
            is Fill.Solid -> drawRect(fill.color.c(), bounds.topLeft, bounds.size, alpha = alpha)
            is Fill.Ruled -> {
                drawRect(fill.base.c(), bounds.topLeft, bounds.size, alpha = alpha)
                var yy = bounds.top + fill.spacing - 1f
                while (yy < bounds.bottom) {
                    drawRect(fill.rule.c(), Offset(bounds.left, yy), Size(bounds.width, 1f), alpha = alpha); yy += fill.spacing
                }
                fill.marginX?.let { drawRect(fill.margin.c(), Offset(bounds.left + it, bounds.top), Size(1f, bounds.height), alpha = alpha) }
            }
            is Fill.Stripes -> {
                drawRect(fill.base.c(), bounds.topLeft, bounds.size, alpha = alpha)
                var t = 0f
                if (!fill.vertical) {
                    while (t < bounds.width) {
                        drawRect(fill.stripe.c(), Offset(bounds.left + t, bounds.top + fill.inset), Size(fill.on, bounds.height - 2 * fill.inset), alpha = alpha)
                        t += fill.on + fill.off
                    }
                } else {
                    while (t < bounds.height) {
                        drawRect(fill.stripe.c(), Offset(bounds.left + fill.inset, bounds.top + t), Size(bounds.width - 2 * fill.inset, fill.on), alpha = alpha)
                        t += fill.on + fill.off
                    }
                }
            }
            is Fill.Linear -> {
                val cols = fill.colors.map { it.c() }
                val a = (fill.angleDeg - 90f) * PI.toFloat() / 180f
                val c = bounds.center
                val half = (kotlin.math.abs(bounds.width * cos(a)) + kotlin.math.abs(bounds.height * sin(a))) / 2f
                val start = Offset(c.x - cos(a) * half, c.y - sin(a) * half)
                val end = Offset(c.x + cos(a) * half, c.y + sin(a) * half)
                val brush = if (fill.stops != null && fill.stops.size == cols.size)
                    Brush.linearGradient(*fill.stops.zip(cols).toTypedArray(), start = start, end = end)
                else Brush.linearGradient(cols, start, end)
                drawRect(brush, bounds.topLeft, bounds.size, alpha = alpha)
            }
            is Fill.Sprockets -> {
                drawRect(fill.base.c(), bounds.topLeft, bounds.size, alpha = alpha)
                var t = (fill.pitch - fill.holeW) / 2f
                val yy = bounds.top + (bounds.height - fill.holeH) / 2f
                while (t + fill.holeW <= bounds.width) {
                    drawRect(fill.hole.c(), Offset(bounds.left + t, yy), Size(fill.holeW, fill.holeH), alpha = alpha)
                    t += fill.pitch
                }
            }
        }
    }
}

private fun DrawScope.drawBox(b: El.Box) {
    val path = boxPath(b)
    val bounds = Rect(b.x, b.y, b.x + b.w, b.y + b.h)
    b.shadow?.let { drawShadow(path, it, b.alpha) }
    b.fill?.let { drawFill(it, path, bounds, b.alpha) }
    if (b.borderColor != null && b.borderWidth > 0f) {
        val inset = b.borderWidth / 2f
        val p = if (b.oval) Path().apply { addOval(bounds.deflate(inset)) }
        else if (b.radius > 0f) Path().apply { addRoundRect(RoundRect(bounds.deflate(inset), CornerRadius(b.radius))) }
        else path
        drawPath(
            p, b.borderColor.c(), alpha = b.alpha,
            style = Stroke(b.borderWidth, pathEffect = if (b.dashed) PathEffect.dashPathEffect(floatArrayOf(4f, 3f)) else null),
        )
    }
}

fun fontFamilyOf(kind: FontKind) = when (kind) {
    FontKind.Sans -> Inter
    FontKind.Serif -> TemplateSerif
    FontKind.Mono -> DmMono
}

fun textStyleOf(t: El.Text): TextStyle = TextStyle(
    fontFamily = fontFamilyOf(t.font),
    fontSize = t.size.sp,
    lineHeight = if (t.lineHeight > 0f) t.lineHeight.sp else (t.size * 1.2f).sp,
    fontWeight = FontWeight(t.weight),
    fontStyle = if (t.italic) FontStyle.Italic else FontStyle.Normal,
    letterSpacing = t.tracking.sp,
    color = t.color.c(),
    textAlign = when (t.align) { TextAlignKind.Left -> TextAlign.Left; TextAlignKind.Center -> TextAlign.Center; TextAlignKind.Right -> TextAlign.Right },
    shadow = t.glow?.let { androidx.compose.ui.graphics.Shadow(it.c(), Offset.Zero, 6f) },
)

private fun DrawScope.drawTextEl(t: El.Text, measurer: TextMeasurer) {
    val txt = if (t.caps) t.text.uppercase() else t.text
    val layout = measurer.measure(
        AnnotatedString(txt), textStyleOf(t),
        constraints = if (t.w > 0f) Constraints.fixedWidth(t.w.toInt().coerceAtLeast(1)) else Constraints(),
        softWrap = t.w > 0f,
    )
    val dx = if (t.w > 0f) 0f else 0f
    drawText(layout, topLeft = Offset(t.x + dx, t.y))
}

/** Measures a text element's laid-out size (design units). */
fun measureText(t: El.Text, measurer: TextMeasurer): Size {
    val txt = if (t.caps) t.text.uppercase() else t.text
    val l = measurer.measure(
        AnnotatedString(txt), textStyleOf(t),
        constraints = if (t.w > 0f) Constraints.fixedWidth(t.w.toInt().coerceAtLeast(1)) else Constraints(),
        softWrap = t.w > 0f,
    )
    return Size(if (t.w > 0f) t.w else l.size.width.toFloat(), l.size.height.toFloat())
}

private fun DrawScope.drawSpot(s: El.Spot, image: ImageBitmap?, o: RenderOptions, number: Int?, design: Design, measurer: TextMeasurer) {
    val path = spotPath(s)
    val bounds = Rect(s.x, s.y, s.x + s.w, s.y + s.h)
    if (s.uri != null && image != null) {
        clipPath(path) { drawPhoto(image, s, bounds) }
    } else if (s.uri != null) {
        // Photo still loading.
        drawPath(path, s.pastel.c())
    } else when (o.empty) {
        EmptyStyle.Pastel -> drawPath(path, s.pastel.c())
        EmptyStyle.Paper -> drawPath(path, Color(0xFFFBF6EC))
        EmptyStyle.Hatch -> {
            clipPath(path) {
                drawRect(HatchA, bounds.topLeft, bounds.size)
                // 135° stripes, 7 units each.
                val len = s.w + s.h
                var t = -s.h
                val stripe = Path()
                while (t < len) {
                    stripe.reset()
                    stripe.moveTo(s.x + t, s.y); stripe.lineTo(s.x + t + 7f * 1.414f, s.y)
                    stripe.lineTo(s.x + t + 7f * 1.414f - s.h, s.y + s.h); stripe.lineTo(s.x + t - s.h, s.y + s.h); stripe.close()
                    // mirror for 135deg (top-right to bottom-left)
                    drawPath(stripe, HatchB)
                    t += 14f * 1.414f
                }
            }
            val inner = spotPath(s, s.x + 2f, s.y + 2f, s.w - 4f, s.h - 4f)
            drawPath(inner, DashInk, style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))))
        }
    }
    if (o.addButtons && s.uri == null) {
        // One + per slide the spot touches (HANDOFF E3).
        val range = s.slideRangeLocal(design)
        for (k in range) {
            val l = max(s.x, k * SLIDE_W); val r = min(s.x + s.w, (k + 1) * SLIDE_W)
            val c = Offset((l + r) / 2f, s.y + s.h / 2f)
            val big = range.first != range.last && k == range.first
            drawAddButton(c, if (big) 22f else if (min(s.w, s.h) < 90f) 15f else 16f)
        }
    }
    if (number != null) drawNumberBadge(Offset(s.x + 6f, s.y + 6f), number, measurer)
}

private fun El.Spot.slideRangeLocal(d: Design): IntRange {
    val a = (x / SLIDE_W).toInt().coerceIn(0, d.slides - 1)
    val b = ((x + w - 0.5f) / SLIDE_W).toInt().coerceIn(0, d.slides - 1)
    return a..b
}

fun DrawScope.drawAddButton(center: Offset, radius: Float) {
    drawCircle(InkC, radius, center)
    val arm = radius * 0.42f
    drawLine(Color.White, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), 1.9f, StrokeCap.Round)
    drawLine(Color.White, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), 1.9f, StrokeCap.Round)
}

/** Number badge (HANDOFF §5.22): 20 min, radius 6, ink, white 11/700. */
fun DrawScope.drawNumberBadge(topLeft: Offset, n: Int, measurer: TextMeasurer) {
    val layout = measurer.measure(
        n.toString(),
        TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 20.sp, color = Color.White),
    )
    val w = max(20f, layout.size.width + 10f)
    drawRoundRect(InkC, topLeft, Size(w, 20f), CornerRadius(6f))
    drawText(layout, topLeft = Offset(topLeft.x + (w - layout.size.width) / 2f, topLeft.y + (20f - layout.size.height) / 2f))
}

/** Draws [image] inside the spot's bounds honouring fit, zoom, pan, flip and quarter turns. */
fun DrawScope.drawPhoto(image: ImageBitmap, s: El.Spot, bounds: Rect) {
    val turned = s.turns % 2 != 0
    val iw = (if (turned) image.height else image.width).toFloat()
    val ih = (if (turned) image.width else image.height).toFloat()
    val base = if (s.fit == PhotoFit.Fill) max(bounds.width / iw, bounds.height / ih) else min(bounds.width / iw, bounds.height / ih)
    val k = base * s.zoom
    val dw = iw * k; val dh = ih * k
    val ox = max(0f, (dw - bounds.width) / 2f); val oy = max(0f, (dh - bounds.height) / 2f)
    val cx = bounds.center.x - s.panX * ox
    val cy = bounds.center.y - s.panY * oy
    if (s.fit == PhotoFit.Fit) drawRect(Color(0xFFFFFFFF), bounds.topLeft, bounds.size)
    withTransform({
        translate(cx, cy)
        rotate(90f * s.turns, Offset.Zero)
        scale(if (s.flip) -k else k, k, Offset.Zero)
    }) {
        drawImage(image, Offset(-image.width / 2f, -image.height / 2f))
    }
}

private fun DrawScope.drawSticker(s: El.Sticker, bmp: ImageBitmap?) {
    if (bmp == null) return
    withTransform({
        translate(s.x, s.y)
        scale(s.w / bmp.width, s.h / bmp.height, Offset.Zero)
    }) { drawImage(bmp) }
}

private fun DrawScope.drawLines(l: El.Lines) {
    for (pts in l.paths) {
        if (pts.size < 4) continue
        val p = Path()
        p.moveTo(l.x + pts[0] * l.w, l.y + pts[1] * l.h)
        var i = 2
        while (i + 1 < pts.size) { p.lineTo(l.x + pts[i] * l.w, l.y + pts[i + 1] * l.h); i += 2 }
        drawPath(p, l.color.c(), style = Stroke(l.stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ───────────────────────── geometry helpers for hit-testing ─────────────────────────

/** Converts a world point into an element's unrotated local frame. */
fun El.toLocal(p: Offset): Offset {
    val c = Offset(x + w / 2f, y + h / 2f)
    val r = -rotation * PI.toFloat() / 180f
    val d = p - c
    return Offset(c.x + d.x * cos(r) - d.y * sin(r), c.y + d.x * sin(r) + d.y * cos(r))
}

fun El.contains(p: Offset, slop: Float = 0f): Boolean {
    val l = toLocal(p)
    return l.x >= x - slop && l.x <= x + w + slop && l.y >= y - slop && l.y <= y + h + slop
}

/** Axis-aligned bounding box of a rotated element (world units). */
fun El.aabb(): Rect {
    val r = rotation * PI.toFloat() / 180f
    val hw = w / 2f; val hh = h / 2f
    val ex = kotlin.math.abs(hw * cos(r)) + kotlin.math.abs(hh * sin(r))
    val ey = kotlin.math.abs(hw * sin(r)) + kotlin.math.abs(hh * cos(r))
    val c = Offset(x + hw, y + hh)
    return Rect(c.x - ex, c.y - ey, c.x + ex, c.y + ey)
}

/** World position of a local corner (fx, fy ∈ 0..1) of a rotated element. */
fun El.worldPoint(fx: Float, fy: Float): Offset {
    val c = Offset(x + w / 2f, y + h / 2f)
    val r = rotation * PI.toFloat() / 180f
    val dx = (fx - 0.5f) * w; val dy = (fy - 0.5f) * h
    return Offset(c.x + dx * cos(r) - dy * sin(r), c.y + dx * sin(r) + dy * cos(r))
}

fun angleDeg(from: Offset, to: Offset): Float = (atan2(to.y - from.y, to.x - from.x) * 180f / PI.toFloat())
fun dist(a: Offset, b: Offset): Float = sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y))

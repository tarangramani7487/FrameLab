package com.framelab.app.feature.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framelab.app.R
import com.framelab.app.data.model.El
import com.framelab.app.data.model.PhotoFit
import com.framelab.app.data.model.SLIDE_W
import com.framelab.app.render.EmptyStyle
import com.framelab.app.render.RenderOptions
import com.framelab.app.render.aabb
import com.framelab.app.render.contains
import com.framelab.app.render.drawDesign
import com.framelab.app.render.measureText
import com.framelab.app.render.spotPath
import com.framelab.app.ui.components.FlIcon
import com.framelab.app.ui.theme.DmMono
import com.framelab.app.ui.theme.Fl
import com.framelab.app.ui.theme.Inter
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Where the current slide sits inside the canvas box. */
data class SlideFrame(val left: Dp, val top: Dp, val width: Dp, val slideH: Float) {
    val height: Dp get() = width * (slideH / SLIDE_W)
}

private enum class Mode { None, Move, Resize, Pan, Pinch, Locked }

/** Gives an element a hit/selection size (template text has h = 0 until measured). */
fun sized(e: El, measurer: TextMeasurer): El = if (e is El.Text) {
    val s = measureText(e, measurer)
    e.copy(w = s.width, h = s.height)
} else e

/** Photo rectangle (in the spot's unrotated local frame) for crop drawing: centre + turned size. */
private fun photoGeometry(s: El.Spot, img: ImageBitmap): Triple<Offset, Size, Float> {
    val turned = s.turns % 2 != 0
    val iw = (if (turned) img.height else img.width).toFloat()
    val ih = (if (turned) img.width else img.height).toFloat()
    val base = if (s.fit == PhotoFit.Fill) max(s.w / iw, s.h / ih) else min(s.w / iw, s.h / ih)
    val k = base * s.zoom
    val dw = iw * k; val dh = ih * k
    val ox = max(0f, (dw - s.w) / 2f); val oy = max(0f, (dh - s.h) / 2f)
    return Triple(Offset(s.x + s.w / 2f - s.panX * ox, s.y + s.h / 2f - s.panY * oy), Size(dw, dh), k)
}

/**
 * The editor canvas: draws the carousel (current slide + 24 dp peek of the next under fl_peek_dim),
 * pages between slides, hit-tests taps and runs every selected-photo gesture (HANDOFF §8.4, §8.7).
 */
@Composable
fun EditorCanvas(
    st: EditorState,
    frame: SlideFrame,
    measurer: TextMeasurer,
    images: (String) -> ImageBitmap?,
    art: (String) -> ImageBitmap?,
    numbers: Boolean,
    interactive: Boolean,
    snapRotation: Boolean,
    haptics: Boolean,
    onTapEmpty: (El.Spot) -> Unit,
    onTapText: (El.Text) -> Unit,
    onLockedAttempt: () -> Unit,
    onReplace: () -> Unit,
    modifier: Modifier = Modifier,
    showPeek: Boolean = true,
    highlightTextId: String? = null,
    hidden: Set<String> = emptySet(),
    dimOutsideTop: Dp = 0.dp,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    val leftPx = with(density) { frame.left.toPx() }
    val topPx = with(density) { frame.top.toPx() }
    val wPx = with(density) { frame.width.toPx() }
    val hPx = with(density) { frame.height.toPx() }
    val s = wPx / SLIDE_W
    val design = st.design

    fun toPx(u: Offset): Offset = Offset(leftPx + (u.x - st.slide * SLIDE_W) * s + drag.value, topPx + u.y * s)
    fun toUnits(p: Offset): Offset = Offset((p.x - leftPx - drag.value) / s + st.slide * SLIDE_W, (p.y - topPx) / s)

    val cbTapEmpty by rememberUpdatedState(onTapEmpty)
    val cbTapText by rememberUpdatedState(onTapText)
    val cbLocked by rememberUpdatedState(onLockedAttempt)
    val interactiveNow by rememberUpdatedState(interactive)
    val snapNow by rememberUpdatedState(snapRotation)
    val hapticsNow by rememberUpdatedState(haptics)
    val geom by rememberUpdatedState(arrayOf(leftPx, topPx, wPx, s))

    fun hit(u: Offset): El? {
        val d = st.design
        val sel = st.selected?.let { sized(it, measurer) }
        if (sel != null && sel.contains(u, 6f / geom[3])) return sel
        for (e in d.elements.asReversed()) {
            if (e.id in hidden) continue
            when (e) {
                is El.Spot, is El.Sticker -> if (e.contains(u)) return e
                is El.Text -> if (sized(e, measurer).contains(u, 4f)) return e
                else -> Unit
            }
        }
        return null
    }

    fun buzz() { if (hapticsNow) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(leftPx, topPx, wPx, hidden) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    if (!interactiveNow) return@awaitEachGesture
                    val (lp, tp, wp, sc) = geom.let { arrayOf(it[0], it[1], it[2], it[3]) }
                    val base = st.design
                    val e0 = st.selected?.let { sized(it, measurer) }
                    val locked = (e0 as? El.Spot)?.locked == true
                    var mode = Mode.None
                    var handle: Pair<Float, Float>? = null
                    if (e0 != null) {
                        val hr = 24.dp.toPx()
                        if (!st.crop && !locked) {
                            val sideW = e0.w * sc; val sideH = e0.h * sc
                            val cands = mutableListOf(0f to 0f, 1f to 0f, 0f to 1f, 1f to 1f)
                            if (sideW >= 110.dp.toPx()) { cands += 0.5f to 0f; cands += 0.5f to 1f }
                            if (sideH >= 110.dp.toPx()) { cands += 0f to 0.5f; cands += 1f to 0.5f }
                            handle = cands.minByOrNull { (fx, fy) -> (toPx(worldPoint(e0, fx, fy)) - down.position).getDistance() }
                                ?.takeIf { (fx, fy) -> (toPx(worldPoint(e0, fx, fy)) - down.position).getDistance() < hr }
                        }
                        val u = toUnits(down.position)
                        mode = when {
                            handle != null -> Mode.Resize
                            e0.contains(u, 6f / sc) -> if (st.crop) Mode.Pan else if (locked) Mode.Locked else Mode.Move
                            else -> Mode.None
                        }
                    }
                    if (mode == Mode.Move || mode == Mode.Resize || mode == Mode.Pan) st.beginLive()
                    var total = Offset.Zero
                    var moved = false
                    var paging = false
                    var zoomAcc = 1f; var rotAcc = 0f; var panAcc = Offset.Zero
                    var lockedWarned = false
                    var lastSnap = Float.NaN
                    while (true) {
                        val ev = awaitPointerEvent()
                        val pressed = ev.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (pressed.size >= 2 && e0 != null && (mode == Mode.Move || mode == Mode.Pan || mode == Mode.Pinch || mode == Mode.Resize)) {
                            if (mode != Mode.Pinch) { st.beginLive(); mode = Mode.Pinch }
                            moved = true
                            zoomAcc *= ev.calculateZoom(); rotAcc += ev.calculateRotation(); panAcc += ev.calculatePan()
                            if (st.crop) {
                                val cur = st.design.element(e0.id) as? El.Spot
                                if (cur != null) st.live(st.design.replace(cur.copy(zoom = (cur.zoom * ev.calculateZoom()).coerceIn(1f, 6f))))
                            } else {
                                var rot = e0.rotation + rotAcc
                                if (snapNow) {
                                    val snapped = Math.round(rot / 90f) * 90f
                                    if (abs(rot - snapped) < 4f) { rot = snapped; if (lastSnap != snapped) { buzz(); lastSnap = snapped } } else lastSnap = Float.NaN
                                }
                                val w = (e0.w * zoomAcc).coerceAtLeast(24f); val h = (e0.h * zoomAcc).coerceAtLeast(24f)
                                st.live(EditorState.rigTransform(base, e0, e0.x + e0.w / 2f + panAcc.x / sc, e0.y + e0.h / 2f + panAcc.y / sc, w, h, rot))
                            }
                            ev.changes.forEach { it.consume() }
                            continue
                        }
                        val ch = pressed.first()
                        val delta = ch.positionChange()
                        total += delta
                        if (!moved && total.getDistance() > touchSlop) moved = true
                        if (!moved) continue
                        when (mode) {
                            Mode.Move -> st.live(EditorState.rigTransform(base, e0!!, e0.x + e0.w / 2f + total.x / sc, e0.y + e0.h / 2f + total.y / sc, e0.w, e0.h, e0.rotation))
                            Mode.Resize -> {
                                val (fx, fy) = handle!!
                                val p = toUnits(ch.position)
                                st.live(resize(base, e0!!, fx, fy, p))
                            }
                            Mode.Pan -> {
                                val cur = st.design.element(e0!!.id) as? El.Spot
                                val img = cur?.uri?.let(images)
                                if (cur != null && img != null) {
                                    val (_, sz, _) = photoGeometry(cur, img)
                                    val r = -cur.rotation * PI.toFloat() / 180f
                                    val dx = (delta.x * cos(r) - delta.y * sin(r)) / sc
                                    val dy = (delta.x * sin(r) + delta.y * cos(r)) / sc
                                    val ox = max(0.0001f, (sz.width - cur.w) / 2f); val oy = max(0.0001f, (sz.height - cur.h) / 2f)
                                    st.live(st.design.replace(cur.copy(
                                        panX = if (sz.width > cur.w) (cur.panX - dx / ox).coerceIn(-1f, 1f) else 0f,
                                        panY = if (sz.height > cur.h) (cur.panY - dy / oy).coerceIn(-1f, 1f) else 0f,
                                    )))
                                }
                            }
                            Mode.Locked -> if (!lockedWarned) { lockedWarned = true; cbLocked() }
                            Mode.None -> if (st.selectedId == null && !st.crop) {
                                paging = true
                                val atStart = st.slide == 0 && drag.value + delta.x > 0
                                val atEnd = st.slide == st.design.slides - 1 && drag.value + delta.x < 0
                                val d = if (atStart || atEnd) delta.x * 0.35f else delta.x
                                scope.launch { drag.snapTo(drag.value + d) }
                            }
                            Mode.Pinch -> Unit
                        }
                        ch.consume()
                    }
                    if (mode == Mode.Move || mode == Mode.Resize || mode == Mode.Pan || mode == Mode.Pinch) st.commitLive()
                    if (paging) {
                        val threshold = wp * 0.18f
                        var target = st.slide
                        if (drag.value < -threshold && st.slide < st.design.slides - 1) target++
                        if (drag.value > threshold && st.slide > 0) target--
                        val shift = (target - st.slide) * wp
                        st.slide = target
                        scope.launch { drag.snapTo(drag.value + shift); drag.animateTo(0f, tween(220)) }
                    } else if (!moved) {
                        val u = toUnits(down.position)
                        val inSlide = down.position.x >= lp && down.position.x <= lp + wp && down.position.y >= tp
                        val h = if (inSlide || st.selectedId != null) hit(u) else null
                        when {
                            st.crop -> if (h?.id != st.selectedId) st.crop = false
                            h is El.Spot && h.uri == null -> { st.deselect(); cbTapEmpty(h) }
                            h is El.Spot || h is El.Sticker -> { if (st.selectedId != h.id) buzz(); st.select(h.id) }
                            h is El.Text -> { st.deselect(); cbTapText(h) }
                            down.position.x > lp + wp && st.selectedId == null && st.slide < st.design.slides - 1 -> {
                                // Tapping the peek pages forward.
                                st.slide++
                                scope.launch { drag.snapTo(wp); drag.animateTo(0f, tween(220)) }
                            }
                            else -> st.deselect()
                        }
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val dragging = drag.value != 0f
            clipRect(if (dragging || !showPeek) 0f else leftPx - 0.5f, topPx, if (showPeek) size.width else leftPx + wPx, topPx + hPx) {
                translate(leftPx - st.slide * SLIDE_W * s + drag.value, topPx) {
                    scale(s, s, Offset.Zero) {
                        drawDesign(
                            design, measurer, images, art,
                            RenderOptions(EmptyStyle.Hatch, addButtons = showPeek, numbers = numbers, hidden = hidden),
                        )
                        val sel = st.selected
                        if (st.crop && sel is El.Spot) drawCropGhost(sel, sel.uri?.let(images), s)
                    }
                }
            }
            if (showPeek) {
                drawRect(Fl.peekDim, Offset(leftPx + wPx, topPx), Size(size.width - leftPx - wPx, hPx))
                if (dragging) drawRect(Fl.peekDim, Offset(0f, topPx), Size(leftPx, hPx))
            }
            // Selection chrome
            val sel = st.selected?.let { sized(it, measurer) }
            if (sel != null && interactive) drawSelection(sel, ::toPx, s, st.crop)
            val t = highlightTextId?.let { design.element(it) }?.let { sized(it, measurer) }
            if (t != null) drawTextFrame(t, ::toPx, s)
        }

        // Spanning-spot labels ("Background · slides 1–2") under the big + on its first slide.
        if (showPeek) design.spots.filter { it.uri == null && it.label != null }.forEach { sp ->
            val first = (sp.x / SLIDE_W).toInt()
            val last = ((sp.x + sp.w - 0.5f) / SLIDE_W).toInt()
            if (first == last) return@forEach
            val l = max(sp.x, first * SLIDE_W); val r = min(sp.x + sp.w, (first + 1) * SLIDE_W)
            val c = toPx(Offset((l + r) / 2f, sp.y + sp.h / 2f))
            val y = c.y + 22f * s + with(density) { 8.dp.toPx() }
            if (c.x < 0 || c.x > with(density) { leftPx + wPx }) return@forEach
            Box(Modifier.offset { IntOffset(c.x.roundToInt(), y.roundToInt()) }.offset(x = (-120).dp).width(240.dp), contentAlignment = Alignment.TopCenter) {
                Box(
                    Modifier.shadow(1.dp, RoundedCornerShape(11.dp)).height(22.dp).background(Color.White, RoundedCornerShape(11.dp)).padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        sp.label!!,
                        style = androidx.compose.ui.text.TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        color = Fl.ink, maxLines = 1,
                    )
                }
            }
        }

        val sel = st.selected?.let { sized(it, measurer) }
        if (sel != null && interactive) SelectionControls(st, sel, ::toPx, s, frame, snapRotation, haptics, onReplace, onLockedAttempt, images)
    }
}

/** World position of a local fraction point of an element. */
fun worldPoint(e: El, fx: Float, fy: Float): Offset {
    val c = Offset(e.x + e.w / 2f, e.y + e.h / 2f)
    val r = e.rotation * PI.toFloat() / 180f
    val dx = (fx - 0.5f) * e.w; val dy = (fy - 0.5f) * e.h
    return Offset(c.x + dx * cos(r) - dy * sin(r), c.y + dx * sin(r) + dy * cos(r))
}

/** Corner = proportional resize; side = stretch along one axis. Opposite corner/side stays put. */
private fun resize(base: com.framelab.app.data.model.Design, e0: El, fx: Float, fy: Float, p: Offset): com.framelab.app.data.model.Design {
    val r = e0.rotation * PI.toFloat() / 180f
    val o = worldPoint(e0, 1f - fx, 1f - fy)
    val v = p - o
    val lx = v.x * cos(-r) - v.y * sin(-r)
    val ly = v.x * sin(-r) + v.y * cos(-r)
    val minS = 24f
    var w = e0.w; var h = e0.h
    var lcx: Float; var lcy: Float
    if (fx != 0.5f && fy != 0.5f) {
        val dx = if (fx == 1f) e0.w else -e0.w
        val dy = if (fy == 1f) e0.h else -e0.h
        val k = ((lx * dx + ly * dy) / (dx * dx + dy * dy)).coerceAtLeast(minS / min(e0.w, e0.h))
        w = e0.w * k; h = e0.h * k
        lcx = dx * k / 2f; lcy = dy * k / 2f
    } else if (fy == 0.5f) {
        val sign = if (fx == 1f) 1f else -1f
        w = (lx * sign).coerceAtLeast(minS)
        lcx = sign * w / 2f; lcy = 0f
    } else {
        val sign = if (fy == 1f) 1f else -1f
        h = (ly * sign).coerceAtLeast(minS)
        lcx = 0f; lcy = sign * h / 2f
    }
    val cx = o.x + lcx * cos(r) - lcy * sin(r)
    val cy = o.y + lcx * sin(r) + lcy * cos(r)
    return EditorState.rigTransform(base, e0, cx, cy, w, h, e0.rotation)
}

/** E10: the whole photo at 42% outside the box with a dashed outline, thirds grid inside. Units space. */
private fun DrawScope.drawCropGhost(s: El.Spot, img: ImageBitmap?, scale: Float) {
    if (img == null) return
    val (c, sz, k) = photoGeometry(s, img)
    rotate(s.rotation, Offset(s.x + s.w / 2f, s.y + s.h / 2f)) {
        clipPath(spotPath(s), ClipOp.Difference) {
            withTransform({
                translate(c.x, c.y); rotate(90f * s.turns, Offset.Zero); scale(if (s.flip) -k else k, k, Offset.Zero)
            }) { drawImage(img, Offset(-img.width / 2f, -img.height / 2f), alpha = 0.42f) }
        }
        drawRect(
            Color.White.copy(alpha = 0.85f), Offset(c.x - sz.width / 2f, c.y - sz.height / 2f), sz,
            style = Stroke(1f / scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f / scale, 3f / scale))),
        )
        val g = Color.White.copy(alpha = 0.55f)
        for (i in 1..2) {
            drawLine(g, Offset(s.x + s.w * i / 3f, s.y), Offset(s.x + s.w * i / 3f, s.y + s.h), 1f / scale)
            drawLine(g, Offset(s.x, s.y + s.h * i / 3f), Offset(s.x + s.w, s.y + s.h * i / 3f), 1f / scale)
        }
    }
}

/** Selection frame and handles (§5.21), in screen px, rotated with the element. */
private fun DrawScope.drawSelection(e: El, toPx: (Offset) -> Offset, s: Float, crop: Boolean) {
    val c = toPx(Offset(e.x + e.w / 2f, e.y + e.h / 2f))
    val w = e.w * s; val h = e.h * s
    val locked = (e as? El.Spot)?.locked == true
    val dp = density
    rotate(e.rotation, c) {
        val tl = Offset(c.x - w / 2f, c.y - h / 2f)
        drawRect(Color.Black.copy(alpha = 0.25f), tl - Offset(1.25f * dp, 1.25f * dp), Size(w + 2.5f * dp, h + 2.5f * dp), style = Stroke(1f * dp))
        drawRect(if (locked) Color.White.copy(alpha = 0.7f) else Color.White, tl, Size(w, h), style = Stroke(1.5f * dp))
        if (!locked && !crop) {
            val corners = listOf(tl, tl + Offset(w, 0f), tl + Offset(0f, h), tl + Offset(w, h))
            corners.forEach {
                drawCircle(Color.Black.copy(alpha = 0.18f), 7.5f * dp, it + Offset(0f, 1f * dp))
                drawCircle(Color.White, 7f * dp, it)
                drawCircle(Color.Black.copy(alpha = 0.2f), 7f * dp, it, style = Stroke(1f * dp))
            }
            val pill = androidx.compose.ui.geometry.CornerRadius(3f * dp)
            if (w >= 110f * dp) {
                listOf(tl + Offset(w / 2f, 0f), tl + Offset(w / 2f, h)).forEach {
                    drawRoundRect(Color.White, it - Offset(11f * dp, 3f * dp), Size(22f * dp, 6f * dp), pill)
                }
            }
            if (h >= 110f * dp) {
                listOf(tl + Offset(0f, h / 2f), tl + Offset(w, h / 2f)).forEach {
                    drawRoundRect(Color.White, it - Offset(3f * dp, 11f * dp), Size(6f * dp, 22f * dp), pill)
                }
            }
        }
    }
}

/** N20: frame with 10 dp corner dots around the text being edited. */
private fun DrawScope.drawTextFrame(e: El, toPx: (Offset) -> Offset, s: Float) {
    val c = toPx(Offset(e.x + e.w / 2f, e.y + e.h / 2f))
    val pad = 4f * s
    val w = e.w * s + pad * 2; val h = e.h * s + pad * 2
    val dp = density
    rotate(e.rotation, c) {
        val tl = Offset(c.x - w / 2f, c.y - h / 2f)
        drawRect(Color.Black.copy(alpha = 0.25f), tl - Offset(1.25f * dp, 1.25f * dp), Size(w + 2.5f * dp, h + 2.5f * dp), style = Stroke(1f * dp))
        drawRect(Color.White, tl, Size(w, h), style = Stroke(1.5f * dp))
        listOf(tl, tl + Offset(w, 0f), tl + Offset(0f, h), tl + Offset(w, h)).forEach { drawCircle(Color.White, 5f * dp, it) }
    }
}

/** Action pill (delete · replace · lock), rotate button, lock badge and zoom badge. */
@Composable
private fun BoxScope.SelectionControls(
    st: EditorState,
    e: El,
    toPx: (Offset) -> Offset,
    s: Float,
    frame: SlideFrame,
    snapRotation: Boolean,
    haptics: Boolean,
    onReplace: () -> Unit,
    onLockedAttempt: () -> Unit,
    images: (String) -> ImageBitmap?,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val spot = e as? El.Spot
    val locked = spot?.locked == true
    val box = e.aabb()
    val tl = toPx(box.topLeft); val br = toPx(box.bottomRight)
    val cx = (tl.x + br.x) / 2f
    val px = { d: Dp -> with(density) { d.toPx() } }
    val topLimit = px(frame.top) - px(44.dp)
    val bottomLimit = px(frame.top) + px(frame.height) + px(48.dp)

    // Lock badge on the photo's top-right corner (E12).
    if (locked) {
        val corner = toPx(worldPoint(e, 1f, 0f))
        Box(
            Modifier.offset { IntOffset((corner.x - px(16.dp)).roundToInt(), (corner.y - px(10.dp)).roundToInt()) }
                .size(26.dp).background(Fl.ink, CircleShape).border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) { FlIcon(R.drawable.ic_lock, null, Color.White, 14.dp) }
    }

    if (st.crop && spot != null) {
        val zoom = spot.zoom
        val label = (if (zoom % 1f == 0f) "${zoom.toInt()}" else String.format(java.util.Locale.US, "%.1f", zoom)) + "×"
        Box(
            Modifier.offset { IntOffset((cx - px(26.dp)).roundToInt(), (tl.y - px(46.dp)).coerceAtLeast(topLimit).roundToInt()) }
                .height(28.dp).background(Fl.edRaised, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) { Text(label, style = androidx.compose.ui.text.TextStyle(fontFamily = DmMono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp), color = Color.White) }
        return
    }

    // Action pill 150 × 48, 10 dp above the bounding box (below it when there is no room above).
    val pillW = px(150.dp); val pillH = px(48.dp)
    var pillY = tl.y - px(10.dp) - pillH
    val rotY0 = br.y + px(12.dp)
    if (pillY < topLimit) pillY = rotY0 + px(40.dp) + px(8.dp)
    val pillX = (cx - pillW / 2f).coerceIn(px(8.dp), with(density) { (frame.left * 2 + frame.width).toPx() } - pillW - px(8.dp))
    Row(
        Modifier.offset { IntOffset(pillX.roundToInt(), pillY.roundToInt()) }
            .size(150.dp, 48.dp)
            .shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(.4f), spotColor = Color.Black.copy(.4f))
            .background(Color.White, RoundedCornerShape(24.dp))
            .semantics { contentDescription = "Photo actions" },
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val delLabel = stringResource(R.string.ed_delete_photo)
        PillButton(R.drawable.ic_delete, delLabel, if (locked) Fl.disabledText else Fl.ink) {
            if (locked) onLockedAttempt() else st.deleteSelected()
        }
        if (spot != null) PillButton(R.drawable.ic_replace, stringResource(R.string.ed_replace_photo), Fl.ink, onClick = onReplace)
        else PillButton(R.drawable.ic_duplicate, stringResource(R.string.ed_duplicate), Fl.ink) { st.duplicateSelected() }
        if (spot != null) {
            if (locked) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).clickableRole(stringResource(R.string.ed_unlock_photo)) { st.toggleLock() },
                    contentAlignment = Alignment.Center,
                ) { Box(Modifier.size(38.dp).background(Fl.ink, CircleShape), contentAlignment = Alignment.Center) { FlIcon(R.drawable.ic_unlock, null, Color.White, 20.dp) } }
            } else PillButton(R.drawable.ic_lock, stringResource(R.string.ed_lock_photo), Fl.ink) { st.toggleLock() }
        } else PillButton(R.drawable.ic_close, stringResource(R.string.ed_deselect), Fl.ink) { st.deselect() }
    }

    // Rotate button 40 dp, 12 dp below the bounding box.
    val rotY = if (pillY > br.y) (tl.y - px(12.dp) - px(40.dp)).coerceAtLeast(topLimit) else rotY0.coerceAtMost(bottomLimit)
    val rotX = cx - px(20.dp)
    val livePos = remember { floatArrayOf(0f, 0f) }
    val rotDesc = stringResource(R.string.ed_rotate_photo)
    Box(
        Modifier.offset { IntOffset(rotX.roundToInt(), rotY.roundToInt()) }
            .size(40.dp)
            .alpha(if (locked) 0.4f else 1f)
            .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(.4f), spotColor = Color.Black.copy(.4f))
            .background(Color.White, CircleShape)
            .semantics { contentDescription = rotDesc; role = Role.Button }
            .onGloballyPositioned { val p = it.positionInParent(); livePos[0] = p.x; livePos[1] = p.y }
            .pointerInput(locked, e.id) {
                if (locked) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val base = st.design
                    val e0 = st.selected ?: return@awaitEachGesture
                    val c0 = Offset(e0.x + e0.w / 2f, e0.y + e0.h / 2f)
                    val cPx = toPx(c0)
                    val a0 = atan2(livePos[1] + down.position.y - cPx.y, livePos[0] + down.position.x - cPx.x)
                    var moved = false
                    var lastSnap = Float.NaN
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull() ?: break
                        if (!ch.pressed) break
                        if (!moved && (ch.position - down.position).getDistance() > viewConfiguration.touchSlop) { moved = true; st.beginLive() }
                        if (moved) {
                            val a = atan2(livePos[1] + ch.position.y - cPx.y, livePos[0] + ch.position.x - cPx.x)
                            var rot = e0.rotation + (a - a0) * 180f / PI.toFloat()
                            if (snapRotation) {
                                val snapped = Math.round(rot / 90f) * 90f
                                if (abs(rot - snapped) < 4f) {
                                    rot = snapped
                                    if (lastSnap != snapped && haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastSnap = snapped
                                } else lastSnap = Float.NaN
                            }
                            st.live(EditorState.rigTransform(base, e0, c0.x, c0.y, e0.w, e0.h, rot))
                            ch.consume()
                        }
                    }
                    if (moved) st.commitLive() else st.rotate90()
                }
            },
        contentAlignment = Alignment.Center,
    ) { FlIcon(R.drawable.ic_rotate, null, Fl.ink, 22.dp) }
}

@Composable
private fun PillButton(icon: Int, label: String, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickableRole(label, onClick), contentAlignment = Alignment.Center) {
        FlIcon(icon, null, tint, 22.dp)
    }
}

fun Modifier.clickableRole(label: String, onClick: () -> Unit): Modifier =
    this.then(Modifier.semantics { contentDescription = label }).then(
        Modifier.pointerInput(onClick) {
            awaitEachGesture {
                val d = awaitFirstDown()
                d.consume()
                val up = waitForUpOrCancellationCompat()
                if (up) onClick()
            }
        },
    )

/** Waits for the pointer to lift; true when it lifted without being cancelled/consumed elsewhere. */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.waitForUpOrCancellationCompat(): Boolean =
    waitForUpOrCancellation()?.also { it.consume() } != null

/** Column helper for overlays. */
@Composable
fun CenterColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) { content() }

/** Rect of the current slide in px (used by coach marks). */
fun slideRectPx(frame: SlideFrame, density: androidx.compose.ui.unit.Density): Rect = with(density) {
    Rect(frame.left.toPx(), frame.top.toPx(), (frame.left + frame.width).toPx(), (frame.top + frame.height).toPx())
}

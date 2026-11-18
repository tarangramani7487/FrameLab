package com.htr.collagemaker.feature.editor

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htr.collagemaker.R
import com.htr.collagemaker.data.model.El
import com.htr.collagemaker.data.model.PhotoFit
import com.htr.collagemaker.data.model.SpotShape
import com.htr.collagemaker.data.model.homeSlide
import com.htr.collagemaker.render.c
import com.htr.collagemaker.ui.components.FlIcon
import com.htr.collagemaker.ui.components.Spinner
import com.htr.collagemaker.ui.theme.DmMono
import com.htr.collagemaker.ui.theme.Fl
import com.htr.collagemaker.ui.theme.FlType
import com.htr.collagemaker.ui.theme.Inter

private val labelStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp)
private val chipStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
val monoCaption = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.8.sp)

// ───────────────────────────── Top bar (§5.18) ─────────────────────────────

@Composable
fun EditorTopBar(
    full: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    saving: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onPreview: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().height(56.dp).background(Fl.edBg).padding(start = 4.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TopIcon(R.drawable.ic_back, stringResource(R.string.ed_back), Color.White, onClick = onBack, iconSize = 26.dp)
        if (!full) return@Row
        Spacer(Modifier.weight(1f))
        TopIcon(R.drawable.ic_undo, stringResource(R.string.ed_undo), if (canUndo) Color.White else Fl.edDisabled, canUndo, onUndo)
        TopIcon(R.drawable.ic_redo, stringResource(R.string.ed_redo), if (canRedo) Color.White else Fl.edDisabled, canRedo, onRedo)
        TopIcon(R.drawable.ic_preview, stringResource(R.string.ed_preview), Color.White, onClick = onPreview)
        Spacer(Modifier.width(6.dp))
        Row(
            Modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).clickable(enabled = !saving, role = Role.Button, onClick = onSave),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.height(36.dp).background(Fl.accent, RoundedCornerShape(18.dp)).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (saving) Spinner(16.dp, Fl.ink, Fl.ink.copy(alpha = 0.25f))
                Text(
                    stringResource(if (saving) R.string.ed_saving else R.string.ed_save),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp), color = Fl.ink,
                )
            }
        }
    }
}

@Composable
private fun TopIcon(@DrawableRes icon: Int, label: String, tint: Color, enabled: Boolean = true, onClick: () -> Unit, iconSize: Dp = 24.dp) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { FlIcon(icon, null, tint, iconSize) }
}

// ───────────────────────────── Dock (§5.19) ─────────────────────────────

data class DockItem(val label: String, @DrawableRes val icon: Int, val active: Boolean = false, val enabled: Boolean = true, val accentPill: Boolean = false, val onClick: () -> Unit)

@Composable
fun EditorDock(items: List<DockItem>, onDeselect: (() -> Unit)?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(80.dp)) {
        val shape = RoundedCornerShape(22.dp)
        Row(
            Modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp).fillMaxWidth().height(64.dp)
                .shadow(16.dp, shape, ambientColor = Color.Black.copy(.35f), spotColor = Color.Black.copy(.35f))
                .background(Fl.edSurface, shape).border(1.dp, Color.White.copy(alpha = 0.07f), shape)
                .padding(horizontal = 4.dp)
                .semantics { contentDescription = "Editor tools" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items.forEach { it ->
                val fg = when { !it.enabled -> Fl.edDisabled; it.active -> Fl.accent; else -> Color.White }
                Column(
                    Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (it.active) Fl.edActiveTint else Color.Transparent)
                        .clickable(enabled = it.enabled, role = Role.Button, onClick = it.onClick)
                        .semantics { selected = it.active },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    if (it.accentPill) {
                        Box(Modifier.size(32.dp, 24.dp).background(Fl.accent, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            FlIcon(it.icon, null, Fl.ink, 20.dp)
                        }
                    } else FlIcon(it.icon, null, fg, 22.dp)
                    Text(it.label, style = labelStyle, color = if (it.accentPill) Color.White else fg, maxLines = 1)
                }
            }
        }
        if (onDeselect != null) {
            val desc = stringResource(R.string.ed_deselect)
            Box(
                Modifier.align(Alignment.TopEnd).padding(end = 2.dp).offset(y = (-6).dp).size(48.dp).clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onDeselect).semantics { contentDescription = desc },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(36.dp).shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(.4f), spotColor = Color.Black.copy(.4f))
                        .background(Fl.edRaised, CircleShape).border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { FlIcon(R.drawable.ic_close, null, Color.White, 18.dp) }
            }
        }
    }
}

// ───────────────────────────── Hint line ─────────────────────────────

@Composable
fun HintLine(filled: Int, total: Int, selected: El?, modifier: Modifier = Modifier) {
    val spot = selected as? El.Spot
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        val st = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
        when {
            spot?.locked == true -> {
                FlIcon(R.drawable.ic_lock, null, Fl.edMuted, 14.dp)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.ed_hint_locked), style = st, color = Fl.edMuted)
            }
            selected != null -> Text(stringResource(R.string.ed_hint_selected), style = st, color = Fl.edMuted)
            else -> {
                val count = stringResource(R.string.ed_photos_count, filled, total)
                val rest = stringResource(
                    when { filled == 0 -> R.string.ed_hint_empty; filled < total -> R.string.ed_hint_partial; else -> R.string.ed_hint_full },
                )
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) { append(count) }
                        append(" · "); append(rest)
                    },
                    style = st, color = Fl.edMuted,
                )
            }
        }
    }
}

// ───────────────────────────── Panels (§5.20) ─────────────────────────────

/** E8 · Frame shape tiles (x 19, 5 × 58 tall, gap 8, radius 14). */
@Composable
fun FramePanel(current: SpotShape, onSelect: (SpotShape) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        SpotShape.Original to (R.drawable.ic_shape_original to R.string.ed_shape_original),
        SpotShape.Square to (R.drawable.ic_shape_square to R.string.ed_shape_square),
        SpotShape.Rounded to (R.drawable.ic_shape_rounded to R.string.ed_shape_rounded),
        SpotShape.Circle to (R.drawable.ic_shape_circle to R.string.ed_shape_circle),
        SpotShape.Arch to (R.drawable.ic_shape_arch to R.string.ed_shape_arch),
    )
    Row(modifier.fillMaxWidth().padding(horizontal = 19.dp).height(58.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (shape, res) ->
            val sel = shape == current
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp))
                    .background(if (sel) Color.White else Fl.edRaised)
                    .clickable(role = Role.RadioButton) { onSelect(shape) }.semantics { selected = sel },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                FlIcon(res.first, null, if (sel) Fl.ink else Color.White, 22.dp)
                Text(stringResource(res.second), style = labelStyle, color = if (sel) Fl.ink else Color.White)
            }
        }
    }
}

data class ChipSpec(val label: String, @DrawableRes val icon: Int, val selected: Boolean = false, val enabled: Boolean = true, val onClick: () -> Unit)

/** Chip row y 688–728 (40 dp chips, gap 8, horizontal scroll, 12 dp side padding). */
@Composable
fun ChipPanel(chips: List<ChipSpec>, modifier: Modifier = Modifier, equal: Boolean = false) {
    val rowMod = if (equal) modifier.fillMaxWidth().padding(horizontal = 12.dp)
    else modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp)
    Row(rowMod.height(40.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { c ->
            val bg = when { !c.enabled -> Fl.edSurface; c.selected -> Color.White; else -> Fl.edRaised }
            val fg = when { !c.enabled -> Fl.edDisabled; c.selected -> Fl.ink; else -> Color.White }
            Row(
                (if (equal) Modifier.weight(1f) else Modifier).height(40.dp).clip(RoundedCornerShape(20.dp)).background(bg)
                    .clickable(enabled = c.enabled, role = Role.Button, onClick = c.onClick).semantics { selected = c.selected }
                    .padding(start = 11.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, if (equal) Alignment.CenterHorizontally else Alignment.Start),
            ) {
                FlIcon(c.icon, null, fg, 18.dp)
                Text(c.label, style = chipStyle, color = fg, maxLines = 1)
            }
        }
    }
}

/** E5 · Background colour: caption + template colour (reset) + 8 swatches. */
@Composable
fun ColorPanel(templateColor: Long, current: Long, onSelect: (Long) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(stringResource(R.string.ed_background_caption), style = monoCaption, color = Fl.edMuted, modifier = Modifier.padding(start = 16.dp))
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(44.dp).padding(horizontal = 12.dp)
                .semantics { contentDescription = "Background colour" },
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tplDesc = stringResource(R.string.ed_template_colour)
            Swatch(templateColor.c(), current == templateColor && Fl.swatches.none { it == current.c() }, tplDesc, icon = R.drawable.ic_reset_colour) { onSelect(templateColor) }
            Fl.swatches.forEachIndexed { i, col ->
                Swatch(col, current.c() == col, Fl.swatchNames[i]) { onSelect(colorToLong(col)) }
            }
        }
    }
}

fun colorToLong(c: Color): Long = c.toArgb().toLong() and 0xFFFFFFFFL

@Composable
fun Swatch(color: Color, selected: Boolean, label: String, size: Dp = 36.dp, ringBg: Color = Fl.edBg, @DrawableRes icon: Int? = null, onClick: () -> Unit) {
    val dark = color.luminance() < 0.2f
    Box(
        Modifier.size(size + 8.dp).clip(CircleShape).clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label; this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(size + 8.dp).border(2.dp, Fl.accent, CircleShape))
        Box(
            Modifier.size(size).background(color, CircleShape)
                .then(if (dark) Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) { if (icon != null) FlIcon(icon, null, Fl.ink, 18.dp) }
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

/** E6 · Photo order: caption + 24 dp tiles grouped by slide (S1…), current group outlined. */
@Composable
fun OrderPanel(
    spots: List<El.Spot>,
    slides: Int,
    current: Int,
    images: (String) -> ImageBitmap?,
    onTile: (El.Spot) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text(stringResource(R.string.ed_order_caption), style = monoCaption, color = Fl.edMuted, modifier = Modifier.padding(start = 16.dp))
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            var n = 0
            for (k in 0 until slides) {
                val group = spots.filter { it.homeSlide().coerceAtMost(slides - 1) == k }
                if (group.isEmpty()) continue
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.then(if (k == current) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(9.dp)).padding(3.dp) else Modifier.padding(3.dp)),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        group.forEach { s ->
                            n++
                            OrderTile(n, s, s.uri?.let(images)) { onTile(s) }
                        }
                    }
                    Text(
                        stringResource(R.string.ed_slide_short, k + 1),
                        style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 9.sp, lineHeight = 12.sp),
                        color = if (k == current) Color.White else Fl.edMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderTile(n: Int, s: El.Spot, img: ImageBitmap?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(5.dp)
    Box(Modifier.size(24.dp).clip(shape).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        if (s.uri != null) {
            if (img != null) androidx.compose.foundation.Image(img, null, Modifier.size(24.dp), contentScale = ContentScale.Crop)
            else Box(Modifier.size(24.dp).background(s.pastel.c()))
            Text(
                "$n", color = Color.White,
                style = TextStyle(
                    fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(.5f), Offset(0f, 1f), 2f),
                ),
            )
        } else {
            Canvas(Modifier.size(24.dp)) {
                drawRoundRect(
                    Fl.edDots, Offset(0.75.dp.toPx(), 0.75.dp.toPx()), Size(size.width - 1.5.dp.toPx(), size.height - 1.5.dp.toPx()),
                    CornerRadius(5.dp.toPx()), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))),
                )
            }
            Text("$n", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Fl.edMutedRaised)
        }
    }
}

/** Small round tool button (entry points for Stickers / Slides beside the page dots). */
@Composable
fun SideTool(@DrawableRes icon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(36.dp).background(Fl.edSurface, CircleShape).border(1.dp, Color.White.copy(alpha = 0.07f), CircleShape), contentAlignment = Alignment.Center) {
            FlIcon(icon, null, Color.White, 18.dp)
        }
    }
}

/** Dark-surface pill button used across editor sheets. */
@Composable
fun EdButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    danger: Boolean = false,
    textOnly: Boolean = false,
    height: Dp = 48.dp,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    hPad: Dp = 16.dp,
) {
    val bg = when { textOnly -> Color.Transparent; accent -> Fl.accent; danger -> Fl.danger; else -> Fl.edRaised }
    val fg = when { textOnly && danger -> Fl.edDanger; accent -> Fl.ink; else -> Color.White }
    Row(
        modifier.height(height).widthIn(min = 48.dp).clip(RoundedCornerShape(height / 2)).background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(PaddingValues(horizontal = hPad)),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { FlIcon(it, null, fg, 20.dp) }
        Text(
            text, color = fg, maxLines = 1,
            style = TextStyle(fontFamily = Inter, fontWeight = if (accent || danger && !textOnly) FontWeight.Bold else FontWeight.SemiBold, fontSize = if (height >= 52.dp && accent) 16.sp else 15.sp),
        )
    }
}

val darkTitle = FlType.title.copy(color = Color.White)

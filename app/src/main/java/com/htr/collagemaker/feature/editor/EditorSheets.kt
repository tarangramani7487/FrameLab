package com.htr.collagemaker.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htr.collagemaker.R
import com.htr.collagemaker.data.model.Design
import com.htr.collagemaker.data.model.El
import com.htr.collagemaker.data.model.FontKind
import com.htr.collagemaker.data.model.SLIDE_W
import com.htr.collagemaker.data.model.TextAlignKind
import com.htr.collagemaker.data.model.homeSlide
import com.htr.collagemaker.render.EmptyStyle
import com.htr.collagemaker.render.RenderOptions
import com.htr.collagemaker.render.StickerArt
import com.htr.collagemaker.render.c
import com.htr.collagemaker.render.drawDesign
import com.htr.collagemaker.render.fontFamilyOf
import com.htr.collagemaker.ui.components.FlIcon
import com.htr.collagemaker.ui.components.Spinner
import com.htr.collagemaker.ui.theme.Bricolage
import com.htr.collagemaker.ui.theme.DmMono
import com.htr.collagemaker.ui.theme.Fl
import com.htr.collagemaker.ui.theme.FlType
import com.htr.collagemaker.ui.theme.Inter
import com.htr.collagemaker.ui.theme.TemplateSerif
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val sheetTitle = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp, color = Color.White)
private val sheetBody = TextStyle(fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp, color = Fl.edMuted)
private val panelTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp, color = Color.White)
private val monoMeta = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, color = Fl.edMuted)

// ───────────────────────────── Mini slide ─────────────────────────────

/** A small rendered slide (sheets, strips). */
@Composable
fun MiniSlide(
    design: Design,
    slide: Int,
    width: Dp,
    measurer: TextMeasurer,
    images: (String) -> ImageBitmap?,
    art: (String) -> ImageBitmap?,
    modifier: Modifier = Modifier,
    radius: Dp = 4.dp,
    options: RenderOptions = RenderOptions(EmptyStyle.Hatch),
) {
    Canvas(modifier.size(width, width * (design.slideH / SLIDE_W)).clip(RoundedCornerShape(radius))) {
        val s = size.width / SLIDE_W
        clipRect {
            scale(s, s, Offset.Zero) { translate(-slide * SLIDE_W, 0f) { drawDesign(design, measurer, images, art, options) } }
        }
    }
}

// ───────────────────────────── In-layout sheet ─────────────────────────────

/**
 * Dark editor sheet (§5.12): fl_ed_surface, top radius 24, fl_ed_handle; optional scrim fl_scrim_editor
 * starting at [scrimTop] (Progress keeps the top bar undimmed).
 */
@Composable
fun BoxScope.DarkSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    scrim: Boolean = true,
    scrimTop: Dp = 0.dp,
    imeAware: Boolean = false,
    horizontal: Dp = 20.dp,
    shadow: Boolean = !scrim,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (scrim) AnimatedVisibility(visible, enter = fadeIn(tween(280)), exit = fadeOut(tween(200)), modifier = Modifier.padding(top = scrimTop)) {
        Box(Modifier.fillMaxSize().background(Fl.scrimEditor).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
    }
    AnimatedVisibility(
        visible, modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(tween(280)) { it }, exit = slideOutVertically(tween(220)) { it },
    ) {
        val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        Column(
            Modifier.fillMaxWidth()
                .then(if (shadow) Modifier.shadow(20.dp, shape, ambientColor = Color.Black.copy(.45f), spotColor = Color.Black.copy(.45f)) else Modifier)
                .background(Fl.edSurface, shape)
                .clickable(remember { MutableInteractionSource() }, null) {}
                .semantics { paneTitle = title }
                .windowInsetsPadding(if (imeAware) WindowInsets.navigationBars.union(WindowInsets.ime) else WindowInsets.navigationBars)
                .padding(horizontal = horizontal),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(36.dp, 5.dp).background(Fl.edHandle, RoundedCornerShape(3.dp)))
            }
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ───────────────────────────── N23 · Save as a draft? ─────────────────────────────

@Composable
fun BoxScope.DraftSheet(visible: Boolean, filled: Int, total: Int, onSave: () -> Unit, onDiscard: () -> Unit, onCancel: () -> Unit) {
    val title = stringResource(R.string.ed_save_as_draft)
    DarkSheet(visible, onCancel, title) {
        Spacer(Modifier.height(18.dp))
        Text(title, style = sheetTitle)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.ed_draft_body, filled, total), style = sheetBody)
        Spacer(Modifier.height(20.dp))
        EdButton(stringResource(R.string.ed_save_draft), onSave, Modifier.fillMaxWidth(), accent = true, height = 52.dp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EdButton(stringResource(R.string.ed_discard), onDiscard, Modifier.weight(1f), danger = true, textOnly = true)
            EdButton(stringResource(R.string.ed_cancel), onCancel, Modifier.weight(1f))
        }
    }
}

// ───────────────────────────── D1 · Pre-check ─────────────────────────────

@Composable
fun BoxScope.PrecheckSheet(
    visible: Boolean, design: Design, measurer: TextMeasurer, images: (String) -> ImageBitmap?, art: (String) -> ImageBitmap?,
    onKeepEditing: () -> Unit, onSaveAnyway: () -> Unit,
) {
    val empty = design.emptyCount
    val title = if (empty == 1) stringResource(R.string.ed_spots_empty_one) else stringResource(R.string.ed_spots_empty, empty)
    DarkSheet(visible, onKeepEditing, title) {
        Spacer(Modifier.height(18.dp))
        Text(title, style = sheetTitle)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.ed_spots_empty_body, design.slides), style = sheetBody)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp, end = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (k in 0 until design.slides) {
                val n = design.spots.count { it.uri == null && it.homeSlide().coerceAtMost(design.slides - 1) == k }
                Box {
                    MiniSlide(design, k, 50.dp, measurer, images, art)
                    val label = if (n > 0) stringResource(R.string.ed_slide_n_empty, k + 1, n) else stringResource(R.string.ed_slide_n_full, k + 1)
                    Box(
                        Modifier.offset(x = 38.dp, y = (-6).dp).size(20.dp).background(if (n > 0) Fl.accent else Color.White, CircleShape)
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (n > 0) Text("$n", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Fl.ink)
                        else FlIcon(R.drawable.ic_check, null, Fl.ink, 13.dp)
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EdButton(stringResource(R.string.ed_keep_editing), onKeepEditing, Modifier.weight(1f), height = 52.dp)
            EdButton(stringResource(R.string.ed_save_anyway), onSaveAnyway, Modifier.weight(1f), accent = true, height = 52.dp)
        }
    }
}

// ───────────────────────────── E13 · Saving ─────────────────────────────

@Composable
fun BoxScope.ProgressSheet(
    visible: Boolean, design: Design, done: Int, measurer: TextMeasurer, images: (String) -> ImageBitmap?, art: (String) -> ImageBitmap?,
    scrimTop: Dp, onCancel: () -> Unit,
) {
    val total = design.slides
    val title = stringResource(R.string.ed_saving_slide, (done + 1).coerceAtMost(total), total)
    DarkSheet(visible, {}, title, scrimTop = scrimTop) {
        Spacer(Modifier.height(18.dp))
        Text(title, style = sheetTitle)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.ed_saving_body), style = sheetBody)
        Spacer(Modifier.height(16.dp))
        val desc = stringResource(R.string.ed_slides_saved)
        com.htr.collagemaker.ui.components.FlProgress(
            (done + 0.5f) / total, Modifier.fillMaxWidth().semantics { contentDescription = desc }, track = Fl.edRaised,
        )
        Spacer(Modifier.height(20.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (k in 0 until total) {
                Box(contentAlignment = Alignment.Center) {
                    MiniSlide(design, k, 50.dp, measurer, images, art, options = RenderOptions(EmptyStyle.Paper))
                    when {
                        k < done -> Box(
                            Modifier.size(20.dp).shadow(4.dp, CircleShape).background(Fl.accent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { FlIcon(R.drawable.ic_check, null, Fl.ink, 13.dp) }
                        k == done -> Box(Modifier.size(50.dp, 63.dp).background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                            Spinner(18.dp, Color.White)
                        }
                        else -> Box(Modifier.size(50.dp, 63.dp).background(Fl.edSurface.copy(alpha = 0.62f)))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        EdButton(stringResource(R.string.ed_cancel), onCancel, Modifier.fillMaxWidth())
    }
}

// ───────────────────────────── D4 · Export failed ─────────────────────────────

@Composable
fun BoxScope.ErrorSheet(visible: Boolean, failedSlide: Int, message: String, onClose: () -> Unit, onRetry: () -> Unit) {
    val title = stringResource(R.string.ed_export_failed)
    DarkSheet(visible, onClose, title) {
        Spacer(Modifier.height(20.dp))
        Box(Modifier.size(48.dp).background(Fl.accent.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
            FlIcon(R.drawable.ic_alert, null, Fl.accent, 24.dp)
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = sheetTitle)
        Spacer(Modifier.height(6.dp))
        val n = failedSlide + 1
        Text(
            when (failedSlide) {
                0 -> stringResource(R.string.ed_export_failed_body_first, n)
                1 -> stringResource(R.string.ed_export_failed_body_second, n)
                else -> stringResource(R.string.ed_export_failed_body, n, failedSlide)
            },
            style = sheetBody,
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.ed_error_line, message), style = monoMeta)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EdButton(stringResource(R.string.ed_close), onClose, Modifier.weight(1f), height = 52.dp)
            EdButton(stringResource(R.string.ed_try_again), onRetry, Modifier.weight(1f), accent = true, height = 52.dp)
        }
    }
}

// ───────────────────────────── D5 · Remove all photos? ─────────────────────────────

@Composable
fun BoxScope.ClearDialog(visible: Boolean, onCancel: () -> Unit, onRemove: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
        Box(Modifier.fillMaxSize().background(Fl.scrimEditor).clickable(remember { MutableInteractionSource() }, null, onClick = onCancel), contentAlignment = Alignment.Center) {
            val title = stringResource(R.string.ed_remove_all)
            val shape = RoundedCornerShape(24.dp)
            Column(
                Modifier.width(318.dp).shadow(25.dp, shape, ambientColor = Color.Black.copy(.5f), spotColor = Color.Black.copy(.5f))
                    .background(Fl.edSurface, shape).clickable(remember { MutableInteractionSource() }, null) {}
                    .semantics { paneTitle = title }.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(title, style = FlType.dialogTitle, color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.ed_remove_all_body), style = sheetBody, textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EdButton(stringResource(R.string.ed_cancel), onCancel, Modifier.weight(1f))
                    EdButton(stringResource(R.string.ed_remove), onRemove, Modifier.weight(1f), danger = true)
                }
            }
        }
    }
}

// ───────────────────────────── N20 · Edit text ─────────────────────────────

private val textColors = listOf(
    0xFF2B2621L to "Ink", 0xFFFFFFFFL to "White", 0xFFFF5A36L to "Coral",
    0xFFB33A17L to "Rust", 0xFF3F7C8EL to "Sea", 0xFF6E5A8FL to "Dusk",
)

@Composable
fun BoxScope.TextSheet(visible: Boolean, text: El.Text?, onChange: ((El.Text) -> El.Text) -> Unit, onDone: () -> Unit) {
    val title = stringResource(R.string.ed_edit_text)
    DarkSheet(visible, onDone, title, scrim = false, imeAware = true) {
        if (text == null) return@DarkSheet
        Row(Modifier.fillMaxWidth().height(40.dp).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = panelTitle, modifier = Modifier.weight(1f))
            Box(Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onDone).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.ed_done), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp), color = Fl.accent)
            }
        }
        Spacer(Modifier.height(6.dp))
        val fieldDesc = stringResource(R.string.ed_text_field)
        BasicTextField(
            value = text.text,
            onValueChange = { v -> onChange { it.copy(text = v) } },
            textStyle = TextStyle(
                fontFamily = fontFamilyOf(text.font), fontStyle = if (text.italic) FontStyle.Italic else FontStyle.Normal,
                fontSize = 22.sp, lineHeight = 26.sp, color = Color.White,
                textAlign = when (text.align) { TextAlignKind.Left -> TextAlign.Left; TextAlignKind.Center -> TextAlign.Center; TextAlignKind.Right -> TextAlign.Right },
            ),
            cursorBrush = SolidColor(Fl.accent),
            modifier = Modifier.fillMaxWidth().height(72.dp).background(Fl.edRaised, RoundedCornerShape(14.dp))
                .border(1.5.dp, Fl.edHandle, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp)
                .semantics { contentDescription = fieldDesc },
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.background(Fl.edRaised, RoundedCornerShape(14.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    Triple(FontKind.Sans, R.string.ed_sans, TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)),
                    Triple(FontKind.Serif, R.string.ed_serif, TextStyle(fontFamily = TemplateSerif, fontStyle = FontStyle.Italic, fontSize = 18.sp)),
                    Triple(FontKind.Mono, R.string.ed_mono, TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 15.sp)),
                ).forEach { (kind, label, style) ->
                    val sel = text.font == kind
                    val d = stringResource(label)
                    Box(
                        Modifier.size(56.dp, 36.dp).clip(RoundedCornerShape(10.dp)).background(if (sel) Color.White else Color.Transparent)
                            .clickable(role = Role.RadioButton) { onChange { it.copy(font = kind, italic = kind == FontKind.Serif) } }
                            .semantics { contentDescription = d; selected = sel },
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.ed_aa), style = style, color = if (sel) Fl.ink else Color.White) }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(
                    Triple(TextAlignKind.Left, R.drawable.ic_align_left, R.string.ed_align_left),
                    Triple(TextAlignKind.Center, R.drawable.ic_align_center, R.string.ed_align_centre),
                    Triple(TextAlignKind.Right, R.drawable.ic_align_right, R.string.ed_align_right),
                ).forEach { (a, icon, label) ->
                    val sel = text.align == a
                    val d = stringResource(label)
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (sel) Fl.edRaised else Color.Transparent)
                            .clickable(role = Role.RadioButton) { onChange { it.copy(align = a) } }.semantics { contentDescription = d; selected = sel },
                        contentAlignment = Alignment.Center,
                    ) { FlIcon(icon, null, if (sel) Color.White else Fl.edMutedRaised, 22.dp) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ed_size), style = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp), color = Fl.edMuted, modifier = Modifier.weight(1f))
            Text("${text.size.roundToInt()}", style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp), color = Color.White)
        }
        Slider(
            value = text.size.coerceIn(12f, 96f), valueRange = 12f..96f,
            onValueChange = { v ->
                onChange { t ->
                    val ratio = if (t.lineHeight > 0f) t.lineHeight / t.size else 0f
                    t.copy(size = v.roundToInt().toFloat(), lineHeight = if (ratio > 0f) v.roundToInt() * ratio else 0f)
                }
            },
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Fl.accent, inactiveTrackColor = Fl.edRaised),
            modifier = Modifier.fillMaxWidth().height(32.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.ed_colour), style = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp), color = Fl.edMuted)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            textColors.forEach { (col, name) ->
                Swatch(col.c(), text.color == col, name, size = 32.dp, ringBg = Fl.edSurface) { onChange { it.copy(color = col) } }
            }
        }
    }
}

// ───────────────────────────── N21 · Stickers ─────────────────────────────

private data class StickerTile(val art: String, val label: String, val cat: Int)

@Composable
fun BoxScope.StickersSheet(visible: Boolean, onClose: () -> Unit, onPick: (String) -> Unit) {
    val title = stringResource(R.string.ed_stickers)
    val cats = listOf(R.string.ed_st_all, R.string.ed_st_tape, R.string.ed_st_stamps, R.string.ed_st_dates, R.string.ed_st_doodles, R.string.ed_st_chips)
    val tiles = listOf(
        StickerTile("tape_masking", "Masking tape", 1), StickerTile("tape_pink", "Pink tape", 1), StickerTile("tape_gingham", "Gingham tape", 1),
        StickerTile("date_stamp", "Date stamp", 3), StickerTile("postmark", "Postmark", 2), StickerTile("postage_stamp", "Postage stamp", 2),
        StickerTile("star", "Star doodle", 4), StickerTile("arrow", "Arrow doodle", 4), StickerTile("swipe_chip", "Swipe chip", 5),
        StickerTile("heart", "Heart doodle", 4), StickerTile("label", "Paper label", 5), StickerTile("ticket", "Boarding pass", 5),
    )
    var cat by remember { mutableIntStateOf(0) }
    DarkSheet(visible, onClose, title, scrim = false) {
        Row(Modifier.fillMaxWidth().height(40.dp).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = panelTitle, modifier = Modifier.weight(1f))
            val d = stringResource(R.string.ed_close_stickers)
            Box(Modifier.offset(x = 10.dp).size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = d }, contentAlignment = Alignment.Center) {
                FlIcon(R.drawable.ic_close, null, Color.White, 22.dp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            cats.forEachIndexed { i, res ->
                val sel = i == cat
                Box(
                    Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (sel) Color.White else Fl.edRaised)
                        .clickable(role = Role.Tab) { cat = i }.semantics { selected = sel }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(res), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp), color = if (sel) Fl.ink else Color.White) }
            }
        }
        Spacer(Modifier.height(14.dp))
        val shown = tiles.filter { cat == 0 || it.cat == cat }
        LazyVerticalGrid(GridCells.Fixed(4), Modifier.fillMaxWidth().heightIn(max = 260.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(shown, key = { it.art }) { t ->
                Box(
                    Modifier.height(80.dp).clip(RoundedCornerShape(16.dp)).background(Fl.edRaised)
                        .clickable(role = Role.Button) { onPick(t.art) }.semantics { contentDescription = t.label },
                    contentAlignment = Alignment.Center,
                ) {
                    StickerArt.res(t.art)?.let { Image(painterResource(it), null, Modifier.size(80.dp), contentScale = ContentScale.Fit) }
                }
            }
        }
    }
}

// ───────────────────────────── N22 · Slides ─────────────────────────────

@Composable
fun BoxScope.SlidesSheet(
    visible: Boolean, st: EditorState, measurer: TextMeasurer, images: (String) -> ImageBitmap?, art: (String) -> ImageBitmap?, onDone: () -> Unit,
) {
    val d = st.design
    val title = stringResource(R.string.ed_slides)
    val density = LocalDensity.current
    DarkSheet(visible, onDone, title, scrim = false, horizontal = 16.dp) {
        Row(Modifier.fillMaxWidth().height(40.dp).padding(top = 4.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = panelTitle)
            Text(stringResource(R.string.ed_slides_meta, d.slides, d.ratio.label), style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp), color = Fl.edMuted)
            Spacer(Modifier.weight(1f))
            Box(Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onDone).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.ed_done), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp), color = Fl.accent)
            }
        }
        Spacer(Modifier.height(16.dp))
        val links = d.linkedPairs()
        var dragFrom by remember { mutableIntStateOf(-1) }
        var dragX by remember { mutableFloatStateOf(0f) }
        Box(Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            for (k in 0 until d.slides) {
                val sel = k == st.slide
                val actions = stringResource(R.string.ed_slide_actions, k + 1)
                Column(
                    Modifier.width(52.dp)
                        .offset { IntOffset(if (dragFrom == k) dragX.roundToInt() else 0, 0) }
                        .pointerInputDrag(k, onStart = { dragFrom = k; dragX = 0f }, onDrag = { dragX += it }, onEnd = {
                            val stepPx = with(density) { 52.dp.toPx() }
                            val steps = (dragX / stepPx).roundToInt()
                            val dir = if (steps > 0) 1 else -1
                            repeat(kotlin.math.abs(steps)) { st.moveSlide(st.slide, dir) }
                            dragFrom = -1; dragX = 0f
                        }, onLongPressStart = { st.slide = k })
                        .semantics { contentDescription = actions },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.border(2.dp, if (sel) Fl.accent else Color.Transparent, RoundedCornerShape(7.dp)).padding(3.dp)
                            .clickable(role = Role.Button) { st.slide = k },
                    ) { MiniSlide(d, k, 44.dp, measurer, images, art) }
                    Spacer(Modifier.height(4.dp))
                    Text("${k + 1}", style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 12.sp), color = if (sel) Color.White else Fl.edMuted)
                }
            }
            val add = stringResource(R.string.ed_add_slide)
            Box(
                Modifier.padding(start = 4.dp, top = 3.dp).size(44.dp, 55.dp).clip(RoundedCornerShape(6.dp))
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(Fl.edDots, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))), cornerRadius = CornerRadius(6.dp.toPx()))
                    }
                    .clickable(role = Role.Button) { st.addSlide() }.semantics { contentDescription = add },
                contentAlignment = Alignment.Center,
            ) { FlIcon(R.drawable.ic_add, null, Fl.edMutedRaised, 20.dp) }
        }
        // Link badges between slides that share a spanning photo (drawn above the thumbnails).
        links.forEach { k ->
            Box(
                Modifier.offset(x = 52.dp * (k + 1) - 8.dp, y = 23.dp).size(16.dp).background(Fl.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) { FlIcon(R.drawable.ic_link, null, Fl.ink, 10.dp) }
        }
        }
        val pairs = st.blocks().filter { it.count() > 1 }
        val note = if (pairs.isEmpty()) stringResource(R.string.ed_slides_note) else {
            val names = pairs.map { stringResource(R.string.ed_slides_range, it.first + 1, it.last + 1) }
            val joined = if (names.size == 1) names[0] else stringResource(R.string.ed_and, names.dropLast(1).joinToString(", "), names.last()).replace(" Slides ", " ")
            stringResource(R.string.ed_slides_pair_note, joined)
        }
        Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlIcon(R.drawable.ic_link, null, Fl.edMuted, 16.dp)
            Text(note, style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 17.sp), color = Fl.edMuted)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val blocks = st.blocks()
            val bi = blocks.indexOfFirst { st.slide in it }
            SlideAction(R.drawable.ic_duplicate, stringResource(R.string.ed_duplicate), Color.White, true) { st.duplicateSlide(st.slide) }
            SlideAction(R.drawable.ic_arrow_left, stringResource(R.string.ed_move_left), Color.White, bi > 0) { st.moveSlide(st.slide, -1) }
            SlideAction(R.drawable.ic_arrow_right, stringResource(R.string.ed_move_right), Color.White, bi in 0 until blocks.size - 1) { st.moveSlide(st.slide, 1) }
            SlideAction(R.drawable.ic_delete, stringResource(R.string.ed_delete), Fl.edDanger, blocks.size > 1) { st.deleteSlide(st.slide) }
        }
    }
}

private fun Modifier.pointerInputDrag(key: Int, onStart: () -> Unit, onDrag: (Float) -> Unit, onEnd: () -> Unit, onLongPressStart: () -> Unit): Modifier =
    this.then(Modifier.pointerInputCompat(key) {
        detectDragGesturesAfterLongPress(
            onDragStart = { onLongPressStart(); onStart() },
            onDragEnd = onEnd, onDragCancel = onEnd,
            onDrag = { ch, d -> ch.consume(); onDrag(d.x) },
        )
    })

private fun Modifier.pointerInputCompat(key: Any, block: suspend androidx.compose.ui.input.pointer.PointerInputScope.() -> Unit) =
    this.pointerInput(key, block)

@Composable
private fun androidx.compose.foundation.layout.RowScope.SlideAction(icon: Int, label: String, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    val fg = if (enabled) tint else Fl.edDisabled
    Column(
        Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(16.dp)).background(Fl.edRaised).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        FlIcon(icon, null, fg, 22.dp)
        Text(label, style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp), color = fg)
    }
}

// ───────────────────────────── N19 · Preview mode ─────────────────────────────

@Composable
fun PreviewMode(
    design: Design, startSlide: Int, measurer: TextMeasurer, images: (String) -> ImageBitmap?, art: (String) -> ImageBitmap?,
    onClose: (Int) -> Unit, onSave: () -> Unit,
) {
    val pager = rememberPagerState(startSlide) { design.slides }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Fl.edBg).windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.navigationBars))) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 20.dp, top = 4.dp).height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            val d = stringResource(R.string.ed_close_preview)
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button) { onClose(pager.currentPage) }.semantics { contentDescription = d }, contentAlignment = Alignment.Center) {
                FlIcon(R.drawable.ic_close, null, Color.White, 24.dp)
            }
            Text(stringResource(R.string.ed_preview), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp, textAlign = TextAlign.Center), color = Color.White, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.ed_counter, pager.currentPage + 1, design.slides),
                style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp, textAlign = TextAlign.End),
                color = Fl.edMuted, modifier = Modifier.width(44.dp),
            )
        }
        Spacer(Modifier.weight(0.4f).heightIn(min = 8.dp))
        HorizontalPager(pager, Modifier.fillMaxWidth()) { k ->
            val label = stringResource(R.string.ed_slide_of, k + 1, design.slides)
            Canvas(Modifier.fillMaxWidth().aspectRatio(SLIDE_W / design.slideH).semantics { contentDescription = label }) {
                val s = size.width / SLIDE_W
                clipRect { scale(s, s, Offset.Zero) { translate(-k * SLIDE_W, 0f) { drawDesign(design, measurer, images, art, RenderOptions(EmptyStyle.Paper)) } } }
            }
        }
        com.htr.collagemaker.ui.components.PageDots(design.slides, pager.currentPage, { scope.launch { pager.animateScrollToPage(it) } }, Modifier.align(Alignment.CenterHorizontally), dark = true)
        Spacer(Modifier.height(8.dp))
        // Strip scrubber: 40 dp slides, current outlined white, others dimmed.
        Box(Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(4.dp))) {
            Row {
                for (k in 0 until design.slides) {
                    Box(Modifier.clickable { scope.launch { pager.animateScrollToPage(k) } }) {
                        MiniSlide(design, k, 40.dp, measurer, images, art, radius = 0.dp, options = RenderOptions(EmptyStyle.Paper))
                        if (k != pager.currentPage) Box(Modifier.size(40.dp, 40.dp * (design.slideH / SLIDE_W)).background(Fl.edBg.copy(alpha = 0.45f)))
                        else Box(Modifier.size(40.dp, 40.dp * (design.slideH / SLIDE_W)).border(2.dp, Color.White, RoundedCornerShape(4.dp)))
                    }
                }
            }
        }
        Spacer(Modifier.weight(0.6f).heightIn(min = 16.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 30.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EdButton(stringResource(R.string.ed_keep_editing), { onClose(pager.currentPage) }, Modifier.width(116.dp), height = 52.dp, hPad = 4.dp)
            EdButton(stringResource(R.string.ed_save_n_images, design.slides), onSave, Modifier.weight(1f), accent = true, height = 52.dp, icon = R.drawable.ic_download)
        }
    }
}

// ───────────────────────────── N23 · Coach marks ─────────────────────────────

/** Coach mark (§5.23): black 74% scrim with a 72 dp ringed cut-out and a white card pointing at it. */
@Composable
fun CoachMark(step: Int, total: Int, target: Offset, title: String, body: String, onSkip: () -> Unit, onNext: () -> Unit) {
    val density = LocalDensity.current
    val r = with(density) { 36.dp.toPx() }
    Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) {}) {
        Canvas(Modifier.fillMaxSize()) {
            val hole = Path().apply { addOval(Rect(target, r)) }
            val full = Path().apply { addRect(Rect(Offset.Zero, size)) }
            drawPath(Path().apply { op(full, hole, PathOperation.Difference) }, Fl.edBg.copy(alpha = 0.74f))
            drawCircle(Fl.accent, r - 1.dp.toPx(), target, style = Stroke(2.dp.toPx()))
        }
        val screenH = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx() }
        val below = target.y + r + with(density) { 18.dp.toPx() } + with(density) { 220.dp.toPx() } < screenH
        val cardTop = if (below) target.y + r + with(density) { 18.dp.toPx() } else null
        val cardBottom = if (!below) target.y - r - with(density) { 18.dp.toPx() } else null
        val shape = RoundedCornerShape(20.dp)
        val arrowY = if (cardTop != null) cardTop - with(density) { 7.dp.toPx() } else cardBottom!! - with(density) { 7.dp.toPx() }
        Box(
            Modifier.offset { IntOffset((target.x - 7.dp.toPx()).roundToInt(), arrowY.roundToInt()) }
                .size(14.dp).graphicsLayer { rotationZ = 45f }.background(Color.White, RoundedCornerShape(2.dp)),
        )
        Column(
            Modifier.padding(horizontal = 28.dp)
                .then(if (cardTop != null) Modifier.offset { IntOffset(0, cardTop.roundToInt()) } else Modifier.align(Alignment.BottomStart).padding(bottom = with(density) { (screenH - cardBottom!!).toDp() }))
                .fillMaxWidth()
                .shadow(16.dp, shape, ambientColor = Color.Black.copy(.4f), spotColor = Color.Black.copy(.4f))
                .background(Color.White, shape)
                .semantics { paneTitle = "Tip ${step + 1} of $total" }
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp),
        ) {
            Text(stringResource(R.string.ed_tip_of, step + 1, total), style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.sp), color = Fl.muted)
            Spacer(Modifier.height(6.dp))
            Text(title, style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp), color = Fl.ink)
            Spacer(Modifier.height(6.dp))
            Text(body, style = TextStyle(fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp), color = Fl.muted)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (step < total - 1) {
                    Box(Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onSkip).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.ed_skip_tips), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp), color = Fl.muted)
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Fl.ink).clickable(role = Role.Button, onClick = onNext).padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(if (step < total - 1) R.string.ed_next else R.string.ed_got_it), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp), color = Color.White)
                }
            }
        }
    }
}

// ───────────────────────────── E1 · Opening / E2 · Couldn't open ─────────────────────────────

@Composable
fun OpeningState(name: String, slides: Int, frame: SlideFrame, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "open")
    val x by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "x")
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier.offset(frame.left, frame.top).size(frame.width, frame.height).clip(RoundedCornerShape(4.dp)).background(Fl.edSurface)
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.linearGradient(listOf(Fl.edSurface, Color(0xFF252528), Fl.edSurface), start = Offset(size.width * x - size.width * 0.4f, 0f), end = Offset(size.width * x + size.width * 0.4f, size.height * 0.2f)))
                },
        )
        Box(Modifier.offset(frame.left + frame.width + 4.dp, frame.top).size(20.dp, frame.height).background(Color(0xFF141416), RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)))
        Column(
            Modifier.offset(frame.left, frame.top + frame.height * 0.36f).width(frame.width),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spinner(32.dp, Fl.accent, Color.White.copy(alpha = 0.12f), stroke = 3.dp)
            Text(stringResource(R.string.ed_opening, name), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp), color = Color.White)
            Text(stringResource(R.string.ed_getting_ready, slides), style = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp), color = Fl.edMuted)
        }
        Row(Modifier.offset(y = frame.top + frame.height + 22.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            repeat(slides) { Box(Modifier.size(6.dp).background(Fl.edDots, CircleShape)) }
        }
    }
}

@Composable
fun CouldntOpen(name: String?, onRetry: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.padding(top = 146.dp).width(310.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).background(Fl.edSurface, CircleShape), contentAlignment = Alignment.Center) { FlIcon(R.drawable.ic_alert, null, Fl.accent, 28.dp) }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.ed_couldnt_open), style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp, textAlign = TextAlign.Center), color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                if (name != null) stringResource(R.string.ed_couldnt_open_body, name) else stringResource(R.string.ed_couldnt_open_body_generic),
                style = sheetBody.copy(textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.ed_error_line, stringResource(R.string.ed_error_not_found)), style = monoMeta.copy(textAlign = TextAlign.Center))
            Spacer(Modifier.height(28.dp))
            Column(Modifier.width(240.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EdButton(stringResource(R.string.ed_try_again), onRetry, Modifier.fillMaxWidth(), accent = true)
                EdButton(stringResource(R.string.ed_go_back), onBack, Modifier.fillMaxWidth())
            }
        }
    }
}

/** Keeps a state across a key change. */
@Composable
fun <T> rememberLatch(key: Any?, value: () -> T) = remember(key) { mutableStateOf(value()) }

@Composable
fun LaunchOnce(block: suspend () -> Unit) = LaunchedEffect(Unit) { block() }

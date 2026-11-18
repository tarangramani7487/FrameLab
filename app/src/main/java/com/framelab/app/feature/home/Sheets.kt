package com.framelab.app.feature.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framelab.app.R
import com.framelab.app.data.model.SLIDE_W
import com.framelab.app.data.model.Template
import com.framelab.app.nav.LocalAppState
import com.framelab.app.nav.Routes
import com.framelab.app.render.SlideView
import com.framelab.app.render.StripView
import com.framelab.app.ui.components.BtnKind
import com.framelab.app.ui.components.CardThumb
import com.framelab.app.ui.components.FlButton
import com.framelab.app.ui.components.FlIcon
import com.framelab.app.ui.components.FlSheet
import com.framelab.app.ui.components.IconButtonFl
import com.framelab.app.ui.components.LocalToaster
import com.framelab.app.ui.components.PageDots
import com.framelab.app.ui.components.Tab
import com.framelab.app.ui.components.TemplateCard
import com.framelab.app.ui.components.ToastHost
import com.framelab.app.ui.theme.Fl
import com.framelab.app.ui.theme.FlType
import kotlinx.coroutines.launch

/** N7 · Template preview sheet. [templateId] null = hidden. */
@Composable
fun TemplatePreviewSheet(templateId: String?, onDismiss: () -> Unit) {
    val app = LocalAppState.current
    val state by app.container.templates.state.collectAsState()
    // Keep the last template while the sheet animates out.
    var lastId by remember { mutableStateOf<String?>(null) }
    if (templateId != null) lastId = templateId
    val t = lastId?.let { state.findTemplate(it) }
    FlSheet(
        visible = templateId != null && t != null,
        onDismiss = onDismiss,
        top = 52.dp,
        handle = false,
        horizontalPadding = 0.dp,
        paneTitle = t?.let { stringResource(R.string.preview_cd, it.name) },
    ) {
        if (t != null) PreviewContent(t, onDismiss)
    }
}

@Composable
private fun PreviewContent(t: Template, onDismiss: () -> Unit) {
    val app = LocalAppState.current
    val c = app.container
    val state by c.templates.state.collectAsState()
    val favs by c.prefs.favourites.collectAsState()
    val profile by c.prefs.profile.collectAsState()
    val toaster = LocalToaster.current
    val ctx = LocalContext.current
    val fav = t.id in favs
    val addedMsg = stringResource(R.string.s_added_to_favourites)
    val removedMsg = stringResource(R.string.preview_removed_favourite)
    val undo = stringResource(R.string.s_undo_2)
    val shareText = stringResource(R.string.preview_share_text, t.name)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Handle + header.
            Box(Modifier.fillMaxWidth().height(86.dp)) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp).size(36.dp, 5.dp).background(Fl.lineStrong, RoundedCornerShape(3.dp)))
                Column(Modifier.padding(start = 20.dp, top = 26.dp).width(290.dp)) {
                    Text(t.name, style = FlType.sheetTitleLarge.copy(letterSpacing = (-0.6).sp), color = Fl.ink, maxLines = 1)
                    Text(
                        if (t.slides == 1) stringResource(R.string.preview_meta_one, t.photos, t.ratio.label)
                        else stringResource(R.string.preview_meta, t.slides, t.photos, t.ratio.label),
                        style = FlType.monoLabel, color = Fl.muted, modifier = Modifier.padding(top = 4.dp),
                    )
                }
                IconButtonFl(
                    R.drawable.ic_close, stringResource(R.string.cd_close), onDismiss,
                    Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 10.dp),
                    size = 44.dp, iconSize = 20.dp, background = Fl.sunk,
                )
            }
            // Category + style chips (y 86).
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.categoryTitle(t.meta.category)?.let { Tag(it, filled = true) }
                if (t.meta.isPro) Tag(stringResource(R.string.preview_pro), filled = true)
                listOf(t.meta.style, t.meta.topic).filter { it.isNotBlank() }.distinct().forEach { Tag(it, filled = false) }
            }
            Spacer(Modifier.height(14.dp))
            SlidePager(t)
            if (t.slides > 1) {
                Text(
                    stringResource(R.string.preview_joined, t.slides), style = FlType.monoSmall, color = Fl.muted,
                    modifier = Modifier.padding(start = 20.dp, top = 4.dp),
                )
                JoinedStrip(t, Modifier.padding(start = 21.dp, end = 21.dp, top = 6.dp))
            }
            // Actions (y 622).
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 17.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButtonFl(
                    if (fav) R.drawable.ic_favourite_filled else R.drawable.ic_favourite,
                    stringResource(if (fav) R.string.preview_remove_favourite else R.string.cd_add_to_favourites),
                    {
                        c.prefs.toggleFavourite(t.id)
                        toaster.show(null, if (fav) removedMsg else addedMsg, action = undo) { c.prefs.toggleFavourite(t.id) }
                    },
                    size = 52.dp, iconSize = 22.dp, tint = if (fav) Fl.accent else Fl.ink, background = Fl.card,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Fl.line),
                )
                IconButtonFl(
                    R.drawable.ic_link, stringResource(R.string.cd_share_link),
                    {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                        ctx.startActivity(Intent.createChooser(send, null))
                    },
                    size = 52.dp, iconSize = 22.dp, background = Fl.card, border = androidx.compose.foundation.BorderStroke(1.dp, Fl.line),
                )
                FlButton(
                    stringResource(R.string.s_use_template),
                    {
                        if (t.meta.isPro && !profile.isPro) { onDismiss(); app.navigate(Routes.PAYWALL) }
                        else app.openEditor(templateId = t.id)
                    },
                    Modifier.weight(1f), large = true,
                    leadingIcon = if (t.meta.isPro && !profile.isPro) R.drawable.ic_pro else null,
                )
            }
            // Similar templates.
            val similar = remember(t.id, state) {
                val pool = if (t.meta.kind == com.framelab.app.data.model.TemplateKind.Frame) state.frames else state.templates
                pool.filter { it.id != t.id }
                    .sortedByDescending { (if (it.meta.category == t.meta.category) 2 else 0) + (if (it.meta.style == t.meta.style) 1 else 0) }
                    .take(8)
            }
            if (similar.isNotEmpty()) {
                Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 14.dp).fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.s_similar_templates),
                        style = FlType.section.copy(fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.sp), color = Fl.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier.height(44.dp).clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) { onDismiss(); app.navigate(Routes.seeAll(t.meta.category, Tab.Home)) }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.s_see_all), style = FlType.chip, color = Fl.ink) }
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    similar.forEach { s ->
                        TemplateCard(
                            s, { app.openPreview(s.id) }, width = 108.dp,
                            favourite = s.id in favs, locked = s.meta.isPro && !profile.isPro, showNew = false,
                            info = if (s.slides > 1) stringResource(R.string.preview_info_slides, s.slides) else stringResource(R.string.preview_info_frame),
                        )
                    }
                }
            } else Spacer(Modifier.height(32.dp))
        }
        ToastHost(Modifier.align(Alignment.BottomCenter).padding(12.dp))
    }
}

@Composable
private fun Tag(text: String, filled: Boolean) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.height(28.dp).clip(shape).background(if (filled) Fl.sunk else Color.Transparent)
            .then(if (!filled) Modifier.border(1.dp, Fl.line, shape) else Modifier)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, style = FlType.caption.copy(fontWeight = if (filled) FontWeight.SemiBold else FontWeight.Medium),
            color = if (filled) Fl.ink else Fl.muted, maxLines = 1,
        )
    }
}

/** Swipeable slides: 280 dp wide, the next slide peeks and is washed white 55%. */
@Composable
private fun SlidePager(t: Template) {
    val pager = rememberPagerState { t.slides }
    val scope = rememberCoroutineScope()
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        val slideW = 280.dp.coerceAtMost(maxWidth)
        val slideH = slideW * (t.design.slideH / SLIDE_W)
        HorizontalPager(
            pager,
            Modifier.fillMaxWidth().height(slideH).clip(RoundedCornerShape(0.dp)),
            pageSize = PageSize.Fixed(slideW),
            contentPadding = PaddingValues(end = maxWidth - slideW),
            beyondViewportPageCount = 1,
        ) { page ->
            val current = page == pager.currentPage
            Box(Modifier.size(slideW, slideH).semantics { contentDescription = "Slide ${page + 1} of ${t.slides}" }) {
                SlideView(t.design, page, Modifier.fillMaxSize(), keepRatio = false)
                if (!current) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.55f)))
            }
        }
    }
    if (t.slides > 1) {
        Box(Modifier.padding(start = 20.dp).width(280.dp).height(22.dp), contentAlignment = Alignment.Center) {
            PageDots(t.slides, pager.currentPage, { scope.launch { pager.animateScrollToPage(it) } })
        }
    } else Spacer(Modifier.height(12.dp))
}

/** "All N slides, joined": 58 dp per slide with dashed slide edges. */
@Composable
private fun JoinedStrip(t: Template, modifier: Modifier) {
    val slideW = 58.dp
    Box(modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        StripView(
            t.design,
            Modifier.width(slideW * t.slides).clip(RoundedCornerShape(6.dp)).drawWithContent {
                drawContent()
                val w = size.width / t.slides
                for (i in 1 until t.slides) {
                    drawLine(Fl.ink.copy(alpha = .35f), Offset(w * i, 0f), Offset(w * i, size.height), 1.dp.toPx(), pathEffect = dash)
                }
            },
        )
    }
}

// ───────────────────────────── N6 · Create sheet ─────────────────────────────

/** N6 · Create sheet. */
@Composable
fun CreateSheet(visible: Boolean, onDismiss: () -> Unit) {
    val app = LocalAppState.current
    val c = app.container
    val drafts by c.drafts.drafts.collectAsState()
    val state by c.templates.state.collectAsState()
    val draft = drafts.firstOrNull()
    FlSheet(visible, onDismiss, scrim = Fl.scrimLight, paneTitle = stringResource(R.string.cd_create)) {
        Row(Modifier.fillMaxWidth().padding(top = 0.dp).height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.s_create), style = FlType.sheetTitle, color = Fl.ink, modifier = Modifier.weight(1f))
            IconButtonFl(R.drawable.ic_close, stringResource(R.string.cd_close), onDismiss, Modifier.padding(end = 0.dp), size = 44.dp, iconSize = 22.dp)
        }
        if (draft != null) {
            val shape = RoundedCornerShape(16.dp)
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().height(84.dp).clip(shape).background(Fl.paper).border(1.dp, Fl.line, shape)
                    .clickable(role = Role.Button) { app.openEditor(draftId = draft.id) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CardThumb(draft.design, 48.dp, height = 60.dp, radius = 8.dp)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.s_continue_last_draft), style = FlType.body.copy(fontWeight = FontWeight.Bold, lineHeight = 20.sp), color = Fl.ink)
                    Text(
                        stringResource(R.string.create_draft_meta, draft.name, draft.filled, draft.total, timeAgo(draft.updatedAt)),
                        style = FlType.secondary, color = Fl.muted, maxLines = 1,
                    )
                }
                FlIcon(R.drawable.ic_chevron_right, null, Fl.muted, 18.dp)
            }
        }
        Column(Modifier.padding(top = 8.dp, bottom = 32.dp)) {
            CreateRow(
                R.drawable.ic_templates, stringResource(R.string.s_start_from_a_template),
                stringResource(R.string.create_browse, state.totalDesigns), divider = true,
            ) { onDismiss(); HomeUi.showFrames = false; app.selectTab(Tab.Explore) }
            CreateRow(
                R.drawable.ic_slides, stringResource(R.string.s_blank_carousel),
                stringResource(R.string.s_choose_a_ratio_and_slide_count), divider = true, enabled = false,
            ) {}
            CreateRow(
                R.drawable.ic_frame, stringResource(R.string.s_add_a_frame),
                stringResource(R.string.s_one_photo_one_decorative_frame), divider = false,
            ) { onDismiss(); HomeUi.showFrames = true; app.selectTab(Tab.Home) }
        }
    }
}

@Composable
private fun CreateRow(icon: Int, title: String, subtitle: String, divider: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().height(68.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(44.dp).background(if (enabled) Fl.accentSoft else Fl.sunk, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { FlIcon(icon, null, if (enabled) Fl.accentInk else Fl.muted, 22.dp) }
            Column(Modifier.weight(1f)) {
                Text(title, style = FlType.body.copy(fontWeight = FontWeight.SemiBold, lineHeight = 20.sp), color = if (enabled) Fl.ink else Fl.muted)
                Text(subtitle, style = FlType.secondary, color = Fl.muted)
            }
            if (enabled) FlIcon(R.drawable.ic_chevron_right, null, Fl.muted, 18.dp)
            else Box(Modifier.height(22.dp).background(Fl.sunk, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.s_soon), style = FlType.monoSmall.copy(letterSpacing = 0.8.sp), color = Fl.muted)
            }
        }
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(Fl.sunk))
    }
}

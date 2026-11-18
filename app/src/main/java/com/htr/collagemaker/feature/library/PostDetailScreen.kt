package com.htr.collagemaker.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.htr.collagemaker.R
import com.htr.collagemaker.data.SavedPost
import com.htr.collagemaker.feature.export.Exporter
import com.htr.collagemaker.nav.LocalAppState
import com.htr.collagemaker.render.SlideView
import com.htr.collagemaker.ui.components.BtnKind
import com.htr.collagemaker.ui.components.FlButton
import com.htr.collagemaker.ui.components.FlDialog
import com.htr.collagemaker.ui.components.FlIcon
import com.htr.collagemaker.ui.components.FlTopBar
import com.htr.collagemaker.ui.components.IconButtonFl
import com.htr.collagemaker.ui.components.LocalToaster
import com.htr.collagemaker.ui.components.MenuItem
import com.htr.collagemaker.ui.components.PageDots
import com.htr.collagemaker.ui.components.PopoverMenu
import com.htr.collagemaker.ui.components.Spinner
import com.htr.collagemaker.ui.theme.Fl
import com.htr.collagemaker.ui.theme.FlType
import kotlinx.coroutines.launch
import java.util.Locale

/** N11 · Saved post. */
@Composable
fun PostDetailScreen(postId: String) {
    val app = LocalAppState.current
    val c = app.container
    val posts by c.saved.posts.collectAsState()
    val settings by c.prefs.settings.collectAsState()
    val post = posts.firstOrNull { it.id == postId }
    var last by remember { mutableStateOf<SavedPost?>(null) }
    if (post != null) last = post
    val p = post ?: last
    val ctx = LocalContext.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    var inOrder by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val savedAgain = stringResource(R.string.lib_toast_saved_again)
    val savedAgainBody = p?.let { stringResource(R.string.lib_toast_saved_again_body, it.design.slides) } ?: ""
    val failLead = stringResource(R.string.lib_toast_save_failed)
    val failBody = stringResource(R.string.lib_toast_try_again)
    val noImgLead = stringResource(R.string.lib_toast_no_images)
    val noImgBody = stringResource(R.string.lib_toast_no_images_body)

    fun share() { if (p != null && !shareImages(ctx, p.images, instagram = false)) toaster.show(noImgLead, noImgBody, R.drawable.ic_alert) }
    fun editCopy() { if (p != null) { val id = c.drafts.save(p.design); app.openEditor(draftId = id) } }
    fun saveAgain() {
        val d = p ?: return
        val s = settings ?: return
        if (saving) return
        saving = true
        scope.launch {
            val r = runCatching { Exporter.saveSlides(ctx, d.design, s) }
            saving = false
            r.onSuccess { uris ->
                c.saved.add(d.copy(images = uris.map { it.toString() }, savedAt = System.currentTimeMillis()))
                toaster.show(savedAgain, savedAgainBody, R.drawable.ic_check)
            }.onFailure { toaster.show(failLead, failBody, R.drawable.ic_alert) }
        }
    }

    Column(Modifier.fillMaxSize().background(Fl.paper).statusBarsPadding()) {
        Spacer(Modifier.height(4.dp))
        FlTopBar(
            title = p?.name, onBack = { app.back() }, backLabel = stringResource(R.string.cd_back),
            right = {
                Box {
                    IconButtonFl(R.drawable.ic_more, stringResource(R.string.cd_more_options), { menu = true }, size = 44.dp,
                        background = if (menu) Fl.track else androidx.compose.ui.graphics.Color.Transparent)
                    PopoverMenu(
                        menu, { menu = false }, offset = DpOffset((-140).dp, 50.dp),
                        items = listOf(
                            MenuItem(stringResource(R.string.s_share_2), R.drawable.ic_share) { share() },
                            MenuItem(stringResource(R.string.s_save_again), R.drawable.ic_download) { saveAgain() },
                            MenuItem(stringResource(R.string.s_edit_a_copy), R.drawable.ic_edit) { editCopy() },
                            MenuItem(stringResource(R.string.s_delete_2), R.drawable.ic_delete, destructive = true) { confirmDelete = true },
                        ),
                    )
                }
            },
        )
        if (p == null) return@Column
        val count = p.design.slides
        val pager = rememberPagerState { count }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))
            HorizontalPager(pager, Modifier.fillMaxWidth().height(428.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp), pageSpacing = 32.dp) { i ->
                Box(
                    Modifier.size(342.dp, 428.dp)
                        .shadow(12.dp, RoundedCornerShape(4.dp), ambientColor = Fl.ink.copy(.16f), spotColor = Fl.ink.copy(.16f))
                        .clip(RoundedCornerShape(4.dp)).background(Fl.sunk),
                ) {
                    SlideView(p.design, i, Modifier.fillMaxSize(), keepRatio = false)
                    p.images.getOrNull(i)?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                }
            }
            Spacer(Modifier.height(0.dp))
            PageDots(count, pager.currentPage, { scope.launch { pager.animateScrollToPage(it) } }, Modifier.fillMaxWidth().height(32.dp))
            val when_ = if (isSameDay(p.savedAt, System.currentTimeMillis())) stringResource(R.string.lib_today_caps) else shortDate(p.savedAt).uppercase(Locale.getDefault())
            Text(
                stringResource(R.string.lib_post_meta, count, p.width, p.height, when_),
                style = FlType.monoLabel.copy(letterSpacing = 0.8.sp), color = Fl.muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlButton(stringResource(R.string.s_share_to_instagram), { inOrder = true }, Modifier.fillMaxWidth(), large = true, leadingIcon = R.drawable.ic_send)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionTile(R.drawable.ic_share, stringResource(R.string.s_share_2), ::share, Modifier.weight(1f))
                    Box(Modifier.weight(1f)) {
                        ActionTile(R.drawable.ic_download, stringResource(R.string.s_save_again), ::saveAgain, Modifier.fillMaxWidth())
                        if (saving) Box(Modifier.matchParentSize(), contentAlignment = Alignment.TopEnd) { Box(Modifier.padding(8.dp)) { Spinner(14.dp, Fl.ink) } }
                    }
                    ActionTile(R.drawable.ic_edit, stringResource(R.string.s_edit_a_copy), ::editCopy, Modifier.weight(1f))
                }
                Row(
                    Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) { confirmDelete = true },
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlIcon(R.drawable.ic_delete, null, Fl.danger, 18.dp)
                    Text(stringResource(R.string.s_delete_from_my_designs), style = FlType.button.copy(fontWeight = FontWeight.SemiBold), color = Fl.danger)
                }
            }
            Spacer(Modifier.height(16.dp).navigationBarsPadding())
        }
    }

    PostInOrderSheet(inOrder, p?.design, p?.images ?: emptyList()) { inOrder = false }

    FlDialog(
        visible = confirmDelete,
        onDismiss = { confirmDelete = false },
        title = stringResource(R.string.s_delete_from_my_designs_2),
        body = stringResource(R.string.lib_delete_post_body, p?.design?.slides ?: 0),
        confirm = stringResource(R.string.s_delete_2),
        dismiss = stringResource(R.string.s_cancel),
        confirmKind = BtnKind.Danger,
        onConfirm = {
            confirmDelete = false
            p?.let { c.saved.delete(it.id) }
            app.back()
        },
    )
}


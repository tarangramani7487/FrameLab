package com.htr.collagemaker.feature.export

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htr.collagemaker.R
import com.htr.collagemaker.feature.library.PostInOrderSheet
import com.htr.collagemaker.feature.onboarding.PillButton
import com.htr.collagemaker.feature.onboarding.PlainTextButton
import com.htr.collagemaker.nav.LocalAppState
import com.htr.collagemaker.nav.Routes
import com.htr.collagemaker.render.RenderOptions
import com.htr.collagemaker.render.SlideView
import com.htr.collagemaker.ui.components.FlIcon
import com.htr.collagemaker.ui.components.IconButtonFl
import com.htr.collagemaker.ui.components.NumberBadge
import com.htr.collagemaker.ui.theme.Bricolage
import com.htr.collagemaker.ui.theme.DmMono
import com.htr.collagemaker.ui.theme.Fl
import com.htr.collagemaker.ui.theme.FlType
import com.htr.collagemaker.ui.theme.Inter

// ───────────────────────────── D3 · Saved (N18-Success) ─────────────────────────────

/** D3 · "6 images saved, in order" — light full screen after a successful export. */
@Composable
fun SuccessScreen(postId: String) {
    val app = LocalAppState.current
    val context = LocalContext.current
    val posts by app.container.saved.posts.collectAsState()
    val post = posts.firstOrNull { it.id == postId }
    val settings by app.container.prefs.settings.collectAsState()
    var showOrder by remember { mutableStateOf(false) }

    val done = {
        if (!app.nav.popBackStack(Routes.EDITOR, inclusive = true)) app.goHome()
    }

    Box(Modifier.fillMaxSize().background(Fl.paper)) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        val minH = maxHeight
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = minH),
        ) {
            // Top row: mono meta + Done (y 52).
            Row(
                Modifier.padding(start = 20.dp, end = 10.dp, top = 4.dp).fillMaxWidth().height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (post != null) stringResource(R.string.success_meta, post.name.uppercase(), post.width, post.height) else "",
                    style = FlType.monoLabel.copy(lineHeight = 14.sp), color = Fl.muted, modifier = Modifier.weight(1f), maxLines = 1,
                )
                Box(
                    Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { done() }.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.s_done), style = FlType.buttonL, color = Fl.ink) }
            }
            Spacer(Modifier.height(8.dp))

            // Contact sheet: 358 × 300 white card, slides 100 wide in rows of three.
            val shape = RoundedCornerShape(20.dp)
            Box(
                Modifier.align(Alignment.CenterHorizontally).width(358.dp)
                    .shadow(4.dp, shape, ambientColor = Fl.ink.copy(alpha = .08f), spotColor = Fl.ink.copy(alpha = .08f))
                    .background(Fl.card, shape)
                    .padding(start = 17.dp, end = 17.dp, top = 16.dp, bottom = 18.dp),
            ) {
                if (post != null) {
                    val d = post.design
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        (0 until d.slides).chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { k ->
                                    Box(Modifier.width(100.dp)) {
                                        Box(Modifier.width(100.dp).clip(RoundedCornerShape(3.dp))) {
                                            SlideView(d, k, Modifier.width(100.dp), RenderOptions(empty = com.htr.collagemaker.render.EmptyStyle.Paper))
                                        }
                                        NumberBadge("%02d".format(k + 1), Modifier.offset(6.dp, 6.dp), mono = true)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.size(324.dp, 266.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
            val count = post?.images?.size ?: post?.design?.slides ?: 0
            Text(
                pluralStringResource(R.plurals.success_images_saved, count, count),
                style = FlType.screenTitle, color = Fl.ink, minLines = 2,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(if (settings?.album == false) R.string.success_body_no_album else R.string.s_they_re_in_your_gallery_in_an_album_call),
                style = FlType.body, color = Fl.muted,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.weight(1f).height(24.dp))

            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp, top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PillButton(stringResource(R.string.s_share_to_instagram), Fl.accent, Fl.ink, Modifier.fillMaxWidth(), icon = R.drawable.ic_send) {
                    showOrder = true
                }
                val shareTitle = stringResource(R.string.s_share_2)
                Row(
                    Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(50)).background(Fl.card)
                        .border(1.dp, Fl.line, RoundedCornerShape(50))
                        .clickable(role = Role.Button) { post?.let { ShareUtils.shareImageStrings(context, it.images, false, shareTitle) } },
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlIcon(R.drawable.ic_share, null, Fl.ink, 20.dp)
                    Text(shareTitle, style = FlType.buttonL.copy(fontWeight = FontWeight.SemiBold), color = Fl.ink)
                }
                Box(
                    Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) {
                        if (!app.nav.popBackStack()) app.goHome()
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.s_back_to_editor),
                        style = FlType.button.copy(textDecoration = TextDecoration.Underline), color = Fl.ink,
                    )
                }
            }
        }
        }
    }

    PostInOrderSheet(showOrder, post?.design, post?.images.orEmpty()) { showOrder = false }
}

// ───────────────────────────── D2 · Photo access is off ─────────────────────────────

/** D2 · Photo access is off — explains how to turn access back on. */
@Composable
fun PhotoAccessOffScreen() {
    val app = LocalAppState.current
    val context = LocalContext.current
    val close = { if (!app.nav.popBackStack()) app.goHome() }

    Box(Modifier.fillMaxSize().background(Fl.paper)) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        val minH = maxHeight
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = minH),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp)) {
                IconButtonFl(R.drawable.ic_close, stringResource(R.string.cd_close), { close() })
            }
            // Illustration 270 × 264 at (70, 96): 10 dp right of centre.
            Image(
                painterResource(R.drawable.img_photo_access_off), null,
                Modifier.offset(x = 10.dp).size(270.dp, 264.dp), contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.s_photo_access_is_off),
                style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.6).sp),
                color = Fl.ink, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.s_framelab_needs_your_photos_to_fill_templ),
                style = FlType.body, color = Fl.muted, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(22.dp))
            val steps = listOf(R.string.s_open_settings_for_framelab, R.string.s_tap_photos, R.string.s_allow_access_to_all_photos)
            val shape = RoundedCornerShape(18.dp)
            Column(
                Modifier.width(350.dp).background(Fl.card, shape).border(1.dp, Fl.line, shape)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                steps.forEachIndexed { i, s ->
                    Row(Modifier.fillMaxWidth().height(50.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(24.dp).background(Fl.ink, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 24.sp), color = Fl.card)
                        }
                        Text(stringResource(s), style = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 20.sp), color = Fl.ink)
                    }
                    if (i < steps.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Fl.sunk))
                }
            }
            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.weight(1f))
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, bottom = 34.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PillButton(stringResource(R.string.s_open_settings), Fl.accent, Fl.ink, Modifier.fillMaxWidth()) {
                    ShareUtils.openAppSettings(context)
                }
                PlainTextButton(stringResource(R.string.s_not_now), Modifier.fillMaxWidth()) { close() }
            }
        }
        }
    }
}

package com.framelab.app.feature.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framelab.app.R
import com.framelab.app.data.catalog.BuiltInCatalog
import com.framelab.app.data.model.Design
import com.framelab.app.feature.library.openUrl
import com.framelab.app.feature.library.shareText
import com.framelab.app.nav.LocalAppState
import com.framelab.app.nav.Routes
import com.framelab.app.render.SlideView
import com.framelab.app.render.StripView
import com.framelab.app.ui.components.BtnKind
import com.framelab.app.ui.components.FlButton
import com.framelab.app.ui.components.FlIcon
import com.framelab.app.ui.components.FlSheet
import com.framelab.app.ui.components.FlTextButton
import com.framelab.app.ui.components.IconButtonFl
import com.framelab.app.ui.components.InitialsAvatar
import com.framelab.app.ui.components.LocalToaster
import com.framelab.app.ui.components.Wordmark
import com.framelab.app.ui.theme.Bricolage
import com.framelab.app.ui.theme.DmMono
import com.framelab.app.ui.theme.Fl
import com.framelab.app.ui.theme.FlType

/** A sample design for decorative strips (first built-in template). */
fun sampleDesign(): Design? = (BuiltInCatalog.byId("torn-diary") ?: BuiltInCatalog.templates.firstOrNull())?.design

/** N12 · Profile (signed in) / N13 · Profile — guest. */
@Composable
fun ProfileScreen() {
    val app = LocalAppState.current
    val c = app.container
    val profile by c.prefs.profile.collectAsState()
    val drafts by c.drafts.drafts.collectAsState()
    val posts by c.saved.posts.collectAsState()
    val favs by c.prefs.favourites.collectAsState()
    var rate by remember { mutableStateOf(false) }
    var share by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Fl.paper).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.s_profile), style = FlType.screenTitle, color = Fl.ink, modifier = Modifier.padding(start = 20.dp, top = 6.dp))
            IconButtonFl(
                R.drawable.ic_settings, stringResource(R.string.cd_settings), { app.navigate(Routes.SETTINGS) },
                Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 10.dp), size = 44.dp,
            )
        }
        if (profile.signedIn) {
            // Header: avatar 64 + name/handle + Edit profile pill (y 106–178).
            Row(
                Modifier.padding(horizontal = 20.dp).padding(top = 6.dp).fillMaxWidth().height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Avatar(profile.initials, 64, profile.avatarUri)
                Column(Modifier.weight(1f)) {
                    Text(profile.name.ifBlank { profile.email }, style = FlType.body.copy(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold), color = Fl.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (profile.handle.isNotBlank()) Text("@${profile.handle}", style = FlType.bodyS, color = Fl.muted, maxLines = 1)
                }
                Box(
                    Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Fl.card).border(1.dp, Fl.line, RoundedCornerShape(18.dp))
                        .clickable(role = Role.Button) { app.navigate(Routes.EDIT_PROFILE) }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.s_edit_profile), style = FlType.chip, color = Fl.ink) }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${drafts.size + posts.size}", stringResource(R.string.s_designs_made), Modifier.weight(1f)) { app.navigate(Routes.designs(0)) }
                StatTile("${posts.size}", stringResource(R.string.s_posts_saved), Modifier.weight(1f)) { app.navigate(Routes.designs(1)) }
                StatTile("${favs.size}", stringResource(R.string.s_favourites), Modifier.weight(1f)) { app.navigate(Routes.designs(2)) }
            }
            Spacer(Modifier.height(18.dp))
        } else {
            GuestCard { app.navigate(Routes.SIGN_IN) }
            Spacer(Modifier.height(16.dp))
        }
        ProBanner(Modifier.padding(horizontal = 20.dp)) { app.navigate(if (profile.isPro) Routes.WELCOME_PRO else Routes.PAYWALL) }
        Spacer(Modifier.height(18.dp))
        val shape = RoundedCornerShape(18.dp)
        Column(
            Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(shape).background(Fl.card).border(1.dp, Fl.line, shape)
                .padding(horizontal = 14.dp).semantics { contentDescription = "More" },
        ) {
            NavRow(R.drawable.ic_settings, stringResource(R.string.s_settings_2)) { app.navigate(Routes.SETTINGS) }
            NavRow(R.drawable.ic_help, stringResource(R.string.s_help_feedback)) { app.navigate(Routes.HELP) }
            NavRow(R.drawable.ic_star, stringResource(R.string.s_rate_framelab)) { rate = true }
            NavRow(R.drawable.ic_share, stringResource(R.string.s_share_the_app)) { share = true }
            NavRow(R.drawable.ic_info, stringResource(R.string.s_about), last = true) { app.navigate(Routes.ABOUT) }
        }
        Spacer(Modifier.height(24.dp))
    }

    RateSheet(rate, onDismiss = { rate = false }, onFeedback = { rate = false; app.navigate(Routes.FEEDBACK) })
    ShareAppSheet(share) { share = false }
}

@Composable
fun Avatar(initials: String, size: Int, uri: String?) {
    if (uri != null) InitialsAvatar(initials, size.dp, imageUri = uri)
    else Box(Modifier.size(size.dp).background(Fl.accentSoft, CircleShape), contentAlignment = Alignment.Center) {
        Text(initials, style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = (size * 0.34f).sp), color = Fl.ink)
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.height(78.dp).clip(shape).background(Fl.card).border(1.dp, Fl.line, shape).clickable(role = Role.Button, onClick = onClick).padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(value, style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 26.sp), color = Fl.ink)
        Text(label, style = FlType.caption.copy(lineHeight = 15.sp), color = Fl.muted, maxLines = 1)
    }
}

@Composable
private fun GuestCard(onSignIn: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.padding(horizontal = 20.dp).padding(top = 6.dp).fillMaxWidth().clip(shape).background(Fl.card).border(1.dp, Fl.line, shape)
            .padding(16.dp).semantics { contentDescription = "Guest" },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(56.dp).background(Fl.sunk, CircleShape), contentAlignment = Alignment.Center) { FlIcon(R.drawable.ic_profile, null, Fl.muted, 26.dp) }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.s_guest), style = FlType.body.copy(fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold), color = Fl.ink)
                Text(stringResource(R.string.s_sign_in_to_keep_drafts_and_favourites_on), style = FlType.secondary, color = Fl.muted)
            }
        }
        Spacer(Modifier.height(14.dp))
        FlButton(stringResource(R.string.s_sign_in), onSignIn, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProBanner(modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(18.dp)).background(Fl.ink).clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(Fl.accent, CircleShape), contentAlignment = Alignment.Center) { FlIcon(R.drawable.ic_pro, null, Fl.ink, 22.dp) }
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.s_framelab_pro), style = FlType.bodyL.copy(fontWeight = FontWeight.Bold, lineHeight = 21.sp), color = Color.White)
            Text(stringResource(R.string.s_every_template_hd_export_no_watermark), style = FlType.secondary, color = Color.White.copy(alpha = .74f))
        }
        Box(Modifier.height(36.dp).background(Fl.accent, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.s_see_plans), style = FlType.chip.copy(fontWeight = FontWeight.Bold), color = Fl.ink)
        }
    }
}

@Composable
fun NavRow(icon: Int, label: String, last: Boolean = false, trailing: String? = null, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FlIcon(icon, null, Fl.ink, 20.dp)
            Text(label, style = FlType.body, color = Fl.ink, modifier = Modifier.weight(1f))
            if (trailing != null) Text(trailing, style = FlType.secondary, color = Fl.muted)
            FlIcon(R.drawable.ic_chevron_right, null, Fl.muted, 18.dp)
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Fl.sunk))
    }
}

// ───────────────────────────── N12-Rate ─────────────────────────────

@Composable
fun RateSheet(visible: Boolean, onDismiss: () -> Unit, onFeedback: () -> Unit) {
    val ctx = LocalContext.current
    FlSheet(visible, onDismiss, paneTitle = stringResource(R.string.cd_enjoying_framelab), horizontalPadding = 24.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 6.dp).size(120.dp, 86.dp)) {
                sampleDesign()?.let {
                    Box(
                        Modifier.offset(32.dp, 4.dp).rotate(-6f).shadow(6.dp, RoundedCornerShape(2.dp), ambientColor = Fl.ink.copy(.18f), spotColor = Fl.ink.copy(.18f))
                            .size(56.dp, 70.dp),
                    ) { SlideView(it, 0, Modifier.size(56.dp, 70.dp), keepRatio = false) }
                }
                Box(Modifier.align(Alignment.TopEnd).offset((-6).dp, 0.dp).size(36.dp).background(Fl.accent, CircleShape), contentAlignment = Alignment.Center) {
                    FlIcon(R.drawable.ic_star, null, Fl.ink, 20.dp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.s_enjoying_framelab), style = FlType.sheetTitle, color = Fl.ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.s_a_quick_rating_helps_other_creators_find), style = FlType.bodyS, color = Fl.muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            FlButton(
                stringResource(R.string.s_yes_rate_framelab),
                {
                    // Play In-App Review API is not bundled; open the store listing instead.
                    val id = ctx.packageName
                    if (!openUrl(ctx, "market://details?id=$id")) openUrl(ctx, "https://play.google.com/store/apps/details?id=$id")
                    onDismiss()
                },
                Modifier.fillMaxWidth(), large = true,
            )
            Spacer(Modifier.height(8.dp))
            FlButton(stringResource(R.string.s_not_really_send_feedback), onFeedback, Modifier.fillMaxWidth(), kind = BtnKind.Outlined, large = true)
            Spacer(Modifier.height(8.dp))
            FlTextButton(stringResource(R.string.s_maybe_later), onDismiss, Modifier.fillMaxWidth().height(44.dp), color = Fl.muted)
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ───────────────────────────── N12-Share ─────────────────────────────

@Composable
fun ShareAppSheet(visible: Boolean, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val toaster = LocalToaster.current
    val link = stringResource(R.string.s_app_link)
    val shareTextValue = stringResource(R.string.lib_share_app_text)
    val copied = stringResource(R.string.lib_link_copied)
    FlSheet(visible, onDismiss, paneTitle = stringResource(R.string.cd_share_framelab), horizontalPadding = 24.dp) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.s_share_framelab), style = FlType.sheetTitle.copy(fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.4).sp), color = Fl.ink, modifier = Modifier.weight(1f))
            IconButtonFl(R.drawable.ic_close, stringResource(R.string.cd_close), onDismiss, Modifier.offset(x = 10.dp), size = 44.dp)
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(20.dp)).background(Fl.ink)) {
            Wordmark(Modifier.padding(start = 20.dp, top = 18.dp), style = FlType.wordmark.copy(fontSize = 26.sp, lineHeight = 30.sp), color = Color.White)
            Text(
                stringResource(R.string.s_carousels_that_flow_across_every_swipe), style = FlType.secondary, color = Color.White.copy(alpha = .74f),
                modifier = Modifier.padding(start = 20.dp, top = 52.dp),
            )
            sampleDesign()?.let {
                Box(Modifier.padding(start = 20.dp, top = 88.dp).width(300.dp).clip(RoundedCornerShape(4.dp))) { StripView(it, Modifier.width(300.dp)) }
            }
            Text(
                link, style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 1.sp),
                color = Color.White.copy(alpha = .74f), modifier = Modifier.padding(start = 20.dp, top = 156.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlButton(
                stringResource(R.string.s_copy_link),
                {
                    copyToClipboard(ctx, link)
                    toaster.show(null, copied, R.drawable.ic_link)
                    onDismiss()
                },
                Modifier.weight(1f), kind = BtnKind.Outlined, large = true, leadingIcon = R.drawable.ic_link,
            )
            FlButton(stringResource(R.string.s_share_2), { shareText(ctx, shareTextValue); onDismiss() }, Modifier.weight(1f), large = true, leadingIcon = R.drawable.ic_share)
        }
        Spacer(Modifier.height(32.dp))
    }
}

fun copyToClipboard(ctx: Context, text: String) {
    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("FrameLab", text))
}

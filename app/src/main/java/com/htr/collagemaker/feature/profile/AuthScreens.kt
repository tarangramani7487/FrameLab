package com.htr.collagemaker.feature.profile

import android.content.Intent
import android.util.Patterns
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htr.collagemaker.R
import com.htr.collagemaker.nav.LocalAppState
import com.htr.collagemaker.nav.Routes
import com.htr.collagemaker.render.SlideView
import com.htr.collagemaker.ui.components.BtnKind
import com.htr.collagemaker.ui.components.FlButton
import com.htr.collagemaker.ui.components.FlTextButton
import com.htr.collagemaker.ui.components.IconButtonFl
import com.htr.collagemaker.ui.components.LocalToaster
import com.htr.collagemaker.ui.theme.DmMono
import com.htr.collagemaker.ui.theme.Fl
import com.htr.collagemaker.ui.theme.FlType
import kotlinx.coroutines.delay

/** "By continuing you agree to the Terms and Privacy Policy." with tappable links. */
@Composable
fun LegalLine(modifier: Modifier = Modifier) {
    val open = rememberLinkOpener()
    val text = buildAnnotatedString {
        append(stringResource(R.string.lib_by_continuing_pre))
        pushStringAnnotation("link", "terms")
        withStyle(SpanStyle(color = Fl.ink, textDecoration = TextDecoration.Underline)) { append(stringResource(R.string.lib_terms)) }
        pop()
        append(stringResource(R.string.lib_and))
        pushStringAnnotation("link", "privacy")
        withStyle(SpanStyle(color = Fl.ink, textDecoration = TextDecoration.Underline)) { append(stringResource(R.string.lib_privacy)) }
        pop()
        append(".")
    }
    @Suppress("DEPRECATION")
    ClickableText(
        text, modifier,
        style = FlType.caption.copy(lineHeight = 18.sp, color = Fl.muted, textAlign = TextAlign.Center),
    ) { off ->
        text.getStringAnnotations("link", off, off).firstOrNull()?.let { open(if (it.item == "terms") LegalLinks.TERMS else LegalLinks.PRIVACY) }
    }
}

// ───────────────────────────── N15 · Sign in ─────────────────────────────

@Composable
fun SignInScreen() {
    val app = LocalAppState.current
    val toaster = LocalToaster.current
    val lead = stringResource(R.string.lib_not_configured_lead)
    val apple = stringResource(R.string.lib_apple_not_configured)
    val google = stringResource(R.string.lib_google_not_configured)
    Column(Modifier.fillMaxSize().background(Fl.paper).statusBarsPadding()) {
        Spacer(Modifier.height(4.dp))
        IconButtonFl(R.drawable.ic_close, stringResource(R.string.cd_close), { app.back() }, Modifier.padding(start = 8.dp), size = 44.dp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))
            sampleDesign()?.let { d ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 43.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    repeat(3) { i ->
                        Box(
                            Modifier.size(96.dp, 120.dp).shadow(10.dp, RoundedCornerShape(4.dp), ambientColor = Fl.ink.copy(.18f), spotColor = Fl.ink.copy(.18f))
                                .clip(RoundedCornerShape(4.dp)),
                        ) { SlideView(d, i, Modifier.size(96.dp, 120.dp), keepRatio = false) }
                    }
                }
            }
            Spacer(Modifier.height(34.dp))
            Column(Modifier.padding(horizontal = 24.dp)) {
                Text(stringResource(R.string.s_keep_your_designs_everywhere), style = FlType.screenTitle, color = Fl.ink)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.s_sign_in_to_sync_drafts_and_favourites_ac), style = FlType.body, color = Fl.muted)
                Spacer(Modifier.height(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Use Apple's and Google's official buttons (Credential Manager) when the IDs are configured.
                    FlButton(stringResource(R.string.s_continue_with_apple), { toaster.show(lead, apple, R.drawable.ic_info) }, Modifier.fillMaxWidth(), kind = BtnKind.Dark, large = true)
                    OutlinedStrong(stringResource(R.string.s_continue_with_google), null) { toaster.show(lead, google, R.drawable.ic_info) }
                    OutlinedStrong(stringResource(R.string.s_continue_with_email), R.drawable.ic_email) { app.navigate(Routes.SIGN_IN_EMAIL) }
                    FlTextButton(stringResource(R.string.s_continue_as_guest), { app.back() }, Modifier.fillMaxWidth().padding(top = 6.dp).height(44.dp), underline = true)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        LegalLine(Modifier.padding(horizontal = 32.dp).fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp))
    }
}

/** 52 dp white button with a 1 dp `fl_line_strong` border (sign-in options). */
@Composable
private fun OutlinedStrong(text: String, icon: Int?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(shape).background(Fl.card).border(1.dp, Fl.lineStrong, shape).clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { com.htr.collagemaker.ui.components.FlIcon(it, null, Fl.ink, 20.dp) }
        Text(text, style = FlType.bodyL.copy(fontWeight = FontWeight.SemiBold), color = Fl.ink)
    }
}

// ───────────────────────────── N15 · Sign in with email ─────────────────────────────

@Composable
fun SignInEmailScreen() {
    val app = LocalAppState.current
    var email by rememberSaveable { mutableStateOf("") }
    val valid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    var tried by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    fun send() {
        tried = true
        if (!valid) return
        val e = email.trim()
        val local = e.substringBefore("@")
        val name = local.split('.', '_', '-').filter { it.isNotBlank() }.joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        // No auth backend: the magic link is simulated and the account is created locally.
        app.container.prefs.signIn(e, name, local.lowercase().filter { it.isLetterOrDigit() || it == '.' || it == '_' })
        app.navigate(Routes.inbox(e))
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(Modifier.fillMaxSize().background(Fl.paper).statusBarsPadding().imePadding()) {
        Spacer(Modifier.height(4.dp))
        IconButtonFl(R.drawable.ic_back, stringResource(R.string.cd_back), { app.back() }, Modifier.padding(start = 4.dp), size = 44.dp, iconSize = 26.dp)
        Column(Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.s_sign_in_with_email), style = FlType.screenTitle, color = Fl.ink)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.s_we_ll_email_you_a_sign_in_link_no_passwo), style = FlType.body, color = Fl.muted)
            Spacer(Modifier.height(10.dp))
            Spacer(Modifier.height(0.dp))
            Text(stringResource(R.string.s_email_2), style = FieldLabel, color = Fl.muted)
            Spacer(Modifier.height(8.dp))
            PlainField(
                email, { email = it }, true, error = tried && !valid,
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                modifier = Modifier.focusRequester(focus), placeholder = "you@example.com", label = stringResource(R.string.s_email_2),
            )
            if (tried && !valid) Text(stringResource(R.string.lib_email_invalid), style = FlType.caption, color = Fl.danger, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(16.dp))
            FlButton(stringResource(R.string.s_send_link), ::send, Modifier.fillMaxWidth(), large = true, enabled = email.isNotBlank())
            Spacer(Modifier.height(16.dp))
            LegalLine(Modifier.fillMaxWidth())
        }
    }
}

// ───────────────────────────── N15 · Check your inbox ─────────────────────────────

@Composable
fun InboxScreen(email: String) {
    val app = LocalAppState.current
    val ctx = LocalContext.current
    val toaster = LocalToaster.current
    var seconds by remember { mutableIntStateOf(30) }
    var round by remember { mutableIntStateOf(0) }
    val sent = stringResource(R.string.lib_link_sent)
    LaunchedEffect(round) {
        seconds = 30
        while (seconds > 0) { delay(1000); seconds-- }
    }
    Column(Modifier.fillMaxSize().background(Fl.paper).statusBarsPadding()) {
        Spacer(Modifier.height(4.dp))
        IconButtonFl(R.drawable.ic_back, stringResource(R.string.cd_back), { app.back() }, Modifier.padding(start = 4.dp), size = 44.dp, iconSize = 26.dp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(30.dp))
            Image(painterResource(R.drawable.img_email_sent), null, Modifier.size(220.dp, 208.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.s_check_your_inbox), style = FlType.screenTitle, color = Fl.ink, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.lib_inbox_body_pre))
                    withStyle(SpanStyle(color = Fl.ink, fontWeight = FontWeight.SemiBold)) { append(email) }
                    append(stringResource(R.string.lib_inbox_body_post))
                },
                style = FlType.body, color = Fl.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 28.dp),
            )
        }
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlButton(
                stringResource(R.string.s_open_email_app),
                {
                    runCatching {
                        ctx.startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                },
                Modifier.fillMaxWidth(), large = true, leadingIcon = R.drawable.ic_email,
            )
            val countdown = seconds > 0
            Box(
                Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !countdown, role = Role.Button) { round++; toaster.show(null, sent, R.drawable.ic_check) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    buildAnnotatedString {
                        if (countdown) {
                            append(stringResource(R.string.lib_resend_in))
                            withStyle(SpanStyle(fontFamily = DmMono)) { append("%d:%02d".format(seconds / 60, seconds % 60)) }
                        } else append(stringResource(R.string.lib_resend))
                    },
                    style = FlType.body.copy(fontWeight = if (countdown) FontWeight.Medium else FontWeight.SemiBold),
                    color = if (countdown) Fl.muted else Fl.ink,
                )
            }
            FlTextButton(stringResource(R.string.s_use_a_different_email), { app.container.prefs.signOut(); app.back() }, Modifier.fillMaxWidth().height(44.dp), underline = true)
        }
    }
}

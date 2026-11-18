package com.framelab.app.feature.profile

import android.app.Activity
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.framelab.app.R
import com.framelab.app.nav.LocalAppState
import com.framelab.app.nav.Routes
import com.framelab.app.ui.components.FlButton
import com.framelab.app.ui.components.FlIcon
import com.framelab.app.ui.components.FlTextButton
import com.framelab.app.ui.components.IconButtonFl
import com.framelab.app.ui.components.LocalToaster
import com.framelab.app.ui.components.Tab
import com.framelab.app.ui.theme.Bricolage
import com.framelab.app.ui.theme.DmMono
import com.framelab.app.ui.theme.Fl
import com.framelab.app.ui.theme.FlType

// ───────────────────────────── N17 · FrameLab Pro (paywall) ─────────────────────────────

@Composable
fun PaywallScreen() {
    val app = LocalAppState.current
    val prefs = app.container.prefs
    val profile by prefs.profile.collectAsState()
    val toaster = LocalToaster.current
    var plan by rememberSaveable { mutableStateOf(0) }
    val none = stringResource(R.string.lib_restore_none)
    val restored = stringResource(R.string.lib_restore_done)

    Column(Modifier.fillMaxSize().background(Fl.paper).statusBarsPadding()) {
        Spacer(Modifier.height(4.dp))
        IconButtonFl(R.drawable.ic_close, stringResource(R.string.cd_close), { app.back() }, Modifier.padding(start = 8.dp), size = 44.dp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.img_pro_hero), null, Modifier.offset(y = (-24).dp).size(330.dp, 190.dp), contentScale = ContentScale.Fit)
            Column(Modifier.offset(y = (-24).dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(22.dp))
                Text(
                    stringResource(R.string.s_framelab_pro),
                    style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-1).sp),
                    color = Fl.ink, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.s_make_every_post_look_finished), style = FlType.body, color = Fl.muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Column(Modifier.fillMaxWidth().padding(start = 32.dp, end = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        R.string.s_every_template_plus_new_trend_drops, R.string.s_no_watermark_on_your_exports,
                        R.string.s_max_quality_hd_export_jpg_or_png, R.string.s_early_access_to_new_trends,
                    ).forEach { Benefit(stringResource(it), dark = false) }
                }
                Spacer(Modifier.height(30.dp))
                Row(Modifier.padding(horizontal = 20.dp).semantics { contentDescription = "Plan" }, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlanCard(
                        stringResource(R.string.s_yearly), stringResource(R.string.s_yearly_price), stringResource(R.string.s_per_year),
                        selected = plan == 0, tag = stringResource(R.string.s_best_value), modifier = Modifier.weight(1f),
                    ) { plan = 0 }
                    PlanCard(
                        stringResource(R.string.s_monthly), stringResource(R.string.s_monthly_price), stringResource(R.string.s_per_month),
                        selected = plan == 1, tag = null, modifier = Modifier.weight(1f),
                    ) { plan = 1 }
                }
            }
        }
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            FlButton(
                stringResource(R.string.s_start_free_trial),
                {
                    // TODO(billing): launch the Google Play Billing flow for the selected plan; unlock only on a verified purchase.
                    prefs.setPro(true)
                    app.nav.navigate(Routes.WELCOME_PRO) { popUpTo(Routes.PAYWALL) { inclusive = true } }
                },
                Modifier.fillMaxWidth(), large = true,
            )
            Spacer(Modifier.height(8.dp))
            FlTextButton(
                stringResource(R.string.s_restore_purchases),
                { toaster.show(null, if (profile.isPro) restored else none, if (profile.isPro) R.drawable.ic_check else R.drawable.ic_info) },
                Modifier.height(44.dp), underline = true, style = FlType.bodyS.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.s_trial_length_and_renewal_terms_cancel_an),
                style = TextStyle(fontFamily = com.framelab.app.ui.theme.Inter, fontSize = 11.sp, lineHeight = 16.sp),
                color = Fl.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
private fun Benefit(text: String, dark: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(24.dp).background(if (dark) Fl.accent else Fl.accentSoft, CircleShape), contentAlignment = Alignment.Center) {
            FlIcon(R.drawable.ic_check, null, if (dark) Fl.ink else Fl.accentInk, 14.dp)
        }
        Text(text, style = FlType.body.copy(lineHeight = 20.sp), color = if (dark) Color.White else Fl.ink)
    }
}

@Composable
private fun PlanCard(name: String, price: String, per: String, selected: Boolean, tag: String?, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().height(96.dp).clip(shape).background(Fl.card)
                .border(if (selected) 2.dp else 1.dp, if (selected) Fl.ink else Fl.line, shape)
                .clickable(role = Role.RadioButton, onClick = onClick).semantics { this.selected = selected }
                .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
        ) {
            Text(name, style = FlType.cardTitle, color = Fl.ink)
            Text(price, style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 22.sp), color = Fl.ink, maxLines = 1)
            Text(per, style = FlType.caption, color = Fl.muted)
        }
        if (tag != null) {
            Box(
                Modifier.offset(x = 12.dp, y = (-11).dp).height(22.dp).background(Fl.accent, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) { Text(tag, style = FlType.tag.copy(lineHeight = 22.sp), color = Fl.ink) }
        }
    }
}

// ───────────────────────────── N17 · Welcome to Pro ─────────────────────────────

@Composable
fun WelcomeProScreen() {
    val app = LocalAppState.current
    val view = LocalView.current
    val ctx = LocalContext.current
    // Light status-bar icons on the ink background.
    DisposableEffect(Unit) {
        val window = (ctx as? Activity)?.window
        val ctrl = window?.let { WindowCompat.getInsetsController(it, view) }
        val prev = ctrl?.isAppearanceLightStatusBars
        ctrl?.isAppearanceLightStatusBars = false
        ctrl?.isAppearanceLightNavigationBars = false
        onDispose { ctrl?.isAppearanceLightStatusBars = prev ?: true; ctrl?.isAppearanceLightNavigationBars = true }
    }
    Column(Modifier.fillMaxSize().background(Fl.ink).statusBarsPadding()) {
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().padding(end = 12.dp), horizontalArrangement = Arrangement.End) {
            Box(
                Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { app.back() }.padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.s_done), style = FlType.bodyL.copy(fontWeight = FontWeight.Bold), color = Color.White) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.img_pro_welcome_hero), null, Modifier.size(330.dp, 204.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.height(18.dp))
            Text(
                buildAnnotatedString {
                    val t = stringResource(R.string.s_you_re_pro)
                    append(t.removeSuffix("."))
                    if (t.endsWith(".")) withStyle(SpanStyle(color = Fl.accent)) { append(".") }
                },
                style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1.2).sp),
                color = Color.White, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.s_every_template_is_unlocked_and_your_expo),
                style = FlType.body, color = Color.White.copy(alpha = .76f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(30.dp))
            Column(Modifier.fillMaxWidth().padding(start = 40.dp, end = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(R.string.s_all_templates_including_new_trend_drops, R.string.s_max_quality_hd_export_jpg_or_png, R.string.s_no_watermark_early_access_to_trends)
                    .forEach { Benefit(stringResource(it), dark = true) }
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.s_trial_ends_date_manage_in_your_app_store),
                style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 16.sp, letterSpacing = 1.sp),
                color = Color.White.copy(alpha = .66f), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(42.dp))
            FlButton(stringResource(R.string.s_start_creating), { app.selectTab(Tab.Explore) }, Modifier.fillMaxWidth(), large = true)
        }
    }
}


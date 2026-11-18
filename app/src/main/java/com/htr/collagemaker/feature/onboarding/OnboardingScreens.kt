package com.htr.collagemaker.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htr.collagemaker.R
import com.htr.collagemaker.nav.LocalAppState
import com.htr.collagemaker.nav.Routes
import com.htr.collagemaker.ui.components.FlIcon
import com.htr.collagemaker.ui.components.Wordmark
import com.htr.collagemaker.ui.components.animationsEnabled
import com.htr.collagemaker.ui.theme.Fl
import com.htr.collagemaker.ui.theme.FlType
import com.htr.collagemaker.ui.theme.Inter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// ───────────────────────────── N1 · Splash ─────────────────────────────

/** N1 · Splash — shown after the system splash while settings and templates load (≤ 1 s). */
@Composable
fun SplashScreen() {
    val app = LocalAppState.current
    var step by remember { mutableIntStateOf(0) }
    val animate = animationsEnabled()

    LaunchedEffect(Unit) {
        if (animate) launch { while (true) { delay(330); step = (step + 1) % 3 } }
        val started = System.currentTimeMillis()
        val settings = app.container.prefs.settings.first { it != null }!!
        withTimeoutOrNull(1000) { app.container.templates.state.first { !it.loading } }
        val elapsed = System.currentTimeMillis() - started
        if (elapsed < 700) delay(700 - elapsed)
        // Something else (deep link / debug route) already moved on.
        if (app.nav.currentDestination?.route != Routes.SPLASH) return@LaunchedEffect
        if (settings.onboardingDone) app.goHome()
        else app.nav.navigate(Routes.ONBOARDING) { popUpTo(Routes.SPLASH) { inclusive = true } }
    }

    val loading = stringResource(R.string.cd_framelab_is_loading)
    Box(Modifier.fillMaxSize().background(Fl.paper)) {
        Column(
            Modifier.align(Alignment.Center).semantics { contentDescription = loading },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Wordmark(style = FlType.wordmarkSplash)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    val c by animateColorAsState(if (i == step) Fl.accent else Fl.track, tween(200), label = "bar")
                    Box(Modifier.size(22.dp, 6.dp).background(c, RoundedCornerShape(3.dp)))
                }
            }
        }
        Text(
            stringResource(R.string.s_seamless_carousels),
            style = FlType.monoLabel.copy(letterSpacing = 2.sp),
            color = Fl.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp),
        )
    }
}

// ───────────────────────────── N2 · Onboarding 1–3 ─────────────────────────────

private data class OnboardingPage(
    @DrawableRes val image: Int,
    val imageW: Dp,
    val imageH: Dp,
    /** Top of the illustration below the status-bar area (mockup y − 48). */
    val imageTop: Dp,
    @StringRes val title: Int,
    @StringRes val body: Int,
    @StringRes val step: Int,
)

private val pages = listOf(
    OnboardingPage(R.drawable.img_onboarding_flow, 390.dp, 270.dp, 112.dp, R.string.s_posts_that_flow, R.string.s_photos_and_frames_cross_from_one_slide_i, R.string.cd_step_1_of_3),
    OnboardingPage(R.drawable.img_onboarding_add, 300.dp, 370.dp, 32.dp, R.string.s_tap_to_add_photos, R.string.s_pick_your_photos_once_and_they_fill_the, R.string.cd_step_2_of_3),
    OnboardingPage(R.drawable.img_onboarding_save, 350.dp, 300.dp, 52.dp, R.string.s_save_post_in_order, R.string.s_save_turns_every_slide_into_a_full_size, R.string.cd_step_3_of_3),
)

/** N2 · Onboarding 1–3 ("Posts that flow" · "Tap + to add photos" · "Save & post in order"). */
@Composable
fun OnboardingScreen() {
    val app = LocalAppState.current
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex
    val toPrimer = { app.nav.navigate(Routes.PRIMER) { popUpTo(Routes.ONBOARDING) { inclusive = true } } }

    Box(Modifier.fillMaxSize().background(Fl.paper)) {
        // Illustration + copy swipe together.
        HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
            val p = pages[i]
            Box(Modifier.fillMaxSize()) {
                Image(
                    painterResource(p.image), null,
                    Modifier.statusBarsPadding().padding(top = p.imageTop).align(Alignment.TopCenter).size(p.imageW, p.imageH),
                    contentScale = ContentScale.Fit,
                )
                // Text block anchored to the button: dots sit 192 dp above the button top.
                Column(
                    Modifier.align(Alignment.BottomStart).navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, bottom = BUTTON_BOTTOM + 52.dp)
                        .height(192.dp),
                ) {
                    Spacer(Modifier.height(26.dp))
                    Text(stringResource(p.title), style = FlType.display, color = Fl.ink)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(p.body), style = FlType.bodyL, color = Fl.muted)
                }
            }
        }

        // Skip (pages 1–2).
        if (!last) {
            Box(
                Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 4.dp, end = 12.dp)
                    .height(44.dp).clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { toPrimer() }.padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.s_skip), style = FlType.button, color = Fl.ink) }
        }

        // Step dots (y 556 → 192 dp above the button), left-aligned at x 24.
        val stepLabel = stringResource(pages[pager.currentPage].step)
        Row(
            Modifier.align(Alignment.BottomStart).navigationBarsPadding()
                .padding(start = 24.dp, bottom = BUTTON_BOTTOM + 52.dp + 192.dp - 6.dp)
                .semantics { contentDescription = stepLabel },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(pages.size) { i ->
                val active = i == pager.currentPage
                val w by animateDpAsState(if (active) 18.dp else 6.dp, tween(200), label = "dot")
                Box(Modifier.size(w, 6.dp).background(if (active) Fl.ink else Fl.controlOff, RoundedCornerShape(3.dp)))
            }
        }

        // Next ×2 (ink), then Start creating (accent).
        PillButton(
            text = stringResource(if (last) R.string.s_start_creating else R.string.s_next_2),
            bg = if (last) Fl.accent else Fl.ink,
            fg = if (last) Fl.ink else Fl.card,
            style = if (last) FlType.buttonL else FlType.buttonL.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = BUTTON_BOTTOM).fillMaxWidth(),
        ) {
            if (last) toPrimer() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
        }
    }
}

/** Space between the bottom button and the navigation-bar inset (mockup: 44 dp incl. a 34 dp home indicator). */
private val BUTTON_BOTTOM = 20.dp

@Composable
internal fun PillButton(
    text: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = FlType.buttonL,
    height: Dp = 52.dp,
    @DrawableRes icon: Int? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier.height(height).clip(RoundedCornerShape(50)).background(bg)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { FlIcon(it, null, fg, 20.dp) }
        Text(text, style = style, color = fg)
    }
}

/** 44 dp plain text button (“Not now”). */
@Composable
internal fun PlainTextButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = FlType.button, color = Fl.ink) }
}

// ───────────────────────────── N2 · Photo-access primer ─────────────────────────────

/** Runtime permissions to read the gallery on this API level. */
fun photoPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

/** N2 · Photo-access primer — explains access, then shows the system prompt. */
@Composable
fun PrimerScreen() {
    val app = LocalAppState.current
    val finish = {
        app.container.prefs.setOnboardingDone()
        app.goHome()
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { finish() }

    Box(Modifier.fillMaxSize().background(Fl.paper)) {
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            Image(painterResource(R.drawable.img_photo_access), null, Modifier.size(250.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.s_let_framelab_use_your_photos),
                style = FlType.screenTitle, color = Fl.ink, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.s_your_phone_will_ask_next_here_s_what_acc),
                style = FlType.body, color = Fl.muted, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Reason(R.drawable.ic_photos_add, R.string.s_pick_the_photos_that_fill_each_template)
                Reason(R.drawable.ic_download, R.string.s_save_finished_slides_to_an_album_called)
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PillButton(stringResource(R.string.s_allow_photo_access), Fl.accent, Fl.ink, Modifier.fillMaxWidth()) {
                launcher.launch(photoPermissions())
            }
            PlainTextButton(stringResource(R.string.s_not_now), Modifier.fillMaxWidth()) { finish() }
        }
    }
}

@Composable
private fun Reason(@DrawableRes icon: Int, @StringRes text: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(44.dp).background(Fl.accentSoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            FlIcon(icon, null, Fl.accentInk, 22.dp)
        }
        Text(stringResource(text), style = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 21.sp), color = Fl.ink)
    }
}

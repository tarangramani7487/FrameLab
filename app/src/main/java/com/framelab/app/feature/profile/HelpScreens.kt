package com.framelab.app.feature.profile

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.framelab.app.R
import com.framelab.app.data.Settings
import com.framelab.app.feature.library.MonoCaption
import com.framelab.app.feature.library.SettingsCard
import com.framelab.app.feature.library.SettingsRow
import com.framelab.app.nav.LocalAppState
import com.framelab.app.nav.Routes
import com.framelab.app.render.StripView
import com.framelab.app.ui.components.BtnKind
import com.framelab.app.ui.components.FlButton
import com.framelab.app.ui.components.FlChip
import com.framelab.app.ui.components.FlIcon
import com.framelab.app.ui.components.FlSwitch
import com.framelab.app.ui.components.FlTopBar
import com.framelab.app.ui.components.LocalToaster
import com.framelab.app.ui.components.SearchField
import com.framelab.app.ui.components.TopBarTextAction
import com.framelab.app.ui.components.Wordmark
import com.framelab.app.ui.theme.DmMono
import com.framelab.app.ui.theme.Fl
import com.framelab.app.ui.theme.FlType

private val Body15 = TextStyle(fontFamily = com.framelab.app.ui.theme.Inter, fontSize = 15.sp, lineHeight = 20.sp)

fun openSupportEmail(ctx: android.content.Context, subject: String, body: String = "") {
    val uri = Uri.parse("mailto:${LegalLinks.SUPPORT_EMAIL}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
    runCatching { ctx.startActivity(Intent(Intent.ACTION_SENDTO, uri)) }
}

// ───────────────────────────── N12 · Help & FAQ ─────────────────────────────

@Composable
fun HelpScreen() {
    val app = LocalAppState.current
    val ctx = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var open by rememberSaveable { mutableStateOf(0) }
    val faq = listOf(
        stringResource(R.string.s_how_do_seamless_carousels_work) to stringResource(R.string.s_each_slide_saves_as_its_own_image_some_p),
        stringResource(R.string.s_why_does_one_photo_spot_cover_two_slides) to stringResource(R.string.lib_faq_a2),
        stringResource(R.string.s_how_do_i_post_the_slides_in_the_right_or) to stringResource(R.string.lib_faq_a3),
        stringResource(R.string.s_where_are_my_drafts_kept) to stringResource(R.string.lib_faq_a4),
        stringResource(R.string.s_how_do_i_restore_framelab_pro) to stringResource(R.string.lib_faq_a5),
    )
    val q = query.trim()
    val shown = faq.withIndex().filter { (_, f) -> q.isEmpty() || f.first.contains(q, true) || f.second.contains(q, true) }
    val supportSubject = stringResource(R.string.lib_support_subject)

    Column(Modifier.fillMaxSize().background(Fl.paper).statusBarsPadding()) {
        Spacer(Modifier.height(4.dp))
        FlTopBar(stringResource(R.string.s_help_feedback), { app.back() }, backLabel = stringResource(R.string.cd_back))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(12.dp))
            SearchField(query, { query = it }, stringResource(R.string.s_search_help), Modifier.fillMaxWidth(), clearLabel = stringResource(R.string.cd_clear_search))
            Spacer(Modifier.height(16.dp))
            MonoCaption(stringResource(R.string.s_common_questions))
            SettingsCard(Modifier.semantics { contentDescription = "Common questions" }) {
                if (shown.isEmpty()) {
                    Text(stringResource(R.string.lib_no_faq, q), style = FlType.bodyS, color = Fl.muted, modifier = Modifier.padding(14.dp))
                }
                shown.forEachIndexed { pos, (i, f) ->
                    val expanded = open == i || q.isNotEmpty()
                    Column(Modifier.fillMaxWidth().animateContentSize()) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) { open = if (open == i) -1 else i }
                                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(f.first, style = Body15.copy(fontWeight = FontWeight.SemiBold), color = Fl.ink, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
                            FlIcon(if (expanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down, null, Fl.muted, 18.dp)
                        }
                        if (expanded) Text(f.second, style = FlType.bodyS.copy(lineHeight = 21.sp), color = Fl.muted, modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp))
                        if (pos != shown.lastIndex) Box(Modifier.padding(horizontal = 14.dp).fillMaxWidth().height(1.dp).background(Fl.sunk))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier.fillMaxWidth().background(Fl.ink, RoundedCornerShape(18.dp)).padding(16.dp).semantics { contentDescription = "Still stuck" },
            ) {
                Text(stringResource(R.string.s_still_stuck), style = FlType.section.copy(fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.sp), color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.s_tell_us_what_happened_or_ask_for_a_templ), style = FlType.secondary.copy(lineHeight = 19.sp), color = Color.White.copy(alpha = .74f))
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlButton(stringResource(R.string.s_send_feedback), { app.navigate(Routes.FEEDBACK) }, Modifier.weight(1f), height = 44.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp))
                    FlButton(stringResource(R.string.s_email_support), { openSupportEmail(ctx, supportSubject) }, Modifier.weight(1f), kind = BtnKind.OutlinedOnInk, height = 44.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp))
                }
            }
            Spacer(Modifier.height(24.dp).navigationBarsPadding())
        }
    }
}

// ───────────────────────────── N12 · Send feedback ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackScreen() {
    val app = LocalAppState.current
    val c = app.container
    val toaster = LocalToaster.current
    val settings = c.prefs.settings.collectAsState().value ?: Settings()
    val topics = listOf(
        stringResource(R.string.s_something_s_broken), stringResource(R.string.s_idea),
        stringResource(R.string.s_template_request), stringResource(R.string.s_other),
    )
    var topic by rememberSaveable { mutableStateOf(1) }
    var message by rememberSaveable { mutableStateOf("") }
    val shots = remember { mutableStateListOf<Uri>() }
    var includeInfo by remember(settings.includeDeviceInfo) { mutableStateOf(settings.includeDeviceInfo) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(3)) { uris ->
        uris.forEach { if (shots.size < 3 && it !in shots) shots.add(it) }
    }
    val sentLead = stringResource(R.string.lib_feedback_sent_lead)
    val sentBody = stringResource(R.string.lib_feedback_sent)

    Column(Modifier.fillMaxSize().background(Fl.paper).statusBarsPadding().imePadding()) {
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(80.dp)) { TopBarTextAction(stringResource(R.string.s_cancel), { app.back() }) }
            Text(stringResource(R.string.s_send_feedback), style = FlType.navTitle, color = Fl.ink, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(80.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Spacer(Modifier.height(0.dp))
            Column {
                Text(stringResource(R.string.s_what_s_it_about), style = FieldLabel, color = Fl.muted)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topics.forEachIndexed { i, t -> FlChip(t, { topic = i }, selected = topic == i, textStyle = FlType.chipHome) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.s_tell_us_more), style = FieldLabel, color = Fl.muted)
                var focused by remember { mutableStateOf(false) }
                val shape = RoundedCornerShape(14.dp)
                BasicTextField(
                    message, { message = it },
                    textStyle = FlType.body.copy(color = Fl.ink),
                    cursorBrush = SolidColor(Fl.accent),
                    modifier = Modifier.fillMaxWidth().height(132.dp).onFocusChanged { focused = it.isFocused }
                        .semantics { contentDescription = "Tell us more" },
                    decorationBox = { inner ->
                        Box(
                            Modifier.fillMaxSize().background(Fl.card, shape)
                                .border(if (focused) 2.dp else 1.dp, if (focused) Fl.ink else Fl.line, shape)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            if (message.isEmpty()) Text(stringResource(R.string.s_a_concert_night_template_dark_background), style = FlType.body, color = Fl.disabledText)
                            inner()
                        }
                    },
                )
            }
            Column {
                Text(stringResource(R.string.s_screenshots), style = FieldLabel, color = Fl.muted)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    shots.forEach { u ->
                        Box(Modifier.size(64.dp, 80.dp).clip(RoundedCornerShape(10.dp)).background(Fl.sunk)) {
                            AsyncImage(u, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Box(
                                Modifier.align(Alignment.TopEnd).size(30.dp).clickable(role = Role.Button) { shots.remove(u) }
                                    .semantics { contentDescription = "Remove screenshot" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(Modifier.size(22.dp).background(Fl.ink, CircleShape), contentAlignment = Alignment.Center) { FlIcon(R.drawable.ic_close, null, Color.White, 12.dp) }
                            }
                        }
                    }
                    if (shots.size < 3) {
                        Box(
                            Modifier.size(64.dp, 80.dp).clip(RoundedCornerShape(10.dp))
                                .drawBehind {
                                    drawRoundRect(
                                        Fl.controlOff, cornerRadius = CornerRadius(10.dp.toPx()),
                                        style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
                                    )
                                }
                                .clickable(role = Role.Button) { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                                .semantics { contentDescription = "Add screenshot" },
                            contentAlignment = Alignment.Center,
                        ) { FlIcon(R.drawable.ic_add, null, Fl.muted, 22.dp) }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().background(Fl.card, RoundedCornerShape(16.dp)).border(1.dp, Fl.line, RoundedCornerShape(16.dp))
                    .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.s_include_device_and_app_info), style = Body15, color = Fl.ink)
                    Text(stringResource(R.string.s_helps_us_fix_problems_faster), style = FlType.caption, color = Fl.muted)
                }
                FlSwitch(includeInfo, { includeInfo = it; c.prefs.updateSettings(settings.copy(includeDeviceInfo = it)) }, label = stringResource(R.string.cd_include_device_and_app_info))
            }
            Spacer(Modifier.height(8.dp))
        }
        FlButton(
            stringResource(R.string.s_send_feedback),
            {
                // No feedback backend is configured: the message is acknowledged locally.
                val info = if (includeInfo) "\n\n${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}" else ""
                @Suppress("UNUSED_VARIABLE") val payload = "${topics[topic]}\n$message$info"
                toaster.show(sentLead, sentBody, R.drawable.ic_check)
                app.back()
            },
            Modifier.padding(horizontal = 20.dp).fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp),
            large = true, enabled = message.isNotBlank(),
        )
    }
}

// ───────────────────────────── N12 · About ─────────────────────────────

@Composable
fun AboutScreen() {
    val app = LocalAppState.current
    val open = rememberLinkOpener()
    Column(Modifier.fillMaxSize().background(Fl.paper).statusBarsPadding()) {
        Spacer(Modifier.height(4.dp))
        FlTopBar(stringResource(R.string.s_about), { app.back() }, backLabel = stringResource(R.string.cd_back))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(28.dp))
            Wordmark(style = FlType.wordmark.copy(fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1.4).sp))
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.s_seamless_carousels_frames_collages), style = FlType.bodyS, color = Fl.muted)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.s_version_x_y_z), style = FlType.monoLabel, color = Fl.muted)
            Spacer(Modifier.height(28.dp))
            sampleDesign()?.let {
                Box(
                    Modifier.width(360.dp)
                        .then(Modifier.semantics { })
                        .clip(RoundedCornerShape(6.dp)),
                ) { StripView(it, Modifier.width(360.dp)) }
            }
            Spacer(Modifier.height(29.dp))
            Box(Modifier.padding(horizontal = 20.dp).semantics { contentDescription = "About links" }) {
                SettingsCard {
                    AboutRow(stringResource(R.string.s_website), stringResource(R.string.s_website_2)) { open(LegalLinks.WEBSITE) }
                    AboutRow(stringResource(R.string.s_follow_on_instagram), stringResource(R.string.s_handle_2)) { open(LegalLinks.INSTAGRAM) }
                    AboutRow(stringResource(R.string.s_terms_of_service), null) { open(LegalLinks.TERMS) }
                    AboutRow(stringResource(R.string.s_privacy_policy), null) { open(LegalLinks.PRIVACY) }
                    AboutRow(stringResource(R.string.s_licences), null, last = true, chevron = true) { app.navigate(Routes.LICENCES) }
                }
            }
            Spacer(Modifier.height(48.dp))
            Text(
                stringResource(R.string.s_made_by_company_name), style = FlType.caption.copy(lineHeight = 18.sp), color = Fl.muted,
                textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 24.dp).navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String?, last: Boolean = false, chevron: Boolean = false, onClick: () -> Unit) {
    SettingsRow(last = last, minHeight = 52.dp, onClick = onClick) {
        Text(label, style = Body15, color = Fl.ink, modifier = Modifier.weight(1f))
        if (value != null) Text(value, style = FlType.secondary, color = Fl.muted)
        FlIcon(if (chevron) R.drawable.ic_chevron_right else R.drawable.ic_external, null, Fl.muted, 18.dp)
    }
}

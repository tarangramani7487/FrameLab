package com.htr.collagemaker.engage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.htr.collagemaker.R
import com.htr.collagemaker.feature.profile.FieldLabel
import com.htr.collagemaker.ui.components.BtnKind
import com.htr.collagemaker.ui.components.EdgeToEdgeDialogWindow
import com.htr.collagemaker.ui.components.FlButton
import com.htr.collagemaker.ui.components.FlIcon
import com.htr.collagemaker.ui.components.LocalToaster
import com.htr.collagemaker.ui.theme.Fl
import com.htr.collagemaker.ui.theme.FlType
import kotlinx.coroutines.launch

/** Centred card over a scrim (same look as FlDialog), kept above the keyboard. */
@Composable
private fun EngageModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    if (!state.currentState && !state.targetState && state.isIdle) return
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
            dismissOnBackPress = dismissible, dismissOnClickOutside = false,
        ),
    ) {
        EdgeToEdgeDialogWindow()
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(state, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
                Box(
                    Modifier.fillMaxSize().background(Fl.scrim)
                        .clickable(remember { MutableInteractionSource() }, null) { if (dismissible) onDismiss() },
                )
            }
            Box(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.ime)).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    state,
                    enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.94f),
                    exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.96f),
                ) {
                    val shape = RoundedCornerShape(24.dp)
                    Column(
                        Modifier.widthIn(max = 334.dp).fillMaxWidth()
                            .shadow(25.dp, shape, ambientColor = Fl.ink.copy(alpha = .3f), spotColor = Fl.ink.copy(alpha = .3f))
                            .background(Fl.card, shape)
                            .clickable(remember { MutableInteractionSource() }, null) {}
                            .semantics { paneTitle = title }
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModalIcon(icon: Int) {
    Box(Modifier.size(52.dp).background(Fl.accent, CircleShape), contentAlignment = Alignment.Center) {
        FlIcon(icon, null, Fl.ink, 24.dp)
    }
}

// ───────────────────────────── Are you getting issues? ─────────────────────────────

/** Title + description → Firestore `issue_reports`. Opened from Settings and the launcher quick action. */
@Composable
fun ReportIssueDialog(visible: Boolean, onDismiss: () -> Unit) {
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val dialogTitle = stringResource(R.string.ri_title)
    val sentLead = stringResource(R.string.ri_sent_lead)
    val sent = stringResource(R.string.ri_sent)
    val queued = stringResource(R.string.ri_queued)
    val failedLead = stringResource(R.string.ri_failed_lead)
    val failed = stringResource(R.string.ri_failed)

    fun close() { if (!sending) onDismiss() }

    EngageModal(visible, ::close, dialogTitle) {
        ModalIcon(R.drawable.ic_help)
        Spacer(Modifier.height(14.dp))
        Text(dialogTitle, style = FlType.dialogTitle, color = Fl.ink)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.ri_body), style = FlType.bodyS, color = Fl.muted)
        Spacer(Modifier.height(18.dp))

        Text(stringResource(R.string.ri_field_title), style = FieldLabel, color = Fl.muted)
        Spacer(Modifier.height(8.dp))
        Field(
            title, { title = it.take(IssueReports.TITLE_MAX) },
            placeholder = stringResource(R.string.ri_field_title_hint),
            label = stringResource(R.string.ri_field_title),
            singleLine = true, height = 52.dp,
            modifier = Modifier.focusRequester(focus),
        )
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.ri_field_description), style = FieldLabel, color = Fl.muted)
        Spacer(Modifier.height(8.dp))
        Field(
            description, { description = it.take(IssueReports.DESCRIPTION_MAX) },
            placeholder = stringResource(R.string.ri_field_description_hint),
            label = stringResource(R.string.ri_field_description),
            singleLine = false, height = 132.dp,
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.ri_device_note), style = FlType.caption, color = Fl.muted)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlButton(stringResource(R.string.s_cancel), ::close, Modifier.weight(1f), kind = BtnKind.Secondary, enabled = !sending)
            FlButton(
                stringResource(R.string.ri_submit),
                {
                    sending = true
                    scope.launch {
                        when (IssueReports.submit(title, description)) {
                            IssueReports.Result.Sent -> toaster.show(sentLead, sent, R.drawable.ic_check)
                            IssueReports.Result.Queued -> toaster.show(sentLead, queued, R.drawable.ic_offline)
                            IssueReports.Result.Failed -> { toaster.show(failedLead, failed, R.drawable.ic_error); sending = false; return@launch }
                        }
                        sending = false
                        title = ""; description = ""
                        onDismiss()
                    }
                },
                Modifier.weight(1f),
                enabled = title.isNotBlank() && description.isNotBlank(),
                loading = sending,
            )
        }
    }
    LaunchedEffect(visible) { if (visible) runCatching { focus.requestFocus() } }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String,
    singleLine: Boolean,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value, onValueChange,
        singleLine = singleLine,
        textStyle = FlType.body.copy(color = Fl.ink),
        cursorBrush = SolidColor(Fl.accent),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
        modifier = modifier.fillMaxWidth().height(height).onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = label },
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxSize().background(Fl.card, shape)
                    .border(if (focused) 2.dp else 1.dp, if (focused) Fl.ink else Fl.line, shape)
                    .padding(horizontal = 14.dp, vertical = if (singleLine) 0.dp else 14.dp),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty()) Text(placeholder, style = FlType.body, color = Fl.disabledText)
                inner()
            }
        },
    )
}

// ───────────────────────────── Force update ─────────────────────────────

/** Shown while Remote Config `force_app_update` is above this version. Can't be closed. */
@Composable
fun ForceUpdateDialog() {
    val required by AppUpdate.required.collectAsState()
    val ctx = LocalContext.current
    val r = required
    val title = r?.title?.ifBlank { null } ?: stringResource(R.string.up_title)
    val message = r?.message?.ifBlank { null } ?: stringResource(R.string.up_body)
    EngageModal(visible = r != null, onDismiss = {}, title = title, dismissible = false) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ModalIcon(R.drawable.ic_download)
            Spacer(Modifier.height(14.dp))
            Text(title, style = FlType.dialogTitle, color = Fl.ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(message, style = FlType.bodyS, color = Fl.muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            FlButton(
                stringResource(R.string.up_action),
                { AppUpdate.openStore(ctx, r?.url.orEmpty()) },
                Modifier.fillMaxWidth(), large = true,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.up_version, com.htr.collagemaker.BuildConfig.VERSION_NAME, r?.minVersion.orEmpty()),
                style = FlType.caption.copy(fontWeight = FontWeight.Medium), color = Fl.muted, textAlign = TextAlign.Center,
            )
        }
    }
}

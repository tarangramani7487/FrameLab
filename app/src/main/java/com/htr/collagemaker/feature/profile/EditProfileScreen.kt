package com.htr.collagemaker.feature.profile

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htr.collagemaker.R
import com.htr.collagemaker.nav.LocalAppState
import com.htr.collagemaker.ui.components.BtnKind
import com.htr.collagemaker.ui.components.FlButton
import com.htr.collagemaker.ui.components.FlIcon
import com.htr.collagemaker.ui.components.FlSheet
import com.htr.collagemaker.ui.components.FlTextButton
import com.htr.collagemaker.ui.components.LocalToaster
import com.htr.collagemaker.ui.components.TopBarTextAction
import com.htr.collagemaker.ui.theme.DmMono
import com.htr.collagemaker.ui.theme.Fl
import com.htr.collagemaker.ui.theme.FlType
import java.io.File

/** Handles already in use (no backend: a local reserved list stands in for the availability check). */
private val TakenHandles = setOf("sam", "framelab", "admin", "support", "help", "instagram", "test", "official")
private val HandleRegex = Regex("^[a-z0-9._]{2,30}$")

private fun suggestions(handle: String): List<String> = listOf("$handle.shoots.26", "${handle}shoots", "${handle}_frames")

/** N14 · Edit profile (+ Change photo sheet, Handle taken). */
@Composable
fun EditProfileScreen() {
    val app = LocalAppState.current
    val prefs = app.container.prefs
    val profile by prefs.profile.collectAsState()
    val ctx = LocalContext.current
    val toaster = LocalToaster.current
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var handle by rememberSaveable { mutableStateOf(profile.handle) }
    var avatar by rememberSaveable { mutableStateOf(profile.avatarUri) }
    var photoSheet by remember { mutableStateOf(false) }
    val savedText = stringResource(R.string.lib_profile_saved)

    val taken = handle.isNotEmpty() && handle != profile.handle && handle.lowercase() in TakenHandles
    val invalid = handle.isNotEmpty() && !HandleRegex.matches(handle)
    val canSave = name.isNotBlank() && handle.isNotEmpty() && !taken && !invalid

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
        if (bmp != null) avatar = saveAvatar(ctx, bmp)
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) avatar = copyAvatar(ctx, uri)
    }

    Column(Modifier.fillMaxSize().background(Fl.paper).statusBarsPadding().imePadding()) {
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(80.dp)) { TopBarTextAction(stringResource(R.string.s_cancel), { app.back() }) }
            Text(stringResource(R.string.s_edit_profile), style = FlType.navTitle, color = Fl.ink, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Box(Modifier.width(80.dp), contentAlignment = Alignment.CenterEnd) {
                TopBarTextAction(stringResource(R.string.s_save), {
                    prefs.updateProfile(name.trim(), handle, avatar)
                    toaster.show(null, savedText, R.drawable.ic_check)
                    app.back()
                }, emphasis = true, enabled = canSave)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.size(96.dp).clickable(role = Role.Button) { photoSheet = true }
                    .semantics { contentDescription = "Change profile photo" },
            ) {
                Avatar(initialsOf(name), 96, avatar)
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(34.dp).background(Fl.paper, CircleShape).padding(3.dp)
                        .background(Fl.ink, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { FlIcon(R.drawable.ic_camera, null, Fl.card, 16.dp) }
            }
            Spacer(Modifier.height(8.dp))
            FlTextButton(stringResource(R.string.s_change_photo), { photoSheet = true }, Modifier.height(44.dp), underline = true)
            Spacer(Modifier.height(20.dp))
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                LabeledField(stringResource(R.string.s_name)) { focused ->
                    PlainField(name, { name = it }, focused, error = false, capitalize = true)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.s_handle), style = FieldLabel, color = Fl.muted)
                    var focused by remember { mutableStateOf(false) }
                    HandleField(handle, { handle = it.lowercase().replace(" ", "") }, taken || invalid, focused) { focused = it }
                    when {
                        taken -> {
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(color = Fl.danger, fontWeight = FontWeight.SemiBold)) { append(stringResource(R.string.lib_handle_taken_lead)) }
                                    append(" ")
                                    append(stringResource(R.string.lib_handle_try))
                                },
                                style = FlType.caption.copy(lineHeight = 17.sp), color = Fl.muted,
                            )
                            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                suggestions(handle).filter { it !in TakenHandles }.forEach { s ->
                                    Box(
                                        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Fl.card).border(1.dp, Fl.line, RoundedCornerShape(18.dp))
                                            .clickable(role = Role.Button) { handle = s }.padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) { Text("@$s", style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 13.sp), color = Fl.ink, maxLines = 1) }
                                }
                            }
                        }
                        invalid -> Text(stringResource(R.string.lib_handle_invalid), style = FlType.caption.copy(lineHeight = 17.sp), color = Fl.danger)
                        else -> Text(stringResource(R.string.s_shown_on_links_you_share_letters_numbers), style = FlType.caption.copy(lineHeight = 17.sp), color = Fl.muted)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    FlSheet(photoSheet, { photoSheet = false }, paneTitle = stringResource(R.string.cd_profile_photo)) {
        Text(stringResource(R.string.s_profile_photo), style = FlType.navTitle, color = Fl.ink, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
        SheetRow(R.drawable.ic_camera, stringResource(R.string.s_take_photo)) { photoSheet = false; runCatching { takePhoto.launch(null) } }
        SheetRow(R.drawable.ic_image, stringResource(R.string.s_choose_from_library)) {
            photoSheet = false; pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        SheetRow(R.drawable.ic_delete, stringResource(R.string.s_remove_photo), danger = true, last = true) { photoSheet = false; avatar = null }
        Spacer(Modifier.height(12.dp))
        FlButton(stringResource(R.string.s_cancel), { photoSheet = false }, Modifier.fillMaxWidth(), kind = BtnKind.Secondary, large = true)
        Spacer(Modifier.height(32.dp))
    }
}

private fun initialsOf(name: String) = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

val FieldLabel = TextStyle(fontFamily = com.htr.collagemaker.ui.theme.Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp)

@Composable
fun LabeledField(label: String, field: @Composable (focused: Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = FieldLabel, color = Fl.muted)
        field(false)
    }
}

/** 52 dp white field, 1 dp line (2 dp ink when focused, 2 dp danger on error). */
@Composable
fun PlainField(
    value: String,
    onChange: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") focusedHint: Boolean,
    error: Boolean,
    capitalize: Boolean = false,
    mono: Boolean = false,
    keyboard: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = if (capitalize) KeyboardCapitalization.Words else KeyboardCapitalization.None),
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value, onChange, singleLine = true,
        textStyle = if (mono) TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = 2.sp, color = Fl.ink)
        else FlType.bodyL.copy(fontWeight = FontWeight.Medium, color = Fl.ink),
        cursorBrush = SolidColor(Fl.accent),
        keyboardOptions = keyboard,
        modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
            .semantics { if (label != null) contentDescription = label; if (error) this.error("Invalid") },
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().height(52.dp).background(Fl.card, shape)
                    .border(if (focused || error) 2.dp else 1.dp, when { error -> Fl.danger; focused -> Fl.ink; else -> Fl.line }, shape)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && placeholder != null) Text(placeholder, style = FlType.bodyL, color = Fl.muted)
                inner()
            }
        },
    )
}

@Composable
private fun HandleField(value: String, onChange: (String) -> Unit, error: Boolean, focused: Boolean, onFocus: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value, onChange, singleLine = true,
        textStyle = FlType.bodyL.copy(fontWeight = FontWeight.Medium, color = Fl.ink),
        cursorBrush = SolidColor(Fl.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().onFocusChanged { onFocus(it.isFocused) }
            .semantics { contentDescription = "Handle"; if (error) this.error("That handle is taken.") },
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().height(52.dp).background(Fl.card, shape)
                    .border(if (focused || error) 2.dp else 1.dp, when { error -> Fl.danger; focused -> Fl.ink; else -> Fl.line }, shape)
                    .padding(start = 16.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("@", style = FlType.bodyL.copy(fontWeight = FontWeight.Medium), color = Fl.muted)
                Box(Modifier.weight(1f)) { inner() }
                if (error) FlIcon(R.drawable.ic_error, null, Fl.danger, 20.dp)
            }
        },
    )
}

@Composable
fun SheetRow(icon: Int, label: String, danger: Boolean = false, last: Boolean = false, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.fillMaxWidth().height(55.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val c = if (danger) Fl.danger else Fl.ink
            FlIcon(icon, null, c, 22.dp)
            Text(label, style = FlType.bodyL.copy(fontWeight = FontWeight.Medium), color = c)
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Fl.sunk))
    }
}

private fun saveAvatar(ctx: Context, bmp: Bitmap): String {
    val f = File(ctx.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    return Uri.fromFile(f).toString()
}

private fun copyAvatar(ctx: Context, uri: Uri): String? = runCatching {
    val f = File(ctx.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
    ctx.contentResolver.openInputStream(uri)?.use { input -> f.outputStream().use { input.copyTo(it) } }
    Uri.fromFile(f).toString()
}.getOrNull()

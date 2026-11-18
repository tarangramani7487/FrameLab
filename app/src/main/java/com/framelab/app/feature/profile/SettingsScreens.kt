package com.framelab.app.feature.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.imageLoader
import com.framelab.app.R
import com.framelab.app.data.FileFormat
import com.framelab.app.data.Quality
import com.framelab.app.data.Settings
import com.framelab.app.data.ThemeMode
import com.framelab.app.feature.library.MiniSegmented
import com.framelab.app.feature.library.MonoCaption
import com.framelab.app.feature.library.MonoTag
import com.framelab.app.feature.library.RowTexts
import com.framelab.app.feature.library.SettingsCard
import com.framelab.app.feature.library.SettingsRow
import com.framelab.app.feature.library.openUrl
import com.framelab.app.nav.LocalAppState
import com.framelab.app.nav.Routes
import com.framelab.app.ui.components.BtnKind
import com.framelab.app.ui.components.FlButton
import com.framelab.app.ui.components.FlDialog
import com.framelab.app.ui.components.FlIcon
import com.framelab.app.ui.components.FlSheet
import com.framelab.app.ui.components.FlSwitch
import com.framelab.app.ui.components.FlTopBar
import com.framelab.app.ui.components.LocalToaster
import com.framelab.app.ui.components.SheetHeader
import com.framelab.app.ui.theme.Bricolage
import com.framelab.app.ui.theme.DmMono
import com.framelab.app.ui.theme.Fl
import com.framelab.app.ui.theme.FlType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Legal links — placeholders until launch (HANDOFF §10). Fill in to make the rows open a browser. */
object LegalLinks {
    const val TERMS = ""
    const val PRIVACY = ""
    const val WEBSITE = ""
    const val INSTAGRAM = ""
    const val SUPPORT_EMAIL = ""
}

private val RowText = TextStyle(fontFamily = com.framelab.app.ui.theme.Inter, fontSize = 15.sp, lineHeight = 20.sp)

/** Opens a legal link or tells the user it is still a placeholder. */
@Composable
fun rememberLinkOpener(): (String) -> Unit {
    val ctx = LocalContext.current
    val toaster = LocalToaster.current
    val lead = stringResource(R.string.lib_not_configured_lead)
    val body = stringResource(R.string.lib_link_placeholder)
    return remember { { url: String -> if (url.isBlank() || !openUrl(ctx, url)) toaster.show(lead, body, R.drawable.ic_info) } }
}

/** N16 · Settings (scrolls; 390 × 1560 in the design). */
@Composable
fun SettingsScreen() {
    val app = LocalAppState.current
    val c = app.container
    val ctx = LocalContext.current
    val toaster = LocalToaster.current
    val s = c.prefs.settings.collectAsState().value ?: Settings()
    val profile by c.prefs.profile.collectAsState()
    val drafts by c.drafts.drafts.collectAsState()
    fun update(n: Settings) = c.prefs.updateSettings(n)
    var signOut by remember { mutableStateOf(false) }
    var deleteDrafts by remember { mutableStateOf(false) }
    var cacheBytes by remember { mutableLongStateOf(-1L) }
    var cacheTick by remember { mutableStateOf(0) }
    val open = rememberLinkOpener()
    val clearedText = stringResource(R.string.lib_cache_cleared)
    val draftsDeleted = stringResource(R.string.lib_drafts_deleted)
    val signedOut = stringResource(R.string.lib_signed_out)

    LaunchedEffect(cacheTick) {
        cacheBytes = withContext(Dispatchers.IO) {
            (ctx.imageLoader.diskCache?.size ?: 0L) + dirSize(ctx.cacheDir)
        }
    }
    val photosAccess = photoAccessLabel()

    Column(Modifier.fillMaxSize().background(Fl.paper).statusBarsPadding()) {
        Spacer(Modifier.height(4.dp))
        FlTopBar(stringResource(R.string.s_settings_2), { app.back() }, backLabel = stringResource(R.string.cd_back))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // EXPORT
            Column {
                MonoCaption(stringResource(R.string.s_export))
                SettingsCard {
                    SettingsRow(minHeight = 60.dp) {
                        RowTexts(stringResource(R.string.s_quality), stringResource(R.string.s_max_makes_bigger_files), titleStyle = RowText)
                        MiniSegmented(listOf(stringResource(R.string.s_high), stringResource(R.string.s_max)), s.quality.ordinal, { update(s.copy(quality = Quality.entries[it])) }, stringResource(R.string.cd_quality))
                    }
                    SettingsRow {
                        Text(stringResource(R.string.s_file_format), style = RowText, color = Fl.ink, modifier = Modifier.weight(1f))
                        MiniSegmented(listOf(stringResource(R.string.s_jpg), stringResource(R.string.s_png)), s.format.ordinal, { update(s.copy(format = FileFormat.entries[it])) }, stringResource(R.string.cd_file_format))
                    }
                    SettingsRow(minHeight = 64.dp) {
                        RowTexts(stringResource(R.string.s_number_slides_in_file_names), stringResource(R.string.s_torn_diary_01_jpg_02_03), monoSubtitle = true, titleStyle = RowText)
                        FlSwitch(s.numberFiles, { update(s.copy(numberFiles = it)) }, label = stringResource(R.string.cd_number_slides_in_file_names))
                    }
                    SettingsRow(last = true) {
                        Text(stringResource(R.string.s_save_to_an_album_called_framelab), style = RowText, color = Fl.ink, modifier = Modifier.weight(1f))
                        FlSwitch(s.album, { update(s.copy(album = it)) }, label = stringResource(R.string.cd_save_to_framelab_album))
                    }
                }
            }
            // EDITOR
            Column {
                MonoCaption(stringResource(R.string.s_editor))
                SettingsCard {
                    SettingsRow {
                        Text(stringResource(R.string.s_always_show_photo_order_numbers), style = RowText, color = Fl.ink, modifier = Modifier.weight(1f))
                        FlSwitch(s.alwaysNumbers, { update(s.copy(alwaysNumbers = it)) }, label = stringResource(R.string.cd_always_show_photo_order_numbers))
                    }
                    SettingsRow {
                        Text(stringResource(R.string.s_snap_rotation_to_90), style = RowText, color = Fl.ink, modifier = Modifier.weight(1f))
                        FlSwitch(s.snapRotation, { update(s.copy(snapRotation = it)) }, label = stringResource(R.string.cd_snap_rotation_to_90_degrees))
                    }
                    SettingsRow(last = true) {
                        Text(stringResource(R.string.s_haptics), style = RowText, color = Fl.ink, modifier = Modifier.weight(1f))
                        FlSwitch(s.haptics, { update(s.copy(haptics = it)) }, label = stringResource(R.string.cd_haptics))
                    }
                }
            }
            // APPEARANCE
            Column {
                MonoCaption(stringResource(R.string.s_appearance))
                SettingsCard {
                    SettingsRow(minHeight = 64.dp, last = true) {
                        RowTexts(stringResource(R.string.s_theme), stringResource(R.string.s_the_editor_always_stays_dark), titleStyle = RowText)
                        MiniSegmented(
                            listOf(stringResource(R.string.s_light), stringResource(R.string.s_dark), stringResource(R.string.s_system)),
                            s.theme.ordinal, { update(s.copy(theme = ThemeMode.entries[it])) }, stringResource(R.string.cd_theme), padH = 10.dp,
                        )
                    }
                }
            }
            // PERMISSIONS
            Column {
                MonoCaption(stringResource(R.string.s_permissions))
                SettingsCard {
                    SettingsRow {
                        Text(stringResource(R.string.s_photos), style = RowText, color = Fl.ink, modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FlIcon(if (photosAccess.second) R.drawable.ic_check else R.drawable.ic_alert, null, Fl.muted, 16.dp)
                            Text(photosAccess.first, style = FlType.bodyS, color = Fl.muted)
                        }
                    }
                    SettingsRow(last = true, onClick = {
                        runCatching {
                            ctx.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
                        }
                    }) {
                        Text(stringResource(R.string.s_open_system_settings), style = RowText, color = Fl.ink, modifier = Modifier.weight(1f))
                        FlIcon(R.drawable.ic_external, null, Fl.muted, 18.dp)
                    }
                }
            }
            // STORAGE
            Column {
                MonoCaption(stringResource(R.string.s_storage))
                SettingsCard {
                    SettingsRow {
                        RowTexts(
                            stringResource(R.string.s_cache),
                            if (cacheBytes < 0) "…" else stringResource(R.string.lib_cache_value, formatSize(cacheBytes)),
                            monoSubtitle = true, titleStyle = RowText,
                        )
                        Box(
                            Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Fl.card).border(1.dp, Fl.line, RoundedCornerShape(18.dp))
                                .clickable(role = Role.Button) {
                                    ctx.imageLoader.memoryCache?.clear()
                                    ctx.imageLoader.diskCache?.clear()
                                    ctx.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                                    cacheTick++
                                    toaster.show(null, clearedText, R.drawable.ic_check)
                                }.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(stringResource(R.string.s_clear), style = FlType.chip, color = Fl.ink) }
                    }
                    SettingsRow(last = true, onClick = { if (drafts.isNotEmpty()) deleteDrafts = true }) {
                        Text(stringResource(R.string.s_delete_all_drafts), style = RowText.copy(fontWeight = FontWeight.Medium), color = if (drafts.isEmpty()) Fl.disabledText else Fl.danger)
                    }
                }
            }
            // ACCOUNT
            Column {
                MonoCaption(stringResource(R.string.s_account))
                SettingsCard {
                    if (profile.signedIn) {
                        SettingsRow {
                            Text(stringResource(R.string.s_signed_in_as), style = RowText, color = Fl.ink, modifier = Modifier.weight(1f))
                            Text(if (profile.handle.isNotBlank()) "@${profile.handle}" else profile.email, style = FlType.bodyS, color = Fl.muted)
                        }
                        SettingsRow(onClick = { signOut = true }) {
                            Text(stringResource(R.string.s_sign_out), style = RowText.copy(fontWeight = FontWeight.Medium), color = Fl.ink)
                        }
                        SettingsRow(last = true, onClick = { app.navigate(Routes.DELETE_ACCOUNT) }) {
                            Text(stringResource(R.string.s_delete_account), style = RowText.copy(fontWeight = FontWeight.Medium), color = Fl.danger)
                        }
                    } else {
                        SettingsRow {
                            Text(stringResource(R.string.s_signed_in_as), style = RowText, color = Fl.ink, modifier = Modifier.weight(1f))
                            Text(stringResource(R.string.lib_not_signed_in), style = FlType.bodyS, color = Fl.muted)
                        }
                        SettingsRow(last = true, onClick = { app.navigate(Routes.SIGN_IN) }) {
                            Text(stringResource(R.string.s_sign_in), style = RowText.copy(fontWeight = FontWeight.Medium), color = Fl.accentInk)
                        }
                    }
                }
            }
            // ABOUT
            Column {
                MonoCaption(stringResource(R.string.s_about_2))
                SettingsCard {
                    SettingsRow {
                        Text(stringResource(R.string.s_version), style = RowText, color = Fl.ink, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.s_x_y_z), style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 13.sp), color = Fl.muted)
                    }
                    LinkRow(stringResource(R.string.s_terms_of_service)) { open(LegalLinks.TERMS) }
                    LinkRow(stringResource(R.string.s_privacy_policy)) { open(LegalLinks.PRIVACY) }
                    LinkRow(stringResource(R.string.s_licences), last = true, chevron = true) { app.navigate(Routes.LICENCES) }
                }
            }
            Spacer(Modifier.height(16.dp).navigationBarsPadding())
        }
    }

    FlDialog(
        visible = signOut,
        onDismiss = { signOut = false },
        title = stringResource(R.string.s_sign_out_2),
        body = stringResource(R.string.s_your_drafts_stay_on_this_phone_sign_in_a),
        confirm = stringResource(R.string.s_sign_out),
        dismiss = stringResource(R.string.s_cancel),
        confirmKind = BtnKind.Dark,
        onConfirm = { signOut = false; c.prefs.signOut(); toaster.show(null, signedOut, R.drawable.ic_check) },
    )
    FlDialog(
        visible = deleteDrafts,
        onDismiss = { deleteDrafts = false },
        title = stringResource(R.string.lib_delete_all_drafts_title),
        body = stringResource(R.string.lib_delete_all_drafts_body),
        confirm = stringResource(R.string.s_delete_2),
        dismiss = stringResource(R.string.s_cancel),
        confirmKind = BtnKind.Danger,
        onConfirm = { deleteDrafts = false; c.drafts.deleteAll(); toaster.show(null, draftsDeleted, R.drawable.ic_check) },
    )
}

@Composable
private fun LinkRow(label: String, last: Boolean = false, chevron: Boolean = false, onClick: () -> Unit) {
    SettingsRow(last = last, onClick = onClick) {
        Text(label, style = RowText, color = Fl.ink, modifier = Modifier.weight(1f))
        FlIcon(if (chevron) R.drawable.ic_chevron_right else R.drawable.ic_external, null, Fl.muted, 18.dp)
    }
}

@Composable
private fun photoAccessLabel(): Pair<String, Boolean> {
    val ctx = LocalContext.current
    fun granted(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
    return when {
        Build.VERSION.SDK_INT >= 33 && granted(Manifest.permission.READ_MEDIA_IMAGES) -> stringResource(R.string.s_full_access) to true
        Build.VERSION.SDK_INT >= 34 && granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> stringResource(R.string.lib_photos_limited) to true
        Build.VERSION.SDK_INT < 33 && granted(Manifest.permission.READ_EXTERNAL_STORAGE) -> stringResource(R.string.s_full_access) to true
        else -> stringResource(R.string.lib_photos_off) to false
    }
}

private fun dirSize(f: java.io.File): Long = if (f.isFile) f.length() else f.listFiles()?.sumOf { dirSize(it) } ?: 0L

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024 -> "${bytes / (1024 * 1024)} MB"
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "0 MB"
}

// ───────────────────────────── N16 · Delete account ─────────────────────────────

@Composable
fun DeleteAccountScreen() {
    val app = LocalAppState.current
    val c = app.container
    val profile by c.prefs.profile.collectAsState()
    val toaster = LocalToaster.current
    var confirm by remember { mutableStateOf("") }
    val deleted = stringResource(R.string.lib_account_deleted)
    Column(Modifier.fillMaxSize().background(Fl.paper).statusBarsPadding().imePadding()) {
        Spacer(Modifier.height(4.dp))
        FlTopBar(stringResource(R.string.s_delete_account), { app.back() }, backLabel = stringResource(R.string.cd_back))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.s_delete_your_framelab_account),
                style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.6).sp),
                color = Fl.ink,
            )
            InfoCard(stringResource(R.string.s_gets_deleted)) {
                Bullet(R.drawable.ic_close, stringResource(R.string.s_drafts_and_favourites_synced_to_your_acc))
                Bullet(R.drawable.ic_close, stringResource(R.string.lib_your_handle_and_profile, if (profile.handle.isNotBlank()) "@${profile.handle}" else profile.email))
                Bullet(R.drawable.ic_close, stringResource(R.string.s_copies_of_saved_posts_in_my_designs))
            }
            InfoCard(stringResource(R.string.s_stays_on_your_phone)) {
                Bullet(R.drawable.ic_check, stringResource(R.string.s_every_image_you_ve_saved_to_your_gallery))
            }
            Row(
                Modifier.fillMaxWidth().background(Fl.sunk, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FlIcon(R.drawable.ic_pro, null, Fl.ink, 18.dp)
                val full = stringResource(R.string.s_have_pro_cancel_it_in_your_app_store_set)
                val lead = "Have Pro?"
                Text(
                    buildAnnotatedString {
                        if (full.startsWith(lead)) {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(lead) }; append(full.removePrefix(lead))
                        } else append(full)
                    },
                    style = FlType.secondary.copy(lineHeight = 19.sp), color = Fl.ink,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.s_type_delete_to_confirm), style = FieldLabel, color = Fl.muted)
            PlainField(
                confirm, { confirm = it.uppercase() }, false, error = false, mono = true,
                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                label = stringResource(R.string.s_type_delete_to_confirm),
            )
            Spacer(Modifier.height(4.dp))
            FlButton(
                stringResource(R.string.s_delete_account),
                {
                    c.prefs.deleteAccount()
                    c.saved.posts.value.map { it.id }.forEach(c.saved::delete)
                    toaster.show(null, deleted, R.drawable.ic_check)
                    app.goHome()
                },
                Modifier.fillMaxWidth(), kind = BtnKind.Danger, large = true, enabled = confirm.trim() == "DELETE",
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoCard(caption: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Fl.card, RoundedCornerShape(16.dp)).border(1.dp, Fl.line, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(caption, style = FlType.monoLabel, color = Fl.muted, modifier = Modifier.padding(bottom = 0.dp))
        content()
    }
}

@Composable
private fun Bullet(icon: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FlIcon(icon, null, if (icon == R.drawable.ic_close) Fl.danger else Fl.ink, 18.dp, Modifier.padding(top = 1.dp))
        Text(text, style = FlType.bodyS, color = Fl.ink)
    }
}

// ───────────────────────────── N16 · Licences ─────────────────────────────

private data class Licence(val name: String, val tag: String, val raw: Int? = null, val url: String? = null, val style: TextStyle? = null)

@Composable
fun LicencesScreen() {
    val app = LocalAppState.current
    val ctx = LocalContext.current
    var showing by remember { mutableStateOf<Licence?>(null) }
    var text by remember { mutableStateOf("") }
    val apache = "https://www.apache.org/licenses/LICENSE-2.0"
    val software = listOf(
        Licence(stringResource(R.string.lib_sw_compose), stringResource(R.string.lib_apache), url = apache),
        Licence(stringResource(R.string.lib_sw_coil), stringResource(R.string.lib_apache), url = apache),
        Licence(stringResource(R.string.lib_sw_kotlinx), stringResource(R.string.lib_apache), url = apache),
    )
    val ofl = stringResource(R.string.s_ofl_1_1)
    val fonts = listOf(
        Licence(stringResource(R.string.s_bricolage_grotesque), ofl, R.raw.ofl_bricolage, style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 16.sp)),
        Licence(stringResource(R.string.s_inter), ofl, R.raw.ofl_inter),
        Licence(stringResource(R.string.s_dm_mono), ofl, R.raw.ofl_dmmono, style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 15.sp)),
    )
    LaunchedEffect(showing) {
        text = showing?.raw?.let { r -> withContext(Dispatchers.IO) { ctx.resources.openRawResource(r).bufferedReader().use { it.readText() } } } ?: ""
    }
    fun open(l: Licence) { if (l.raw != null) showing = l else l.url?.let { openUrl(ctx, it) } }

    Column(Modifier.fillMaxSize().background(Fl.paper).statusBarsPadding()) {
        Spacer(Modifier.height(4.dp))
        FlTopBar(stringResource(R.string.s_licences), { app.back() }, backLabel = stringResource(R.string.cd_back))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(
                stringResource(R.string.s_framelab_is_built_on_open_source_softwar), style = FlType.bodyS.copy(lineHeight = 21.sp), color = Fl.muted,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
            )
            Spacer(Modifier.height(20.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                MonoCaption(stringResource(R.string.s_software))
                SettingsCard { software.forEachIndexed { i, l -> LicenceRow(l, i == software.lastIndex) { open(l) } } }
                Spacer(Modifier.height(22.dp))
                MonoCaption(stringResource(R.string.s_fonts))
                SettingsCard { fonts.forEachIndexed { i, l -> LicenceRow(l, i == fonts.lastIndex) { open(l) } } }
                Spacer(Modifier.height(20.dp))
                SettingsCard {
                    SettingsRow(last = true, minHeight = 52.dp, onClick = { openUrl(ctx, apache) }) {
                        Text(stringResource(R.string.s_all_other_licences), style = RowText, color = Fl.ink, modifier = Modifier.weight(1f))
                        FlIcon(R.drawable.ic_chevron_right, null, Fl.muted, 18.dp)
                    }
                }
                Spacer(Modifier.height(24.dp).navigationBarsPadding())
            }
        }
    }

    FlSheet(showing != null, { showing = null }, top = 52.dp, paneTitle = showing?.name) {
        SheetHeader(showing?.name ?: "", onClose = { showing = null }, closeLabel = stringResource(R.string.cd_close))
        Text(
            text, style = TextStyle(fontFamily = DmMono, fontSize = 11.sp, lineHeight = 17.sp), color = Fl.ink,
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun LicenceRow(l: Licence, last: Boolean, onClick: () -> Unit) {
    SettingsRow(last = last, minHeight = 52.dp, onClick = onClick) {
        Text(l.name, style = l.style ?: RowText, color = Fl.ink, modifier = Modifier.weight(1f))
        MonoTag(l.tag)
        FlIcon(R.drawable.ic_chevron_right, null, Fl.muted, 18.dp)
    }
}

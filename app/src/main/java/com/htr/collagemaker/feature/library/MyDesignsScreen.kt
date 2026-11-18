package com.htr.collagemaker.feature.library

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.htr.collagemaker.R
import com.htr.collagemaker.data.Draft
import com.htr.collagemaker.data.SavedPost
import com.htr.collagemaker.nav.LocalAppState
import com.htr.collagemaker.nav.Routes
import com.htr.collagemaker.ui.components.BtnKind
import com.htr.collagemaker.ui.components.EdgeToEdgeDialogWindow
import com.htr.collagemaker.ui.components.EmptyState
import com.htr.collagemaker.ui.components.FlButton
import com.htr.collagemaker.ui.components.FlDialog
import com.htr.collagemaker.ui.components.FlIcon
import com.htr.collagemaker.ui.components.FlProgress
import com.htr.collagemaker.ui.components.IconButtonFl
import com.htr.collagemaker.ui.components.LocalToaster
import com.htr.collagemaker.ui.components.MenuItem
import com.htr.collagemaker.ui.components.PopoverMenu
import com.htr.collagemaker.ui.components.Segmented
import com.htr.collagemaker.ui.components.Tab
import com.htr.collagemaker.ui.components.TopBarTextAction
import com.htr.collagemaker.ui.theme.Fl
import com.htr.collagemaker.ui.theme.FlType

/** N8–N10 · My Designs: Drafts | Saved | Favourites. */
@Composable
fun MyDesignsScreen(initialTab: Int) {
    val app = LocalAppState.current
    val c = app.container
    val drafts by c.drafts.drafts.collectAsState()
    val posts by c.saved.posts.collectAsState()
    val favIds by c.prefs.favourites.collectAsState()
    val favourites = remember(favIds) { favIds.mapNotNull { c.templates.template(it) }.sortedBy { it.name } }
    var tab by rememberSaveable { mutableStateOf(initialTab.coerceIn(0, 2)) }
    LaunchedEffect(initialTab) { tab = initialTab.coerceIn(0, 2) }

    // Select mode (N8-Select)
    var selecting by rememberSaveable { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    var confirmBulkDelete by remember { mutableStateOf(false) }
    // Single-draft dialogs
    var renameId by remember { mutableStateOf<String?>(null) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    val toaster = LocalToaster.current
    val dupText = stringResource(R.string.lib_toast_duplicated)

    BackHandler(selecting) { selecting = false; selected.clear() }
    LaunchedEffect(drafts) { selected.retainAll(drafts.map { it.id }.toSet()); if (drafts.isEmpty()) selecting = false }

    Box(Modifier.fillMaxSize().background(Fl.paper)) {
        if (selecting) {
            SelectDrafts(
                drafts = drafts,
                selected = selected,
                onCancel = { selecting = false; selected.clear() },
                onSelectAll = {
                    if (selected.size == drafts.size) selected.clear() else { selected.clear(); selected.addAll(drafts.map { it.id }) }
                },
                onToggle = { id -> if (id in selected) selected.remove(id) else selected.add(id) },
            )
        } else {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.s_my_designs), style = FlType.screenTitle, color = Fl.ink, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(16.dp))
                Segmented(
                    listOf(
                        if (drafts.isEmpty()) stringResource(R.string.s_drafts) else stringResource(R.string.lib_tab_drafts, drafts.size),
                        if (posts.isEmpty()) stringResource(R.string.s_saved) else stringResource(R.string.lib_tab_saved, posts.size),
                        if (favourites.isEmpty()) stringResource(R.string.s_favourites) else stringResource(R.string.lib_tab_favourites, favourites.size),
                    ),
                    tab, { tab = it },
                    Modifier.padding(horizontal = 20.dp).fillMaxWidth().semantics { contentDescription = "My Designs" },
                )
                when (tab) {
                    0 -> if (drafts.isEmpty()) LibraryEmpty(R.drawable.img_empty_drafts, R.string.s_no_drafts_yet, R.string.s_leave_a_design_with_save_draft_and_it_wa)
                    else DraftList(
                        drafts,
                        onOpen = { app.openEditor(draftId = it.id) },
                        onRename = { renameId = it.id },
                        onDuplicate = { c.drafts.duplicate(it.id); toaster.show(null, dupText, R.drawable.ic_duplicate) },
                        onSelect = { selecting = true; selected.clear(); selected.add(it.id) },
                        onDelete = { deleteId = it.id },
                    )
                    1 -> if (posts.isEmpty()) LibraryEmpty(R.drawable.img_empty_saved, R.string.s_nothing_saved_yet, R.string.s_when_you_save_a_design_its_images_go_to)
                    else SavedGrid(posts) { app.navigate(Routes.post(it.id)) }
                    else -> if (favourites.isEmpty()) LibraryEmpty(R.drawable.img_empty_favourites, R.string.s_no_favourites_yet, R.string.s_tap_the_heart_on_any_template_preview_to)
                    else LazyVerticalGrid(
                        GridCells.Fixed(2),
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(favourites, key = { it.id }) { t ->
                            GridCard(t.design, t.name, com.htr.collagemaker.ui.components.infoLine(t), { app.openPreview(t.id) }, favourite = true)
                        }
                    }
                }
            }
        }
    }

    if (selecting) {
        SelectActionBar(
            count = selected.size,
            onDuplicate = {
                selected.toList().forEach { c.drafts.duplicate(it) }
                toaster.show(null, dupText, R.drawable.ic_duplicate)
                selecting = false; selected.clear()
            },
            onDelete = { if (selected.isNotEmpty()) confirmBulkDelete = true },
        )
    }

    // Rename (N8-Rename)
    val renaming = renameId?.let { id -> drafts.firstOrNull { it.id == id } }
    RenameDialog(
        visible = renaming != null,
        draft = renaming,
        onDismiss = { renameId = null },
        onSave = { name -> renaming?.let { c.drafts.rename(it.id, name) }; renameId = null },
    )

    // Delete one (N8-DeleteDraft)
    val deleting = deleteId?.let { id -> drafts.firstOrNull { it.id == id } }
    var lastDeleting by remember { mutableStateOf<Draft?>(null) }
    if (deleting != null) lastDeleting = deleting
    FlDialog(
        visible = deleting != null,
        onDismiss = { deleteId = null },
        title = stringResource(R.string.lib_delete_draft_title, lastDeleting?.name ?: ""),
        body = stringResource(R.string.s_this_draft_goes_from_framelab_the_origin),
        confirm = stringResource(R.string.s_delete_2),
        confirmKind = BtnKind.Danger,
        dismiss = stringResource(R.string.s_cancel),
        onConfirm = { lastDeleting?.let { c.drafts.delete(listOf(it.id)) }; deleteId = null },
        header = {
            lastDeleting?.let { DesignThumb(it.design, 56.dp, 70.dp, 8.dp) }
            Spacer(Modifier.height(14.dp))
        },
    )

    // Delete many
    FlDialog(
        visible = confirmBulkDelete,
        onDismiss = { confirmBulkDelete = false },
        title = stringResource(R.string.lib_delete_drafts_title, selected.size),
        body = stringResource(R.string.lib_delete_drafts_body),
        confirm = stringResource(R.string.s_delete_2),
        confirmKind = BtnKind.Danger,
        dismiss = stringResource(R.string.s_cancel),
        onConfirm = {
            c.drafts.delete(selected.toList()); selected.clear(); selecting = false; confirmBulkDelete = false
        },
    )
}

@Composable
private fun LibraryEmpty(img: Int, title: Int, body: Int) {
    val app = LocalAppState.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        EmptyState(
            image = img, imageW = 240.dp, imageH = 178.dp,
            title = stringResource(title), body = stringResource(body),
            action = stringResource(R.string.s_browse_templates),
            onAction = { app.selectTab(Tab.Explore) },
            modifier = Modifier.padding(top = 68.dp),
        )
    }
}

// ───────────────────────────── Drafts list (N8) ─────────────────────────────

@Composable
private fun DraftList(
    drafts: List<Draft>,
    onOpen: (Draft) -> Unit,
    onRename: (Draft) -> Unit,
    onDuplicate: (Draft) -> Unit,
    onSelect: (Draft) -> Unit,
    onDelete: (Draft) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)) {
        itemsIndexed(drafts, key = { _, d -> d.id }) { i, d ->
            DraftRow(d, last = i == drafts.lastIndex, onOpen, onRename, onDuplicate, onSelect, onDelete)
        }
    }
}

@Composable
private fun DraftRow(
    d: Draft,
    last: Boolean,
    onOpen: (Draft) -> Unit,
    onRename: (Draft) -> Unit,
    onDuplicate: (Draft) -> Unit,
    onSelect: (Draft) -> Unit,
    onDelete: (Draft) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Column {
        Row(
            Modifier.fillMaxWidth().height(113.dp).clickable(role = Role.Button, onClickLabel = d.name) { onOpen(d) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesignThumb(d.design, 72.dp, 90.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(d.name, style = FlType.bodyL.copy(fontWeight = FontWeight.Bold, lineHeight = 21.sp), color = Fl.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(editedLabel(d.updatedAt), style = FlType.secondary, color = Fl.muted)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val p = if (d.total == 0) 0f else d.filled.toFloat() / d.total
                    FlProgress(p, Modifier.width(120.dp), track = Fl.track, fill = if (d.ready) Fl.ink else Fl.accent, height = 4.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (d.ready) stringResource(R.string.s_ready_to_save) else stringResource(R.string.lib_photos_progress, d.filled, d.total),
                        style = FlType.monoLabel.copy(letterSpacing = 0.sp, lineHeight = 14.sp),
                        color = if (d.ready) Fl.ink else Fl.muted,
                    )
                }
            }
            Box(Modifier.padding(start = 8.dp)) {
                IconButtonFl(
                    R.drawable.ic_more, stringResource(R.string.lib_more_options_for, d.name), { menu = true },
                    size = 44.dp, background = if (menu) Fl.track else androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier.padding(end = 0.dp),
                )
                PopoverMenu(
                    expanded = menu,
                    onDismiss = { menu = false },
                    offset = DpOffset((-150).dp, 51.dp),
                    items = listOf(
                        MenuItem(stringResource(R.string.s_rename), R.drawable.ic_edit) { onRename(d) },
                        MenuItem(stringResource(R.string.s_duplicate_2), R.drawable.ic_duplicate) { onDuplicate(d) },
                        MenuItem(stringResource(R.string.s_select), R.drawable.ic_select) { onSelect(d) },
                        MenuItem(stringResource(R.string.s_delete_2), R.drawable.ic_delete, destructive = true) { onDelete(d) },
                    ),
                )
            }
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Fl.line))
    }
}

// ───────────────────────────── Select mode (N8-Select) ─────────────────────────────

@Composable
private fun SelectDrafts(
    drafts: List<Draft>,
    selected: List<String>,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onToggle: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(96.dp)) { TopBarTextAction(stringResource(R.string.s_cancel), onCancel) }
            Text(
                stringResource(R.string.lib_n_selected, selected.size), style = FlType.navTitle, color = Fl.ink,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
            Box(Modifier.width(96.dp), contentAlignment = Alignment.CenterEnd) {
                Box(
                    Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onSelectAll).padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.s_select_all), style = FlType.bodyL.copy(fontWeight = FontWeight.SemiBold), color = Fl.ink) }
            }
        }
        LazyColumn(
            Modifier.fillMaxSize().semantics { contentDescription = "Drafts" },
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(drafts, key = { _, d -> d.id }) { _, d ->
                val sel = d.id in selected
                Row(
                    Modifier.fillMaxWidth().height(104.dp).clip(RoundedCornerShape(18.dp))
                        .background(if (sel) Fl.selectedRow else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable(role = Role.Checkbox, onClickLabel = stringResource(R.string.lib_select_item, d.name)) { onToggle(d.id) }
                        .semantics { this.selected = sel }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (sel) Box(Modifier.size(26.dp).background(Fl.accent, CircleShape), contentAlignment = Alignment.Center) {
                        FlIcon(R.drawable.ic_check, null, Fl.ink, 16.dp)
                    } else Box(Modifier.size(26.dp).border(2.dp, Fl.controlOff, CircleShape))
                    DesignThumb(d.design, 64.dp, 80.dp)
                    Column(Modifier.weight(1f)) {
                        Text(d.name, style = FlType.bodyL.copy(fontWeight = FontWeight.Bold, lineHeight = 21.sp), color = Fl.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(
                                R.string.lib_row_meta, editedLabel(d.updatedAt),
                                if (d.ready) stringResource(R.string.lib_ready_lower) else stringResource(R.string.lib_photos_progress, d.filled, d.total),
                            ),
                            style = FlType.secondary, color = Fl.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Bottom action bar that replaces the tab bar while selecting: drawn in a window anchored to the screen bottom. */
@Composable
private fun SelectActionBar(count: Int, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    val provider = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize) =
                IntOffset(0, windowSize.height - popupContentSize.height)
        }
    }
    Popup(popupPositionProvider = provider, properties = PopupProperties(focusable = false, dismissOnClickOutside = false, clippingEnabled = false)) {
        Column(
            Modifier.fillMaxWidth().background(Fl.card).semantics { contentDescription = "Actions for $count drafts" },
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Fl.line))
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlButton(stringResource(R.string.s_duplicate_2), onDuplicate, Modifier.weight(1f), kind = BtnKind.Secondary, leadingIcon = R.drawable.ic_duplicate, enabled = count > 0)
                FlButton(stringResource(R.string.lib_delete_n, count), onDelete, Modifier.weight(1f), kind = BtnKind.Danger, leadingIcon = R.drawable.ic_delete, enabled = count > 0)
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

// ───────────────────────────── Rename dialog (N8-Rename) ─────────────────────────────

@Composable
private fun RenameDialog(visible: Boolean, draft: Draft?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    var value by remember { mutableStateOf(TextFieldValue("")) }
    var last by remember { mutableStateOf<Draft?>(null) }
    LaunchedEffect(visible) {
        if (visible && draft != null) { last = draft; value = TextFieldValue(draft.name, TextRange(draft.name.length)) }
    }
    if (!state.currentState && !state.targetState && state.isIdle) return
    val focus = remember { FocusRequester() }
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        EdgeToEdgeDialogWindow()
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(state, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
                Box(Modifier.fillMaxSize().background(Fl.scrim).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
            }
            AnimatedVisibility(
                state,
                modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars).padding(top = 142.dp),
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.94f), exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.96f),
            ) {
                val shape = RoundedCornerShape(24.dp)
                Column(
                    Modifier.width(334.dp)
                        .shadow(25.dp, shape, ambientColor = Fl.ink.copy(alpha = .3f), spotColor = Fl.ink.copy(alpha = .3f))
                        .background(Fl.card, shape)
                        .semantics { paneTitle = "Rename draft" }
                        .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
                ) {
                    Text(stringResource(R.string.s_rename_draft), style = FlType.dialogTitle, color = Fl.ink)
                    Spacer(Modifier.height(14.dp))
                    FocusedField(value, { value = it }, focus, clearLabel = stringResource(R.string.cd_clear_name), label = stringResource(R.string.cd_draft_name))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.lib_rename_helper, last?.design?.name ?: ""),
                        style = FlType.caption.copy(lineHeight = 17.sp), color = Fl.muted,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FlButton(stringResource(R.string.s_cancel), onDismiss, Modifier.weight(1f), kind = BtnKind.Secondary)
                        FlButton(
                            stringResource(R.string.s_save), { onSave(value.text.trim()) }, Modifier.weight(1f),
                            enabled = value.text.isNotBlank(),
                        )
                    }
                }
            }
        }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    }
}

/** 52 dp text field with a 2 dp ink border and a round clear button (focused look from the designs). */
@Composable
fun FocusedField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focus: FocusRequester = remember { FocusRequester() },
    clearLabel: String,
    label: String,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
) {
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value, onValueChange,
        singleLine = true,
        textStyle = FlType.bodyL.copy(fontWeight = FontWeight.Medium, color = Fl.ink),
        cursorBrush = SolidColor(Fl.accent),
        modifier = modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = label },
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().height(height).background(Fl.card, shape).border(2.dp, Fl.ink, shape).padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.weight(1f)) { inner() }
                if (value.text.isNotEmpty()) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button) { onValueChange(TextFieldValue("")) }
                            .semantics { contentDescription = clearLabel },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(32.dp).background(Fl.sunk, CircleShape), contentAlignment = Alignment.Center) { FlIcon(R.drawable.ic_close, null, Fl.ink, 16.dp) }
                    }
                }
            }
        },
    )
}

// ───────────────────────────── Saved grid (N9) ─────────────────────────────

@Composable
private fun SavedGrid(posts: List<SavedPost>, onOpen: (SavedPost) -> Unit) {
    LazyVerticalGrid(
        GridCells.Fixed(2),
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(posts, key = { it.id }) { p ->
            val n = p.images.size.takeIf { it > 0 } ?: p.design.slides
            val date = if (isSameDay(p.savedAt, System.currentTimeMillis())) stringResource(R.string.lib_saved_today) else shortDate(p.savedAt)
            GridCard(p.design, p.name, stringResource(R.string.lib_saved_info, n, date), { onOpen(p) })
        }
    }
}

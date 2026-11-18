package com.htr.collagemaker.feature.editor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.htr.collagemaker.R
import com.htr.collagemaker.data.SavedPost
import com.htr.collagemaker.data.Settings
import com.htr.collagemaker.data.model.El
import com.htr.collagemaker.data.model.PhotoFit
import com.htr.collagemaker.data.model.SLIDE_W
import com.htr.collagemaker.data.model.SpotShape
import com.htr.collagemaker.feature.export.ExportException
import com.htr.collagemaker.feature.export.Exporter
import com.htr.collagemaker.nav.LocalAppState
import com.htr.collagemaker.nav.Routes
import com.htr.collagemaker.render.PhotoCache
import com.htr.collagemaker.render.rememberDesignImages
import com.htr.collagemaker.render.rememberDesignTextMeasurer
import com.htr.collagemaker.render.rememberStickerArt
import com.htr.collagemaker.ui.components.LocalToaster
import com.htr.collagemaker.ui.components.PageDots
import com.htr.collagemaker.ui.components.ToastData
import com.htr.collagemaker.ui.components.ToastHost
import com.htr.collagemaker.ui.theme.EditorTheme
import com.htr.collagemaker.ui.theme.Fl
import com.htr.collagemaker.ui.theme.Inter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/** Editor (HANDOFF §7.2, §8) — always dark, full-screen over the tabs. */
@Composable
fun EditorScreen(templateId: String?, draftId: String?) {
    EditorTheme { EditorContent(templateId, draftId) }
}

@Composable
private fun EditorContent(rawTemplateId: String?, draftId: String?) {
    val debug = remember(rawTemplateId) { DebugFlags.parse(rawTemplateId) }
    val templateId = debug.baseId
    val app = LocalAppState.current
    val container = app.container
    val ctx = LocalContext.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val settings = container.prefs.settings.collectAsState().value ?: Settings()

    var loadKey by remember { mutableStateOf(0) }
    val st = remember(loadKey) {
        val tpl = templateId?.let { container.templates.template(it) }
        val draft = draftId?.let { container.drafts.get(it) }
        EditorState(draft?.design ?: tpl?.design, draft?.id ?: draftId)
    }
    val templateName = templateId?.let { container.templates.template(it)?.name }

    // Draft may still be loading from disk on a cold start.
    LaunchedEffect(st) {
        if (draftId != null && st.design.templateId.isEmpty()) {
            val d = withTimeoutOrNull(2500) { container.drafts.drafts.first { l -> l.any { it.id == draftId } } }?.firstOrNull { it.id == draftId }
            if (d != null) { st.reset(d.design); st.open = OpenState.Opening }
        }
        // Backend template while the catalogue is still loading (cold start / process restore / retry).
        if (draftId == null && templateId != null && st.design.templateId.isEmpty()) {
            withTimeoutOrNull(20_000) { container.templates.state.first { s -> !s.loading || container.templates.template(templateId) != null } }
            container.templates.template(templateId)?.let { st.reset(it.design); st.open = OpenState.Opening }
        }
        if (st.design.templateId.isEmpty()) { st.open = OpenState.Failed; return@LaunchedEffect }
        // E1: preload the photos of the draft, at least briefly.
        val start = System.currentTimeMillis()
        withTimeoutOrNull(4000) { st.design.spots.mapNotNull { it.uri }.forEach { PhotoCache.load(ctx, it, 1400) } }
        val left = 450 - (System.currentTimeMillis() - start)
        if (left > 0) delay(left)
        st.open = OpenState.Open
        val loaded = withTimeoutOrNull(1500) { container.prefs.settings.first { it != null } }
        if (debug.flags.isEmpty() && loaded != null && !loaded.coachDone && st.design.filledCount == 0) st.coachStep = 0
        applyDebug(st, debug, ctx)
        if (debug.flags.isNotEmpty()) st.dirty = true
    }

    // Light system-bar icons while in the editor.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val c = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevS = c?.isAppearanceLightStatusBars ?: true
        val prevN = c?.isAppearanceLightNavigationBars ?: true
        c?.isAppearanceLightStatusBars = false
        c?.isAppearanceLightNavigationBars = false
        onDispose { c?.isAppearanceLightStatusBars = prevS; c?.isAppearanceLightNavigationBars = prevN }
    }

    val measurer = rememberDesignTextMeasurer()
    val images = rememberDesignImages(st.design, 1400)
    val art = rememberStickerArt()

    fun editorToast(lead: String?, text: String, icon: Int?, action: String?, onAction: (() -> Unit)?) =
        toaster.show(ToastData(lead, text, icon, action, onAction, editor = true))

    val sLocked = stringResource(R.string.ed_photo_locked)
    val sLockedBody = stringResource(R.string.ed_photo_locked_body)
    val sUnlock = stringResource(R.string.ed_unlock)
    val sNoSpot = stringResource(R.string.ed_no_empty_spot)
    val sNoSpotBody = stringResource(R.string.ed_no_empty_spot_body)
    val sAddSlide = stringResource(R.string.ed_add_slide)
    val sUndo = stringResource(R.string.ed_undo)
    val sDraftSaved = stringResource(R.string.ed_draft_saved)
    val sRemovedOne = stringResource(R.string.ed_photo_removed)
    val sRemovedFmt = stringResource(R.string.ed_photos_removed, 0)

    val lockedToast = {
        editorToast(sLocked, sLockedBody, R.drawable.ic_lock, sUnlock) { st.selectedSpot?.takeIf { it.locked }?.let { st.toggleLock() } }
    }

    // ───────────── Photo picker ─────────────
    var pickStart by remember { mutableStateOf<String?>(null) }
    var replaceId by remember { mutableStateOf<String?>(null) }
    val limit = remember { if (Build.VERSION.SDK_INT >= 33) runCatching { MediaStore.getPickImagesMaxLimit() }.getOrDefault(100) else 100 }
    val pickCount = st.design.spotsInFillOrder.let { order ->
        val start = pickStart
        if (start != null) {
            val i = order.indexOfFirst { it.id == start }.coerceAtLeast(0)
            (order.drop(i) + order.take(i)).count { it.uri == null || it.id == start }
        } else order.count { it.uri == null }.let { if (it == 0) order.count { s -> !s.locked } else it }
    }
    fun persist(uris: List<Uri>) = uris.forEach {
        runCatching { ctx.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    val multi = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(pickCount.coerceIn(2, max(2, limit)))) { uris ->
        if (uris.isNotEmpty()) { persist(uris); st.fillPhotos(uris.take(pickCount).map { it.toString() }, pickStart) }
        pickStart = null
    }
    val single = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            persist(listOf(uri))
            val r = replaceId
            if (r != null) st.replacePhoto(r, uri.toString()) else st.fillPhotos(listOf(uri.toString()), pickStart)
        }
        pickStart = null; replaceId = null
    }
    val imagesOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    fun pickFor(startId: String?) {
        pickStart = startId
        val n = if (startId == null) st.design.emptyCount else pickCount
        if (n <= 1 && startId != null || st.design.emptyCount == 1) single.launch(imagesOnly) else multi.launch(imagesOnly)
    }
    fun onPhotos() {
        st.panel = Panel.None
        if (st.design.emptyCount == 0) editorToast(sNoSpot, sNoSpotBody, R.drawable.ic_info, sAddSlide) { st.addSlide() }
        else pickFor(null)
    }

    // ───────────── Saving (§8.9) ─────────────
    val savedUris = remember { mutableStateListOf<Uri>() }
    var saveJob by remember { mutableStateOf<Job?>(null) }
    fun runSave(from: Int) {
        if (from == 0) savedUris.clear()
        st.sheet = EdSheet.Progress; st.saving = true; st.savedCount = from
        val design = st.design
        saveJob = scope.launch {
            try {
                val uris = Exporter.saveSlides(ctx, design, settings, from) { done, _ -> st.savedCount = done }
                savedUris.addAll(uris)
                val (w, h) = Exporter.pixelSize(design)
                val post = SavedPost(UUID.randomUUID().toString(), design.name, design, savedUris.map { it.toString() }, System.currentTimeMillis(), w, h)
                container.saved.add(post)
                st.saving = false; st.sheet = EdSheet.None
                app.navigate(Routes.success(post.id))
            } catch (e: CancellationException) {
                st.saving = false
                throw e
            } catch (e: ExportException) {
                savedUris.addAll(e.saved)
                st.failedSlide = e.failedSlide.coerceAtLeast(from + e.saved.size)
                st.errorMessage = e.message ?: "Unknown error"
                st.saving = false; st.sheet = EdSheet.Error
            } catch (e: Exception) {
                st.failedSlide = from + 0
                st.errorMessage = e.message ?: e.javaClass.simpleName
                st.saving = false; st.sheet = EdSheet.Error
            }
        }
    }
    val writePerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) runSave(0) else app.navigate(Routes.PHOTO_ACCESS)
    }
    fun startSave() {
        if (Build.VERSION.SDK_INT <= 28 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) { writePerm.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE); return }
        runSave(0)
    }
    fun onSave() {
        st.deselect(); st.panel = Panel.None; st.preview = false
        if (st.design.emptyCount > 0) st.sheet = EdSheet.Precheck else startSave()
    }

    // ───────────── Back / exit ─────────────
    fun exit() { app.back() }
    fun handleBack() {
        when {
            st.coachStep >= 0 -> { st.coachStep = -1; container.prefs.setCoachDone() }
            st.preview -> st.preview = false
            st.sheet == EdSheet.Progress -> { saveJob?.cancel(); st.sheet = EdSheet.None; st.saving = false }
            st.sheet == EdSheet.Text -> { st.commitLive(); st.editingTextId = null; st.sheet = EdSheet.None }
            st.sheet != EdSheet.None -> st.sheet = EdSheet.None
            st.crop -> st.crop = false
            st.panel != Panel.None -> st.panel = Panel.None
            st.selectedId != null -> st.deselect()
            st.dirty && st.open == OpenState.Open -> st.sheet = EdSheet.Draft
            else -> exit()
        }
    }
    BackHandler { handleBack() }

    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize().background(Fl.edBg)) {
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val topBarBottom = statusTop + 56.dp
        val dockTop = maxHeight - navBottom - 30.dp - 64.dp
        val bandTop = dockTop - 102.dp
        val slideH = st.design.slideH

        // Main canvas frame: slide 342 × 428 at x 24, 64 dp below the top bar, shrunk to fit short screens.
        val mainFrame = run {
            var w = maxWidth - 48.dp
            var h = w * (slideH / SLIDE_W)
            val maxBottom = bandTop - 52.dp
            var top = topBarBottom + 64.dp
            if (top + h > maxBottom) top = max((topBarBottom + 12.dp).value, (maxBottom - h).value).dp
            if (top + h > maxBottom) { h = maxBottom - top; w = h * (SLIDE_W / slideH) }
            val left = if (w >= maxWidth - 48.dp - 0.5.dp) 24.dp else (maxWidth - w) / 2
            SlideFrame(left, top, w, slideH)
        }
        val compactSheet = st.sheet in listOf(EdSheet.Text, EdSheet.Stickers, EdSheet.Slides)
        val compactFrame = run {
            val sheetH = when (st.sheet) { EdSheet.Stickers -> 404.dp; EdSheet.Slides -> 368.dp; else -> 374.dp }
            val target = if (st.sheet == EdSheet.Stickers) 250.dp else 274.dp
            val top = statusTop + 64.dp
            val avail = maxHeight - navBottom - sheetH - top - 15.dp
            val w = min(target.value, (avail * (SLIDE_W / slideH)).value).coerceAtLeast(120f).dp
            SlideFrame((maxWidth - w) / 2, top, w, slideH)
        }
        val frame = if (compactSheet) compactFrame else mainFrame
        val selected = st.selected
        val selSpot = st.selectedSpot
        val selectMode = selected != null
        val numbers = st.panel == Panel.Order || settings.alwaysNumbers

        if (st.open == OpenState.Opening) {
            EditorTopBar(false, false, false, false, ::handleBack, {}, {}, {}, {}, Modifier.padding(top = statusTop))
            OpeningState(templateName ?: st.design.name, st.design.slides.coerceAtLeast(1), mainFrame)
            return@BoxWithConstraints
        }
        if (st.open == OpenState.Failed) {
            EditorTopBar(false, false, false, false, ::exit, {}, {}, {}, {}, Modifier.padding(top = statusTop))
            CouldntOpen(templateName, onRetry = { container.templates.refresh(); loadKey++ }, onBack = ::exit, modifier = Modifier.padding(top = statusTop))
            return@BoxWithConstraints
        }

        // Canvas (with its own gestures).
        EditorCanvas(
            st = st, frame = frame, measurer = measurer, images = images, art = art,
            numbers = numbers && !compactSheet,
            interactive = !compactSheet && st.sheet == EdSheet.None && st.coachStep < 0,
            snapRotation = settings.snapRotation, haptics = settings.haptics,
            onTapEmpty = { s -> st.panel = Panel.None; pickFor(s.id) },
            onTapText = { t -> st.panel = Panel.None; st.editingTextId = t.id; st.beginLive(); st.sheet = EdSheet.Text },
            onLockedAttempt = lockedToast,
            onReplace = { replaceId = st.selectedId; single.launch(imagesOnly) },
            showPeek = !compactSheet,
            highlightTextId = if (st.sheet == EdSheet.Text) st.editingTextId else null,
        )

        // Crop hint pill (E10).
        if (st.crop) {
            Box(Modifier.offset(y = topBarBottom + 18.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.ed_adjusting_inside),
                    modifier = Modifier.height(32.dp).background(Fl.edRaised, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 7.dp),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp), color = Color.White,
                )
            }
        }

        // Top bar.
        EditorTopBar(
            full = true, canUndo = st.canUndo, canRedo = st.canRedo, saving = st.saving,
            onBack = ::handleBack, onUndo = st::undo, onRedo = st::redo,
            onPreview = { st.deselect(); st.panel = Panel.None; st.preview = true },
            onSave = ::onSave,
            modifier = Modifier.padding(top = statusTop),
        )

        if (!compactSheet) {
            // Page dots + side tools (stickers / slides).
            val dotsTop = frame.top + frame.height + 12.dp
            if (!selectMode) {
                Box(Modifier.offset(y = dotsTop - 10.dp).fillMaxWidth().height(48.dp)) {
                    PageDots(st.design.slides, st.slide, { st.slide = it }, Modifier.align(Alignment.Center), dark = true)
                    SideTool(R.drawable.ic_sticker, stringResource(R.string.ed_stickers), { st.panel = Panel.None; st.sheet = EdSheet.Stickers }, Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
                    SideTool(R.drawable.ic_slides, stringResource(R.string.ed_slides), { st.panel = Panel.None; st.sheet = EdSheet.Slides }, Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
                }
            }

            // Panels or hint line.
            when {
                selSpot != null && st.panel == Panel.Frame ->
                    FramePanel(selSpot.shape, { st.setShape(it) }, Modifier.offset(y = dockTop - 80.dp))
                selSpot != null && st.panel == Panel.Position -> {
                    val resized = kotlin.math.abs(selSpot.w - selSpot.baseW) > 0.5f || kotlin.math.abs(selSpot.h - selSpot.baseH) > 0.5f
                    val adjust = ChipSpec(stringResource(R.string.ed_adjust_inside), if (st.crop) R.drawable.ic_check else R.drawable.ic_adjust_inside, st.crop) { st.crop = !st.crop }
                    val zoomOut = ChipSpec(stringResource(R.string.ed_zoom_out), R.drawable.ic_zoom_out, enabled = selSpot.zoom > 1f) { st.zoomBy(1 / 1.25f) }
                    val zoomIn = ChipSpec(stringResource(R.string.ed_zoom_in), R.drawable.ic_zoom_in, enabled = selSpot.zoom < 6f) { st.zoomBy(1.25f) }
                    val center = ChipSpec(stringResource(R.string.ed_center_photo), R.drawable.ic_center) { st.centerPhoto() }
                    val chips = if (st.crop) listOf(adjust, zoomOut, zoomIn, center) else listOf(
                        adjust,
                        ChipSpec(stringResource(R.string.ed_reset_size), R.drawable.ic_reset_size, enabled = resized) { st.resetSize() },
                        zoomOut, zoomIn,
                        ChipSpec(stringResource(R.string.ed_turn_photo), R.drawable.ic_rotate) { st.turnPhoto() },
                        center,
                    )
                    ChipPanel(chips, Modifier.offset(y = dockTop - 62.dp))
                }
                selSpot != null && st.panel == Panel.Style -> ChipPanel(
                    listOf(
                        ChipSpec(stringResource(R.string.ed_fill), R.drawable.ic_fill, selSpot.fit == PhotoFit.Fill) { st.setFit(PhotoFit.Fill) },
                        ChipSpec(stringResource(R.string.ed_fit), R.drawable.ic_fit, selSpot.fit == PhotoFit.Fit) { st.setFit(PhotoFit.Fit) },
                        ChipSpec(stringResource(R.string.ed_flip), R.drawable.ic_flip, selSpot.flip) { st.toggleFlip() },
                    ),
                    Modifier.offset(y = dockTop - 62.dp), equal = true,
                )
                !selectMode && st.panel == Panel.Color ->
                    ColorPanel(st.design.templateBackground, st.design.background, { st.setBackground(it) }, Modifier.offset(y = dockTop - 90.dp))
                !selectMode && st.panel == Panel.Order ->
                    OrderPanel(st.design.spotsInFillOrder, st.design.slides, st.slide, images, { s ->
                        st.slide = s.homeSlideClamped(st.design.slides)
                        if (s.uri != null) st.select(s.id)
                    }, Modifier.offset(y = dockTop - 102.dp))
                else -> HintLine(st.design.filledCount, st.design.photoCount, selected, Modifier.offset(y = dockTop - 46.dp))
            }

            // Dock.
            val items = if (selectMode) {
                val locked = selSpot?.locked == true
                listOf(
                    DockItem(stringResource(R.string.ed_frame), R.drawable.ic_frame, st.panel == Panel.Frame, selSpot != null) { st.crop = false; st.panel = if (st.panel == Panel.Frame) Panel.None else Panel.Frame },
                    DockItem(stringResource(R.string.ed_position), R.drawable.ic_position, st.panel == Panel.Position, selSpot != null && !locked) { st.crop = false; st.panel = if (st.panel == Panel.Position) Panel.None else Panel.Position },
                    DockItem(stringResource(R.string.ed_style), R.drawable.ic_style, st.panel == Panel.Style, selSpot != null) { st.crop = false; st.panel = if (st.panel == Panel.Style) Panel.None else Panel.Style },
                    DockItem(stringResource(R.string.ed_duplicate), R.drawable.ic_duplicate) { st.duplicateSelected() },
                    DockItem(stringResource(R.string.ed_delete), R.drawable.ic_delete, enabled = !locked) { st.deleteSelected() },
                )
            } else {
                val unlockedFilled = st.design.spots.count { it.uri != null && !it.locked }
                listOf(
                    DockItem(stringResource(R.string.ed_photos), R.drawable.ic_add, accentPill = true) { onPhotos() },
                    DockItem(stringResource(R.string.ed_shuffle), R.drawable.ic_shuffle, enabled = unlockedFilled >= 2) { st.shuffle() },
                    DockItem(stringResource(R.string.ed_color), R.drawable.ic_color, st.panel == Panel.Color) { st.panel = if (st.panel == Panel.Color) Panel.None else Panel.Color },
                    DockItem(stringResource(R.string.ed_order), R.drawable.ic_order, st.panel == Panel.Order) { st.panel = if (st.panel == Panel.Order) Panel.None else Panel.Order },
                    DockItem(stringResource(R.string.ed_clear), R.drawable.ic_clear, enabled = unlockedFilled > 0) { st.sheet = EdSheet.Clear },
                )
            }
            EditorDock(items, if (selectMode) ({ st.deselect() }) else null, Modifier.offset(y = dockTop - 16.dp))

            // Editor toasts (white), above the hint line and panels, never over the dock.
            ToastHost(Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = navBottom + 30.dp + 64.dp + 68.dp), editor = true)
        }

        // Sheets.
        val textEl = st.editingTextId?.let { st.design.element(it) } as? El.Text
        TextSheet(st.sheet == EdSheet.Text, textEl, { f -> st.editingTextId?.let { id -> st.updateText(id, f) } }) {
            st.commitLive(); st.editingTextId = null; st.sheet = EdSheet.None
        }
        StickersSheet(st.sheet == EdSheet.Stickers, { st.sheet = EdSheet.None }) { a -> st.sheet = EdSheet.None; st.addSticker(a) }
        SlidesSheet(st.sheet == EdSheet.Slides, st, measurer, images, art) { st.sheet = EdSheet.None }
        DraftSheet(
            st.sheet == EdSheet.Draft, st.design.filledCount, st.design.photoCount,
            onSave = {
                val id = container.drafts.save(st.design, st.draftId)
                st.draftId = id; st.dirty = false; st.sheet = EdSheet.None
                toaster.show(ToastData(null, sDraftSaved, R.drawable.ic_check))
                exit()
            },
            onDiscard = { st.sheet = EdSheet.None; st.dirty = false; exit() },
            onCancel = { st.sheet = EdSheet.None },
        )
        PrecheckSheet(st.sheet == EdSheet.Precheck, st.design, measurer, images, art, { st.sheet = EdSheet.None }) { st.sheet = EdSheet.None; startSave() }
        ProgressSheet(st.sheet == EdSheet.Progress, st.design, st.savedCount, measurer, images, art, topBarBottom) {
            saveJob?.cancel(); st.saving = false; st.sheet = EdSheet.None
        }
        ErrorSheet(st.sheet == EdSheet.Error, st.failedSlide, st.errorMessage, { st.sheet = EdSheet.None }) { runSave(st.failedSlide) }
        ClearDialog(st.sheet == EdSheet.Clear, { st.sheet = EdSheet.None }) {
            st.sheet = EdSheet.None
            val n = st.clearPhotos()
            val msg = if (n == 1) sRemovedOne else sRemovedFmt.replaceFirst("0", n.toString())
            editorToast(null, msg, R.drawable.ic_info, sUndo) { st.undo() }
        }

        if (st.preview) {
            PreviewMode(st.design, st.slide, measurer, images, art, onClose = { st.slide = it; st.preview = false }) { st.preview = false; onSave() }
        }

        // N23 · First-visit coach marks.
        if (st.coachStep >= 0 && st.open == OpenState.Open) {
            val px = { d: Dp -> with(density) { d.toPx() } }
            val s = px(mainFrame.width) / SLIDE_W
            val target = when (st.coachStep) {
                0 -> st.design.spotsInFillOrder.firstOrNull()?.let { sp ->
                    val k = st.slide
                    val l = max(sp.x, k * SLIDE_W); val r = min(sp.x + sp.w, (k + 1) * SLIDE_W)
                    Offset(px(mainFrame.left) + ((l + r) / 2f - k * SLIDE_W) * s, px(mainFrame.top) + (sp.y + sp.h / 2f) * s)
                } ?: Offset(px(maxWidth) / 2, px(mainFrame.top + mainFrame.height / 2))
                1 -> Offset(px(maxWidth) / 2, px(mainFrame.top + mainFrame.height / 2))
                else -> Offset(px(maxWidth - 12.dp - 36.dp), px(statusTop + 28.dp))
            }
            val titles = listOf(R.string.ed_tip1_title, R.string.ed_tip2_title, R.string.ed_tip3_title)
            val bodies = listOf(R.string.ed_tip1_body, R.string.ed_tip2_body, R.string.ed_tip3_body)
            CoachMark(
                st.coachStep, 3, target, stringResource(titles[st.coachStep]), stringResource(bodies[st.coachStep]),
                onSkip = { st.coachStep = -1; container.prefs.setCoachDone() },
                onNext = { if (st.coachStep >= 2) { st.coachStep = -1; container.prefs.setCoachDone() } else st.coachStep++ },
            )
        }
    }
}

private fun El.Spot.homeSlideClamped(slides: Int) = (x / SLIDE_W).toInt().coerceIn(0, slides - 1)


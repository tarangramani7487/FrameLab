package com.framelab.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.framelab.app.R
import com.framelab.app.ui.theme.Fl
import com.framelab.app.ui.theme.FlMotion
import com.framelab.app.ui.theme.FlType
import kotlinx.coroutines.delay

// ───────────────────────────── Sheets (§5.12) ─────────────────────────────

/**
 * Bottom sheet over a scrim, drawn in its own full-screen window so it covers the tab bar.
 * Light: white, top radius 28, `fl_line_strong` handle. Dark: `fl_ed_surface`, radius 24, `fl_ed_handle`.
 * Back (and tapping the scrim) calls [onDismiss].
 */
@Composable
fun FlSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    scrim: Color = if (dark) Fl.scrimEditor else Fl.scrim,
    /** Distance from the top of the screen (e.g. 52 dp for the near-full-height template preview). */
    top: Dp? = null,
    handle: Boolean = true,
    paneTitle: String? = null,
    imeAware: Boolean = false,
    horizontalPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    if (!state.currentState && !state.targetState && state.isIdle) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
        EdgeToEdgeDialogWindow()
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(state, enter = fadeIn(tween(FlMotion.SHEET)), exit = fadeOut(tween(200))) {
                Box(
                    Modifier.fillMaxSize().background(scrim)
                        .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
                )
            }
            AnimatedVisibility(
                state,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(tween(FlMotion.SHEET)) { it },
                exit = slideOutVertically(tween(220)) { it },
            ) {
                val shape = RoundedCornerShape(topStart = if (dark) 24.dp else 28.dp, topEnd = if (dark) 24.dp else 28.dp)
                Column(
                    modifier
                        .fillMaxWidth()
                        .then(if (top != null) Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = (top - 48.dp).coerceAtLeast(0.dp)).fillMaxSize() else Modifier)
                        .clip(shape)
                        .background(if (dark) Fl.edSurface else Fl.card, shape)
                        .clickable(remember { MutableInteractionSource() }, null) {}
                        .semantics { if (paneTitle != null) this.paneTitle = paneTitle }
                        .windowInsetsPadding(if (imeAware) WindowInsets.navigationBars.union(WindowInsets.ime) else WindowInsets.navigationBars)
                        .padding(horizontal = horizontalPadding),
                ) {
                    if (handle) {
                        Box(Modifier.fillMaxWidth().height(25.dp), contentAlignment = Alignment.TopCenter) {
                            Box(Modifier.padding(top = 10.dp).size(36.dp, 5.dp).background(if (dark) Fl.edHandle else Fl.lineStrong, RoundedCornerShape(3.dp)))
                        }
                    }
                    content()
                }
            }
        }
    }
}

/** Makes the dialog window edge-to-edge with no dim (we draw our own scrim). */
@Composable
fun EdgeToEdgeDialogWindow() {
    val view = androidx.compose.ui.platform.LocalView.current
    LaunchedEffect(Unit) {
        (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.let { w ->
            w.setDimAmount(0f)
            w.setWindowAnimations(0)
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(w, false)
            w.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
            w.statusBarColor = android.graphics.Color.TRANSPARENT
            w.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }
}

/** Sheet header row: title + optional close button or text action. */
@Composable
fun SheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    onClose: (() -> Unit)? = null,
    closeLabel: String = "Close",
    action: String? = null,
    onAction: () -> Unit = {},
    style: androidx.compose.ui.text.TextStyle = if (dark) FlType.title else FlType.sheetTitle,
) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = style, color = if (dark) Fl.edText else Fl.ink, modifier = Modifier.weight(1f))
        if (action != null) {
            Box(
                Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onAction).padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) { Text(action, style = FlType.body.copy(fontWeight = FontWeight.Bold), color = if (dark) Fl.accent else Fl.accentInk) }
        }
        if (onClose != null) {
            IconButtonFl(
                R.drawable.ic_close, closeLabel, onClose, size = 44.dp, iconSize = 20.dp,
                tint = if (dark) Fl.edText else Fl.ink, background = if (dark) Fl.edRaised else Fl.sunk,
            )
        }
    }
}

// ───────────────────────────── Dialogs (§5.13) ─────────────────────────────

@Composable
fun FlDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    body: String?,
    confirm: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    dismiss: String = "Cancel",
    confirmKind: BtnKind = BtnKind.Primary,
    confirmEnabled: Boolean = true,
    /** Top offset instead of centred (Rename sits at y 190 above the keyboard). */
    top: Dp? = null,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    if (!state.currentState && !state.targetState && state.isIdle) return
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        EdgeToEdgeDialogWindow()
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(state, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
                Box(Modifier.fillMaxSize().background(if (dark) Fl.scrimEditor else Fl.scrim).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
            }
            AnimatedVisibility(
                state,
                modifier = if (top != null) Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars).padding(top = top - 48.dp) else Modifier.align(Alignment.Center),
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.94f), exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.96f),
            ) {
                val shape = RoundedCornerShape(24.dp)
                Column(
                    modifier.width(334.dp)
                        .shadow(25.dp, shape, ambientColor = Fl.ink.copy(alpha = .3f), spotColor = Fl.ink.copy(alpha = .3f))
                        .background(if (dark) Fl.edSurface else Fl.card, shape)
                        .clickable(remember { MutableInteractionSource() }, null) {}
                        .semantics { paneTitle = title }
                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    header?.invoke(this)
                    Text(title, style = FlType.dialogTitle, color = if (dark) Fl.edText else Fl.ink, textAlign = TextAlign.Center)
                    if (body != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(body, style = FlType.bodyS, color = if (dark) Fl.edMuted else Fl.muted, textAlign = TextAlign.Center)
                    }
                    extra?.invoke(this)
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FlButton(dismiss, onDismiss, Modifier.weight(1f), kind = if (dark) BtnKind.DarkSecondary else BtnKind.Secondary, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp))
                        FlButton(confirm, onConfirm, Modifier.weight(1f), kind = confirmKind, enabled = confirmEnabled, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp))
                    }
                }
            }
        }
    }
}

// ───────────────────────────── Popover menu (§5.14) ─────────────────────────────

data class MenuItem(val label: String, @DrawableRes val icon: Int, val destructive: Boolean = false, val onClick: () -> Unit)

/** Anchored popover: 184 wide, radius 16, items 44 dp. Place inside a Box with the ⋯ button. */
@Composable
fun PopoverMenu(expanded: Boolean, onDismiss: () -> Unit, items: List<MenuItem>, offset: DpOffset = DpOffset((-140).dp, 44.dp)) {
    if (!expanded) return
    val density = androidx.compose.ui.platform.LocalDensity.current
    Popup(
        offset = with(density) { IntOffset(offset.x.roundToPx(), offset.y.roundToPx()) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            Modifier.width(184.dp)
                .shadow(16.dp, shape, ambientColor = Fl.ink.copy(alpha = .22f), spotColor = Fl.ink.copy(alpha = .22f))
                .background(Fl.card, shape).border(1.dp, Fl.ink.copy(alpha = .05f), shape).padding(6.dp),
        ) {
            items.forEachIndexed { i, it ->
                if (it.destructive && i > 0) Box(Modifier.padding(vertical = 4.dp).fillMaxWidth().height(1.dp).background(Fl.sunk))
                Row(
                    Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { onDismiss(); it.onClick() }.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val c = if (it.destructive) Fl.danger else Fl.ink
                    FlIcon(it.icon, null, c, 18.dp)
                    Text(it.label, style = FlType.body.copy(fontWeight = FontWeight.Medium), color = c)
                }
            }
        }
    }
}

// ───────────────────────────── Toasts (§5.15) ─────────────────────────────

data class ToastData(
    val lead: String?,
    val text: String,
    @DrawableRes val icon: Int? = null,
    val action: String? = null,
    val onAction: (() -> Unit)? = null,
    /** Editor toasts are white with an `fl_accent_ink` action. */
    val editor: Boolean = false,
    val persistent: Boolean = false,
    val id: Long = System.nanoTime(),
)

@Stable
class Toaster {
    var current by mutableStateOf<ToastData?>(null)
        private set

    fun show(t: ToastData) { current = t }
    fun show(lead: String?, text: String, @DrawableRes icon: Int? = null, action: String? = null, editor: Boolean = false, onAction: (() -> Unit)? = null) =
        show(ToastData(lead, text, icon, action, onAction, editor))
    fun dismiss(id: Long? = null) { if (id == null || current?.id == id) current = null }
}

val LocalToaster = staticCompositionLocalOf { Toaster() }

/** Renders the current toast; place it where the screen wants toasts (above the tab bar / panels). */
@Composable
fun ToastHost(modifier: Modifier = Modifier, editor: Boolean = false, toaster: Toaster = LocalToaster.current) {
    val t = toaster.current
    LaunchedEffect(t?.id) {
        if (t != null && !t.persistent) { delay(FlMotion.TOAST_MS); toaster.dismiss(t.id) }
    }
    AnimatedVisibility(t != null && t.editor == editor, modifier, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
        val last = remember { mutableStateOf<ToastData?>(null) }
        if (t != null) last.value = t
        last.value?.let { Toast(it, onAction = { it.onAction?.invoke(); toaster.dismiss(it.id) }) }
    }
}

/** A single toast/banner. */
@Composable
fun Toast(data: ToastData, modifier: Modifier = Modifier, onAction: () -> Unit = {}) {
    val light = data.editor
    val bg = if (light) Fl.card else Fl.ink
    val fg = if (light) Fl.ink else Fl.card
    val actionColor = if (light) Fl.accentInk else Fl.accent
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp)
            .shadow(if (light) 15.dp else 14.dp, shape, ambientColor = (if (light) Color.Black.copy(.5f) else Fl.ink.copy(.3f)), spotColor = (if (light) Color.Black.copy(.5f) else Fl.ink.copy(.3f)))
            .background(bg, shape)
            .semantics { this.stateDescription = "alert" }
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        data.icon?.let { FlIcon(it, null, fg, 20.dp); Spacer(Modifier.width(12.dp)) }
        val txt = buildAnnotatedString {
            if (data.lead != null) { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(data.lead) }; append(" ") }
            append(data.text)
        }
        Text(txt, style = FlType.bodyS.copy(fontSize = 14.sp, lineHeight = 19.sp), color = fg, modifier = Modifier.weight(1f).padding(vertical = 14.dp))
        if (data.action != null) {
            Box(
                Modifier.height(44.dp).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onAction).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text(data.action, style = FlType.bodyS.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = actionColor) }
        } else Spacer(Modifier.width(10.dp))
    }
}

// ───────────────────────────── Page dots (§5.16) ─────────────────────────────

@Composable
fun PageDots(
    count: Int,
    current: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
) {
    Row(modifier.height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        repeat(count) { i ->
            val active = i == current
            val w by animateDpAsState(if (active) 18.dp else 6.dp, tween(200), label = "dot")
            Box(
                Modifier.size(width = if (active) 30.dp else 18.dp, height = 28.dp)
                    .clickable(remember { MutableInteractionSource() }, null, role = Role.Button) { onSelect(i) }
                    .semantics { contentDescription = "Slide ${i + 1} of $count" },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(w, 6.dp).background(
                        when { active && dark -> Fl.edText; active -> Fl.ink; dark -> Fl.edDots; else -> Fl.controlOff },
                        RoundedCornerShape(3.dp),
                    ),
                )
            }
        }
    }
}

// ───────────────────────────── Tab bar (§5.1) ─────────────────────────────

enum class Tab(val label: String, @DrawableRes val icon: Int) {
    Home("Home", R.drawable.ic_home),
    Explore("Explore", R.drawable.ic_explore),
    Create("Create", R.drawable.ic_add),
    Designs("My Designs", R.drawable.ic_designs),
    Profile("Profile", R.drawable.ic_profile),
}

@Composable
fun TabBar(active: Tab?, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(Fl.card)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Fl.line))
        Row(
            Modifier.fillMaxWidth().height(63.dp).padding(horizontal = 6.dp).semantics { contentDescription = "Main" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tab ->
                val isActive = tab == active
                Column(
                    Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.Tab, onClickLabel = tab.label) { onSelect(tab) }
                        .semantics { if (isActive) stateDescription = "Selected" },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                ) {
                    if (tab == Tab.Create) {
                        Box(Modifier.size(52.dp, 32.dp).background(Fl.accent, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                            FlIcon(R.drawable.ic_add, null, Fl.ink, 24.dp)
                        }
                    } else {
                        FlIcon(tab.icon, null, if (isActive) Fl.accent else Fl.muted, 24.dp)
                    }
                    Text(
                        tab.label,
                        style = FlType.label.copy(fontWeight = when { tab == Tab.Create -> FontWeight.SemiBold; isActive -> FontWeight.Bold; else -> FontWeight.Medium }),
                        color = when { tab == Tab.Create -> Fl.ink; isActive -> Fl.accentInk; else -> Fl.muted },
                    )
                }
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

/** Top inset spacer — keeps the status-bar area empty (mockups reserve 48 dp). */
@Composable
fun StatusBarSpacer() = Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars))

/** Content that sits above the navigation bar. */
fun Modifier.aboveNavBar(): Modifier = this.navigationBarsPadding()

/** Handles Android back while [enabled]. */
@Composable
fun BackPress(enabled: Boolean = true, onBack: () -> Unit) = BackHandler(enabled, onBack)

@Composable
fun ProvideToaster(toaster: Toaster, content: @Composable () -> Unit) = CompositionLocalProvider(LocalToaster provides toaster, content = content)

/** Simple overlay box helper for screens. */
@Composable
fun Overlay(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) = Box(modifier.fillMaxSize(), content = content)

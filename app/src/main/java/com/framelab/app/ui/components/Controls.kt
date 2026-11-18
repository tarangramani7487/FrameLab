package com.framelab.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framelab.app.R
import com.framelab.app.ui.theme.Fl
import com.framelab.app.ui.theme.FlMotion
import com.framelab.app.ui.theme.FlType
import com.framelab.app.ui.theme.Inter

// ───────────────────────────── Icons & press ─────────────────────────────

/** A FrameLab stroke icon, tinted at the use site. */
@Composable
fun FlIcon(@DrawableRes id: Int, contentDescription: String?, tint: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Icon(painterResource(id), contentDescription, modifier.size(size), tint = tint)
}

/** Card press feedback: scale 0.97 + alpha 0.82 (~120 ms). */
fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClickLabel: String? = null,
): Modifier = composedPressable(onClick, enabled, role, onClickLabel)

@Composable
private fun Modifier.composedPressableImpl(onClick: () -> Unit, enabled: Boolean, role: Role, label: String?): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) FlMotion.PRESS_SCALE else 1f, tween(FlMotion.PRESS_MS), label = "press")
    val a by animateFloatAsState(if (pressed) FlMotion.PRESS_ALPHA else 1f, tween(FlMotion.PRESS_MS), label = "pressA")
    return this.scale(s).alpha(a).clickable(src, null, enabled, label, role, onClick)
}

private fun Modifier.composedPressable(onClick: () -> Unit, enabled: Boolean, role: Role, label: String?): Modifier =
    composed { composedPressableImpl(onClick, enabled, role, label) }

/** Round icon-only button with a 48 dp touch target. */
@Composable
fun IconButtonFl(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Fl.ink,
    iconSize: Dp = 24.dp,
    size: Dp = 48.dp,
    background: Color = Color.Transparent,
    border: BorderStroke? = null,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background, CircleShape)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = null, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) { FlIcon(icon, null, tint, iconSize) }
}

// ───────────────────────────── Buttons (§5.6) ─────────────────────────────

enum class BtnKind { Primary, Dark, Secondary, Outlined, Danger, DarkSecondary, Disabled, OutlinedOnInk }

@Composable
fun FlButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: BtnKind = BtnKind.Primary,
    large: Boolean = false,
    enabled: Boolean = true,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
    height: Dp = if (large) 52.dp else 48.dp,
    shape: Shape = RoundedCornerShape(50),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    loading: Boolean = false,
) {
    val k = if (!enabled) BtnKind.Disabled else kind
    val (bg, fg) = when (k) {
        BtnKind.Primary -> Fl.accent to Fl.ink
        BtnKind.Dark -> Fl.ink to Fl.card
        BtnKind.Secondary -> Fl.sunk to Fl.ink
        BtnKind.Outlined -> Fl.card to Fl.ink
        BtnKind.Danger -> Fl.danger to Fl.card
        BtnKind.DarkSecondary -> Fl.edRaised to Fl.edText
        BtnKind.Disabled -> Fl.sunk to Fl.disabledText
        BtnKind.OutlinedOnInk -> Color.Transparent to Fl.card
    }
    val style = when {
        large -> FlType.buttonL
        k == BtnKind.Primary -> FlType.buttonAccent
        else -> FlType.button
    }
    Row(
        modifier
            .height(height)
            .clip(shape)
            .background(bg, shape)
            .then(
                when (k) {
                    BtnKind.Outlined -> Modifier.border(1.dp, Fl.line, shape)
                    BtnKind.OutlinedOnInk -> Modifier.border(1.dp, Color.White.copy(alpha = 0.28f), shape)
                    else -> Modifier
                },
            )
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) Spinner(16.dp, fg)
        leadingIcon?.let { FlIcon(it, null, fg, 20.dp) }
        Text(text, style = style, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailingIcon?.let { FlIcon(it, null, fg, 18.dp) }
    }
}

/** Text button (no fill). Underlined variant for "Back to editor", "Continue as guest". */
@Composable
fun FlTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Fl.ink,
    underline: Boolean = false,
    style: TextStyle = FlType.button,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, style = style.copy(textDecoration = if (underline) TextDecoration.Underline else null),
            color = if (enabled) color else Fl.disabledText,
        )
    }
}

// ───────────────────────────── Chips (§5.5) ─────────────────────────────

@Composable
fun FlChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    height: Dp = 40.dp,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
    badge: Int? = null,
    textStyle: TextStyle = FlType.chip,
    contentDescription: String? = null,
) {
    val shape = RoundedCornerShape(height / 2)
    val bg = if (selected) Fl.ink else Fl.card
    val fg = if (selected) Fl.card else Fl.ink
    Row(
        modifier
            .height(height)
            .clip(shape)
            .background(bg, shape)
            .then(if (!selected) Modifier.border(1.dp, Fl.line, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected; if (contentDescription != null) stateDescription = contentDescription }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leadingIcon?.let { FlIcon(it, null, fg, 18.dp) }
        Text(text, style = textStyle, color = fg, maxLines = 1)
        badge?.let {
            Box(Modifier.size(18.dp).background(if (selected) Fl.card else Fl.ink, CircleShape), contentAlignment = Alignment.Center) {
                Text("$it", style = FlType.label.copy(fontSize = 11.sp, lineHeight = 18.sp), color = if (selected) Fl.ink else Fl.card)
            }
        }
        trailingIcon?.let { FlIcon(it, null, fg, 16.dp) }
    }
}

/** Dark chip used in editor panels. */
@Composable
fun DarkChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    height: Dp = 40.dp,
) {
    val shape = RoundedCornerShape(height / 2)
    val bg = when { !enabled -> Fl.edSurface; selected -> Fl.edText; else -> Fl.edRaised }
    val fg = when { !enabled -> Fl.edDisabled; selected -> Fl.ink; else -> Fl.edText }
    Row(
        modifier
            .height(height)
            .clip(shape)
            .background(bg, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        icon?.let { FlIcon(it, null, fg, 18.dp) }
        Text(text, style = FlType.chip, color = fg, maxLines = 1)
    }
}

/** NEW tag (accent fill + ink) or soft variant (accentSoft + accentSoftText). */
@Composable
fun NewTag(text: String, modifier: Modifier = Modifier, soft: Boolean = false) {
    Box(
        modifier
            .height(if (soft) 22.dp else 24.dp)
            .background(if (soft) Fl.accentSoft else Fl.accent, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = FlType.tag.copy(fontSize = if (soft) 11.sp else 10.sp), color = if (soft) Fl.accentSoftText else Fl.ink)
    }
}

// ───────────────────────────── Segmented (§5.4) ─────────────────────────────

@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 42.dp,
    textStyle: TextStyle = FlType.chipHome,
    small: Boolean = false,
) {
    Row(
        modifier
            .height(height)
            .background(Fl.sunk, RoundedCornerShape(if (small) 12.dp else 13.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (sel) Modifier.shadow(1.5.dp, RoundedCornerShape(10.dp), ambientColor = Fl.ink.copy(alpha = .12f), spotColor = Fl.ink.copy(alpha = .12f)) else Modifier)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (sel) Fl.card else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) }
                    .semantics { this.selected = sel },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = textStyle, color = if (sel) Fl.ink else Fl.muted, maxLines = 1)
            }
        }
    }
}

/** Dark segmented control (editor text sheet). */
@Composable
fun DarkSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, height: Dp = 40.dp) {
    Row(modifier.height(height).background(Fl.edRaised, RoundedCornerShape(12.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(9.dp))
                    .background(if (sel) Fl.edText else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) }.semantics { this.selected = sel },
                contentAlignment = Alignment.Center,
            ) { Text(label, style = FlType.chip, color = if (sel) Fl.ink else Fl.edText) }
        }
    }
}

// ───────────────────────────── Switch (§5.7) ─────────────────────────────

@Composable
fun FlSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, label: String? = null) {
    val knobX by androidx.compose.animation.core.animateDpAsState(if (checked) 23.dp else 4.5.dp, tween(160), label = "knob")
    val track by animateColorAsState(if (checked) Fl.accent else Fl.sunk, tween(160), label = "track")
    Box(
        modifier
            .size(48.dp, 48.dp)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .semantics { role = Role.Switch; stateDescription = if (checked) "On" else "Off"; if (label != null) contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(48.dp, 28.dp).background(track, CircleShape)
                .then(if (!checked) Modifier.border(1.5.dp, Fl.controlOff, CircleShape) else Modifier),
        ) {
            val d = if (checked) 22.dp else 19.dp
            Box(
                Modifier.offset(x = knobX, y = (28.dp - d) / 2).size(d)
                    .then(if (checked) Modifier.shadow(2.dp, CircleShape) else Modifier)
                    .background(if (checked) Fl.card else Fl.controlOff, CircleShape),
            )
        }
    }
}

// ───────────────────────────── Search field (§5.3) ─────────────────────────────

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocusChange: (Boolean) -> Unit = {},
    clearLabel: String = "Clear search",
    onSearch: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = FlType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = Fl.ink),
        cursorBrush = SolidColor(Fl.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier
            .height(48.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused; onFocusChange(it.isFocused) },
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().height(48.dp).background(Fl.card, shape)
                    .border(if (focused) 2.dp else 1.dp, if (focused) Fl.ink else Fl.line, shape)
                    .padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FlIcon(R.drawable.ic_search, null, if (focused || value.isNotEmpty()) Fl.ink else Fl.muted, 20.dp)
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = FlType.body, color = Fl.muted, maxLines = 1)
                    inner()
                }
                if (value.isNotEmpty()) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button) { onValueChange("") }
                            .semantics { contentDescription = clearLabel },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(36.dp).background(Fl.sunk, CircleShape), contentAlignment = Alignment.Center) {
                            FlIcon(R.drawable.ic_close, null, Fl.ink, 16.dp)
                        }
                    }
                }
            }
        },
    )
}

/** Static search "button" (Home before focus). */
@Composable
fun SearchButton(placeholder: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.height(48.dp).clip(shape).background(Fl.card, shape).border(1.dp, Fl.line, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FlIcon(R.drawable.ic_search, null, Fl.muted, 20.dp)
        Text(placeholder, style = FlType.body, color = Fl.muted, maxLines = 1)
    }
}

// ───────────────────────────── Top bars (§5.2) ─────────────────────────────

/** Light pushed-screen top bar: 48 dp tall, back + centred title + optional right content. */
@Composable
fun FlTopBar(
    title: String?,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    backLabel: String = "Back",
    @DrawableRes backIcon: Int = R.drawable.ic_back,
    left: (@Composable RowScope.() -> Unit)? = null,
    right: @Composable RowScope.() -> Unit = {},
    titleColor: Color = Fl.ink,
    iconTint: Color = Fl.ink,
) {
    Box(modifier.fillMaxWidth().height(48.dp)) {
        Row(Modifier.align(Alignment.CenterStart).padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (left != null) left()
            else if (onBack != null) IconButtonFl(backIcon, backLabel, onBack, iconSize = 26.dp, tint = iconTint)
        }
        if (title != null) Text(
            title, style = FlType.navTitle, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center).widthIn(max = 220.dp),
        )
        Row(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically, content = right)
    }
}

/** 44 dp text action for top bars ("Cancel", "Save", "Select all"). */
@Composable
fun TopBarTextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, emphasis: Boolean = false, enabled: Boolean = true) {
    Box(
        modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = FlType.body.copy(fontWeight = if (emphasis) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium),
            color = when { !enabled -> Fl.disabledText; emphasis -> Fl.accentInk; else -> Fl.ink },
        )
    }
}

// ───────────────────────────── Misc ─────────────────────────────

/** Indeterminate ring spinner. */
@Composable
fun Spinner(size: Dp, color: Color, track: Color = color.copy(alpha = 0.25f), stroke: Dp = 2.dp) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "spin")
    val angle by t.animateFloat(0f, 360f, androidx.compose.animation.core.infiniteRepeatable(tween(900, easing = androidx.compose.animation.core.LinearEasing)), label = "a")
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val sw = stroke.toPx()
        drawCircle(track, radius = (this.size.minDimension - sw) / 2, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
        drawArc(color, angle, 90f, false, topLeft = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2),
            size = androidx.compose.ui.geometry.Size(this.size.width - sw, this.size.height - sw),
            style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

/** Progress bar (§5.17): 6 dp (4 in lists), radius 3. */
@Composable
fun FlProgress(progress: Float, modifier: Modifier = Modifier, track: Color = Fl.sunk, fill: Color = Fl.accent, height: Dp = 6.dp) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(250), label = "p")
    Box(modifier.height(height).clip(RoundedCornerShape(3.dp)).background(track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(p).background(fill, RoundedCornerShape(3.dp)))
    }
}

/** Number badge (§5.22). */
@Composable
fun NumberBadge(text: String, modifier: Modifier = Modifier, mono: Boolean = false, color: Color = Fl.ink, textColor: Color = Color.White) {
    Box(
        modifier.height(20.dp).widthIn(min = 20.dp).background(color, RoundedCornerShape(6.dp)).padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = if (mono) FlType.monoSmall.copy(fontSize = 11.sp, letterSpacing = 0.5.sp) else FlType.label.copy(fontFamily = Inter, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 11.sp, lineHeight = 20.sp),
            color = textColor,
        )
    }
}

/** Avatar with initials on accentSoft (§4.5). */
@Composable
fun InitialsAvatar(initials: String, size: Dp, modifier: Modifier = Modifier, imageUri: String? = null) {
    Box(modifier.size(size).clip(CircleShape).background(Fl.accentSoft), contentAlignment = Alignment.Center) {
        if (imageUri != null) {
            coil.compose.AsyncImage(imageUri, null, Modifier.size(size), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        } else {
            Text(initials, style = FlType.title.copy(fontSize = (size.value * 0.36f).sp, lineHeight = (size.value * 0.42f).sp), color = Fl.accentSoftText)
        }
    }
}

@Composable
fun HSpacer(w: Dp) = Spacer(Modifier.width(w))

@Composable
fun VSpacer(h: Dp) = Spacer(Modifier.height(h))


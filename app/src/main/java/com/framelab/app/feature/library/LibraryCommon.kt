package com.framelab.app.feature.library

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framelab.app.R
import com.framelab.app.data.model.Design
import com.framelab.app.ui.components.CardThumb
import com.framelab.app.ui.components.FlIcon
import com.framelab.app.ui.components.pressable
import com.framelab.app.ui.theme.DmMono
import com.framelab.app.ui.theme.Fl
import com.framelab.app.ui.theme.FlType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ───────────────────────────── Time & text helpers ─────────────────────────────

/** "Edited 2 h ago" / "Edited yesterday" / "Edited Nov 12". */
@Composable
fun editedLabel(time: Long): String {
    val now = System.currentTimeMillis()
    val diff = (now - time).coerceAtLeast(0)
    val min = diff / 60_000
    return when {
        min < 1 -> stringResource(R.string.lib_edited_now)
        min < 60 -> stringResource(R.string.lib_edited_min, min.toInt())
        isSameDay(time, now) -> stringResource(R.string.lib_edited_h, (min / 60).toInt())
        isSameDay(time, now - 86_400_000) -> stringResource(R.string.s_edited_yesterday)
        else -> stringResource(R.string.lib_edited_date, shortDate(time))
    }
}

fun isSameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

fun shortDate(time: Long): String = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(time))

// ───────────────────────────── Intents ─────────────────────────────

/** Hands images to another app in order (multi-image share). Instagram first when [instagram]. */
fun shareImages(context: Context, images: List<String>, instagram: Boolean): Boolean {
    if (images.isEmpty()) return false
    val uris = ArrayList(images.map(Uri::parse))
    val send = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/*"
        if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris[0]) else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        clipData = ClipData.newRawUri(null, uris[0]).also { c -> uris.drop(1).forEach { c.addItem(ClipData.Item(it)) } }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    if (instagram) {
        val ig = Intent(send).setPackage("com.instagram.android")
        if (ig.resolveActivity(context.packageManager) != null) {
            return runCatching { context.startActivity(ig) }.isSuccess
        }
    }
    return runCatching { context.startActivity(Intent.createChooser(send, null)) }.isSuccess
}

fun openUrl(context: Context, url: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
} catch (_: ActivityNotFoundException) { false }

fun shareText(context: Context, text: String) {
    runCatching {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
    }
}

// ───────────────────────────── Shared bits ─────────────────────────────

/** Draft / saved thumbnail with the slide drawn live (bare card variant). */
@Composable
fun DesignThumb(design: Design, width: Dp, height: Dp, radius: Dp = 10.dp, modifier: Modifier = Modifier) {
    CardThumb(design, width, modifier, radius = radius, height = height)
}

/** 2-column grid card: bare-art thumbnail 169 × 211 + title + info. */
@Composable
fun GridCard(
    design: Design,
    title: String,
    info: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    favourite: Boolean = false,
    width: Dp = 169.dp,
) {
    Column(modifier.width(width).pressable(onClick, onClickLabel = title)) {
        CardThumb(design, width, slidesBadge = design.slides, favourite = favourite)
        Spacer(Modifier.height(8.dp))
        Text(title, style = FlType.cardTitle.copy(fontSize = 15.sp, lineHeight = 19.sp), color = Fl.ink, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(info, style = FlType.caption, color = Fl.muted, maxLines = 1)
    }
}

/** Settings-style card (padding 0 14, radius 18, 1 dp line). Rows draw their own dividers. */
@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(Fl.card, shape).border(1.dp, Fl.line, shape),
        content = content,
    )
}

/** A row inside [SettingsCard]: 14 dp sides, bottom divider unless [last]. */
@Composable
fun SettingsRow(
    modifier: Modifier = Modifier,
    last: Boolean = false,
    minHeight: Dp = 56.dp,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = minHeight).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        if (!last) Box(Modifier.padding(horizontal = 14.dp).fillMaxWidth().height(1.dp).background(Fl.sunk))
    }
}

/** Title (+ optional subtitle) for a settings row. */
@Composable
fun RowScope.RowTexts(title: String, subtitle: String? = null, monoSubtitle: Boolean = false, titleColor: Color = Fl.ink, titleStyle: TextStyle = FlType.body.copy(lineHeight = 20.sp)) {
    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
        Text(title, style = titleStyle, color = titleColor)
        if (subtitle != null) Text(
            subtitle,
            style = if (monoSubtitle) FlType.monoLabel.copy(letterSpacing = 0.sp) else FlType.caption,
            color = Fl.muted,
        )
    }
}

/** Small segmented radio group used in settings rows (track 3 dp padding, 32 dp segments). */
@Composable
fun MiniSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, label: String, padH: Dp = 12.dp) {
    Row(
        Modifier.background(Fl.sunk, RoundedCornerShape(10.dp)).padding(3.dp).semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier.height(32.dp)
                    .then(if (sel) Modifier.shadow(1.dp, RoundedCornerShape(8.dp), ambientColor = Fl.ink.copy(.12f), spotColor = Fl.ink.copy(.12f)) else Modifier)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (sel) Fl.card else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(i) }
                    .semantics { this.selected = sel }
                    .padding(horizontal = padH),
                contentAlignment = Alignment.Center,
            ) { Text(o, style = FlType.chip, color = if (sel) Fl.ink else Fl.muted) }
        }
    }
}

/** Mono licence/meta tag ("OFL 1.1"). */
@Composable
fun MonoTag(text: String) {
    Box(Modifier.height(22.dp).background(Fl.sunk, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        Text(text, style = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 22.sp), color = Fl.ink)
    }
}

/** Mono caption above a group (x 4). */
@Composable
fun MonoCaption(text: String, modifier: Modifier = Modifier) {
    Text(text, style = FlType.monoLabel, color = Fl.muted, modifier = modifier.padding(start = 4.dp, bottom = 8.dp))
}

/** 64 dp outlined tile with icon over label (Saved post actions). */
@Composable
fun ActionTile(@DrawableRes icon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.height(64.dp).clip(shape).background(Fl.card, shape).border(1.dp, Fl.line, shape)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        FlIcon(icon, null, Fl.ink, 22.dp)
        Text(label, style = FlType.label.copy(fontSize = 12.sp, lineHeight = 15.sp), color = Fl.ink)
    }
}

/** Vertical filler that keeps a fixed visual block height. */
@Composable
fun ColumnScope.Fill() = Spacer(Modifier.weight(1f).fillMaxHeight())

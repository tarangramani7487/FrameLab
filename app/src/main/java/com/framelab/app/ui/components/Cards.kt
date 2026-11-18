package com.framelab.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framelab.app.R
import com.framelab.app.data.model.Design
import com.framelab.app.data.model.Template
import com.framelab.app.data.model.TemplateKind
import com.framelab.app.render.RenderOptions
import com.framelab.app.render.SlideView
import com.framelab.app.ui.theme.Bricolage
import com.framelab.app.ui.theme.Fl
import com.framelab.app.ui.theme.FlType

/** "6 slides · 12 photos" / "4:5 · 4 photos" / "Frame · 1 photo" info line. */
fun infoLine(t: Template): String = when {
    t.meta.kind == TemplateKind.Frame -> "Frame · ${t.photos} photo${if (t.photos == 1) "" else "s"}"
    t.slides == 1 -> "${t.ratio.label} · ${t.photos} photo${if (t.photos == 1) "" else "s"}"
    else -> "${t.slides} slides · ${t.photos} photos"
}

/**
 * Template card (§5.8). Thumbnail is the first slide drawn live; badges sit outside the scaled art.
 * Sizes: 128 × 160 (rows) or 169 × 211 (2-column grids, pass [width] = 169.dp).
 */
@Composable
fun TemplateCard(
    template: Template,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 128.dp,
    favourite: Boolean = false,
    locked: Boolean = template.meta.isPro,
    showNew: Boolean = template.meta.isNew && template.meta.framelabMade,
    info: String = infoLine(template),
    title: String = template.name,
    design: Design = template.design,
    highlight: String? = null,
) {
    Column(modifier.width(width).pressable(onClick, onClickLabel = title)) {
        CardThumb(design, width, slidesBadge = design.slides, favourite = favourite, locked = locked, showNew = showNew)
        Spacer(Modifier.height(8.dp))
        HighlightText(title, highlight, FlType.cardTitle.copy(fontSize = if (width > 140.dp) 15.sp else 14.sp), Fl.ink)
        Spacer(Modifier.height(2.dp))
        Text(info, style = FlType.caption, color = Fl.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Title with an optional `fl_accent_soft` highlight on the search match. */
@Composable
fun HighlightText(text: String, query: String?, style: androidx.compose.ui.text.TextStyle, color: Color, maxLines: Int = 1) {
    val q = query?.trim().orEmpty()
    val i = if (q.isEmpty()) -1 else text.indexOf(q, ignoreCase = true)
    if (i < 0) {
        Text(text, style = style, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    } else {
        val s = androidx.compose.ui.text.buildAnnotatedString {
            append(text.substring(0, i))
            pushStyle(androidx.compose.ui.text.SpanStyle(background = Fl.accentSoft))
            append(text.substring(i, i + q.length))
            pop()
            append(text.substring(i + q.length))
        }
        Text(s, style = style, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

/** Card thumbnail with badges. Bare variant: all badges off. */
@Composable
fun CardThumb(
    design: Design,
    width: Dp,
    modifier: Modifier = Modifier,
    slidesBadge: Int = 1,
    favourite: Boolean = false,
    locked: Boolean = false,
    showNew: Boolean = false,
    radius: Dp = 14.dp,
    height: Dp = width * 1.25f,
    options: RenderOptions = RenderOptions(),
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .size(width, height)
            .shadow(1.dp, shape, ambientColor = Fl.ink.copy(alpha = .1f), spotColor = Fl.ink.copy(alpha = .1f))
            .clip(shape)
            .background(Fl.sunk),
    ) {
        SlideView(design, 0, Modifier.fillMaxSize(), options = options, keepRatio = false)
        if (slidesBadge > 1) SlidesBadge(slidesBadge, Modifier.align(Alignment.TopEnd).padding(8.dp))
        if (locked) ProBadge(Modifier.align(Alignment.TopStart).padding(8.dp))
        else if (favourite) FavBadge(Modifier.align(Alignment.TopStart).padding(8.dp))
        if (showNew) NewTag("NEW", Modifier.align(Alignment.BottomStart).padding(8.dp))
    }
}

@Composable
fun SlidesBadge(n: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.height(22.dp).background(Fl.badgeScrim, RoundedCornerShape(11.dp)).padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        FlIcon(R.drawable.ic_slides, null, Color.White, 13.dp)
        Text("$n", style = FlType.label.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = Color.White)
    }
}

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Box(modifier.size(26.dp).background(Fl.ink, CircleShape), contentAlignment = Alignment.Center) {
        FlIcon(R.drawable.ic_pro, "Pro", Fl.accent, 15.dp)
    }
}

@Composable
fun FavBadge(modifier: Modifier = Modifier) {
    Box(modifier.size(28.dp).shadow(1.dp, CircleShape).background(Fl.card, CircleShape), contentAlignment = Alignment.Center) {
        FlIcon(R.drawable.ic_favourite_filled, "Favourite", Fl.accent, 16.dp)
    }
}

/** Home row header (§5.9). */
@Composable
fun RowHeader(
    title: String,
    count: Int?,
    onSeeAll: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    seeAllLabel: String = "See all",
    trailing: String? = null,
    highlight: String? = null,
) {
    Row(modifier.fillMaxWidth().heightIn(min = 24.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f, fill = false)) { HighlightText(title, highlight, FlType.section, Fl.ink) }
        if (isNew) { Spacer(Modifier.width(8.dp)); NewTag("NEW", soft = true) }
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = FlType.secondary.copy(fontWeight = FontWeight.Medium), color = Fl.muted)
        } else if (onSeeAll != null) {
            Row(
                Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onSeeAll).padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(seeAllLabel, style = FlType.secondary.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Fl.ink)
                if (count != null) {
                    Spacer(Modifier.width(4.dp))
                    Text("$count", style = FlType.secondary.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = Fl.muted)
                }
                Spacer(Modifier.width(4.dp))
                FlIcon(R.drawable.ic_chevron_right, null, Fl.ink, 16.dp)
            }
        }
    }
}

/** Shimmering skeleton block (§5.17). Respects "Remove animations". */
@Composable
fun Skeleton(modifier: Modifier = Modifier, radius: Dp = 8.dp) {
    val animate = animationsEnabled()
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "x")
    Box(
        modifier.clip(RoundedCornerShape(radius)).background(Fl.sunk).drawWithContent {
            drawContent()
            if (animate) {
                val w = size.width
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Fl.shimmer, Color.Transparent),
                        start = Offset(w * x - w * 0.5f, 0f), end = Offset(w * x + w * 0.5f, size.height),
                    ),
                )
            }
        },
    )
}

/** Whether system animations are on (Settings › Accessibility › Remove animations). */
@Composable
fun animationsEnabled(): Boolean {
    val ctx = LocalContext.current
    LocalView.current
    return android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
}

/** Skeleton for a 128 × 160 card + two caption bars. */
@Composable
fun CardSkeleton(width: Dp = 128.dp) {
    Column(Modifier.width(width)) {
        Skeleton(Modifier.size(width, width * 1.25f), 14.dp)
        Spacer(Modifier.height(10.dp))
        Skeleton(Modifier.size(width * 0.72f, 12.dp), 6.dp)
        Spacer(Modifier.height(6.dp))
        Skeleton(Modifier.size(width * 0.5f, 10.dp), 5.dp)
    }
}

/** Empty state (§5.11): illustration, title, body, primary button. */
@Composable
fun EmptyState(
    @DrawableRes image: Int,
    imageW: Dp,
    imageH: Dp,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
    actionKind: BtnKind = BtnKind.Primary,
    @DrawableRes actionIcon: Int? = null,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    Column(modifier.width(326.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(image), null, Modifier.size(imageW, imageH), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(20.dp))
        Text(title, style = FlType.title, color = Fl.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = FlType.bodyS, color = Fl.muted, textAlign = TextAlign.Center)
        extra()
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            FlButton(action, onAction, kind = actionKind, leadingIcon = actionIcon)
        }
    }
}

// ───────────────────────────── Grouped list (§5.10) ─────────────────────────────

@Composable
fun GroupCaption(text: String, modifier: Modifier = Modifier, color: Color = Fl.muted) {
    Text(text.uppercase(), style = FlType.monoLabel, color = color, modifier = modifier.padding(start = 4.dp, bottom = 8.dp))
}

@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(modifier.fillMaxWidth().clip(shape).background(Fl.card, shape).border(1.dp, Fl.line, shape), content = content)
}

@Composable
fun GroupDivider(inset: Dp = 0.dp) {
    Box(Modifier.padding(start = inset).fillMaxWidth().height(1.dp).background(Fl.sunk))
}

/** A settings-style row: optional 20 dp leading icon, title + subtitle, trailing content or chevron. */
@Composable
fun GroupRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    iconTint: Color = Fl.ink,
    titleColor: Color = Fl.ink,
    chevron: Boolean = true,
    @DrawableRes trailingIcon: Int? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    minHeight: Dp = 52.dp,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = minHeight)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { FlIcon(icon, null, iconTint, 20.dp); Spacer(Modifier.width(14.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = FlType.body.copy(fontWeight = FontWeight.Medium), color = titleColor)
            if (subtitle != null) Text(subtitle, style = FlType.secondary, color = Fl.muted)
        }
        if (value != null) { Text(value, style = FlType.secondary, color = Fl.muted); Spacer(Modifier.width(6.dp)) }
        when {
            trailing != null -> trailing()
            trailingIcon != null -> FlIcon(trailingIcon, null, Fl.muted, 18.dp)
            chevron && onClick != null -> FlIcon(R.drawable.ic_chevron_right, null, Fl.muted, 18.dp)
        }
    }
}

/** 44 dp accentSoft icon well. */
@Composable
fun IconWell(@DrawableRes icon: Int, modifier: Modifier = Modifier, size: Dp = 44.dp, bg: Color = Fl.accentSoft, tint: Color = Fl.accentInk, iconSize: Dp = 22.dp) {
    Box(modifier.size(size).background(bg, CircleShape), contentAlignment = Alignment.Center) { FlIcon(icon, null, tint, iconSize) }
}

/** Live wordmark "FrameLab." with an accent dot. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = FlType.wordmark, color: Color = Fl.ink) {
    val s = androidx.compose.ui.text.buildAnnotatedString {
        append("FrameLab")
        pushStyle(androidx.compose.ui.text.SpanStyle(color = Fl.accent)); append("."); pop()
    }
    Text(s, style = style.copy(fontFamily = Bricolage), color = color, modifier = modifier.semantics { contentDescription = "FrameLab" })
}

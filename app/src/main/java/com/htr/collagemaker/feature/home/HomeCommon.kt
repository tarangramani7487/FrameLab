package com.htr.collagemaker.feature.home

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htr.collagemaker.R
import com.htr.collagemaker.data.CatalogState
import com.htr.collagemaker.data.catalog.BuiltInCatalog
import com.htr.collagemaker.data.catalog.Category
import com.htr.collagemaker.data.model.Template
import com.htr.collagemaker.data.model.TemplateKind
import com.htr.collagemaker.nav.AppState
import com.htr.collagemaker.ui.components.FlIcon
import com.htr.collagemaker.ui.components.HighlightText
import com.htr.collagemaker.ui.components.TemplateCard
import com.htr.collagemaker.ui.theme.Fl
import com.htr.collagemaker.ui.theme.FlType

/** Small cross-screen UI state for the Home package (Create › Add a frame switches Home to Frames). */
object HomeUi {
    var showFrames by mutableStateOf(false)
}

/** Category id used for the built-in "FrameLab Originals" row (H3). */
const val ORIGINALS_ID = "framelab-originals"

fun CatalogState.allItems(): List<Template> = templates + frames

fun CatalogState.findTemplate(id: String): Template? = allItems().firstOrNull { it.id == id } ?: BuiltInCatalog.byId(id)

fun CatalogState.categoryById(id: String): Category? =
    (categories + frameCategories).firstOrNull { it.id == id }

fun CatalogState.categoryTitle(id: String): String? = categoryById(id)?.title

/** Templates of a category in category order (ids that aren't loaded are skipped). */
fun CatalogState.templatesOf(cat: Category): List<Template> {
    val map = allItems().associateBy { it.id }
    return cat.templateIds.mapNotNull { map[it] }
}

fun CatalogState.originals(): List<Template> =
    categoryById(ORIGINALS_ID)?.let { templatesOf(it) } ?: templates.filter { it.meta.framelabMade }

/** Search match on name, category title, style, topic. */
fun CatalogState.matches(t: Template, q: String): Boolean {
    if (q.isBlank()) return true
    val s = q.trim()
    return t.name.contains(s, true) || t.meta.style.contains(s, true) || t.meta.topic.contains(s, true) ||
        (categoryTitle(t.meta.category)?.contains(s, true) == true)
}

fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/** Opens a template preview; Pro templates are still previewable (Use template routes to the paywall). */
fun AppState.preview(t: Template) = openPreview(t.id)

/** "2 h ago" style relative time. */
@Composable
fun timeAgo(ts: Long): String {
    val m = ((System.currentTimeMillis() - ts) / 60000L).coerceAtLeast(0)
    return when {
        m < 1 -> stringResource(R.string.time_now)
        m < 60 -> stringResource(R.string.time_min, m.toInt())
        m < 60 * 24 -> stringResource(R.string.time_hours, (m / 60).toInt())
        else -> stringResource(R.string.time_days, (m / (60 * 24)).toInt())
    }
}

// ───────────────────────────── Local components ─────────────────────────────

/** Row header exactly as in Main.dc.html: 19/24 title, soft NEW tag (18 dp), "See all" 600 13 + count + chevron. */
@Composable
fun HomeRowHeader(
    title: String,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    count: Int? = null,
    onSeeAll: (() -> Unit)? = null,
    highlight: String? = null,
    trailing: String? = null,
    tag: String? = null,
) {
    Row(modifier.fillMaxWidth().height(24.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f, fill = false)) { HighlightText(title, highlight, FlType.section, Fl.ink) }
            if (isNew) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.height(18.dp).background(Fl.accentSoft, RoundedCornerShape(5.dp)).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.s_new), style = FlType.tag, color = Fl.accentSoftText)
                }
            }
            if (tag != null) {
                Spacer(Modifier.width(8.dp))
                Text(tag, style = FlType.monoLabel.copy(fontSize = 11.sp), color = Fl.muted, maxLines = 1)
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            trailing != null -> Text(trailing, style = FlType.caption.copy(fontWeight = FontWeight.Medium), color = Fl.muted)
            onSeeAll != null -> Row(
                Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onSeeAll).padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(stringResource(R.string.s_see_all), style = FlType.chip, color = Fl.ink)
                if (count != null) Text("$count", style = FlType.chip.copy(fontWeight = FontWeight.Medium), color = Fl.muted)
                FlIcon(R.drawable.ic_chevron_right, null, Fl.ink, 16.dp)
            }
        }
    }
}

/** Outlined suggestion / filter chip (32 dp suggestions, 36 dp removable). */
@Composable
fun OutlineChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 32.dp,
    @DrawableRes trailing: Int? = null,
    cd: String? = null,
) {
    val shape = RoundedCornerShape(height / 2)
    Row(
        modifier.height(height).clip(shape).background(Fl.card, shape).border(1.dp, Fl.line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .then(if (cd != null) Modifier.semantics { contentDescription = cd } else Modifier)
            .padding(start = if (height > 32.dp) 14.dp else 12.dp, end = if (trailing != null) 8.dp else if (height > 32.dp) 14.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text, style = FlType.chip, color = Fl.ink, maxLines = 1)
        trailing?.let { FlIcon(it, null, Fl.ink, 16.dp) }
    }
}

/** Dark pill button (44 dp, radius 22) — "Clear search", "Clear all filters", "Try again". */
@Composable
fun DarkPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null) {
    Row(
        modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Fl.ink).clickable(role = Role.Button, onClick = onClick)
            .padding(start = if (icon != null) 14.dp else 20.dp, end = if (icon != null) 18.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon?.let { FlIcon(it, null, Color.White, 20.dp) }
        Text(text, style = FlType.button, color = Color.White)
    }
}

/** Two-column card grid laid out in a plain Column (used inside lazy lists). Cards are 169 dp on a 390 dp phone. */
@Composable
fun TemplateGrid(
    items: List<Template>,
    favourites: Set<String>,
    isPro: Boolean,
    onOpen: (Template) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        val w = (maxWidth - 12.dp) / 2
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { t ->
                        HomeCard(t, favourites, isPro, onOpen, w)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeCard(t: Template, favourites: Set<String>, isPro: Boolean, onOpen: (Template) -> Unit, width: Dp = 128.dp, highlight: String? = null) {
    TemplateCard(
        t, { onOpen(t) }, width = width,
        favourite = t.id in favourites,
        locked = t.meta.isPro && !isPro,
        showNew = t.meta.isNew,
        info = infoText(t),
        highlight = highlight,
    )
}

@Composable
fun infoText(t: Template): String = when {
    t.meta.kind == TemplateKind.Frame ->
        if (t.photos == 1) stringResource(R.string.s_frame_1_photo) else stringResource(R.string.home_frame_photos, t.photos)
    else -> com.htr.collagemaker.ui.components.infoLine(t)
}

/** 76 dp sunk circle icon (N4 no results). */
@Composable
fun SunkCircleIcon(@DrawableRes icon: Int) {
    Box(Modifier.size(76.dp).background(Fl.sunk, CircleShape), contentAlignment = Alignment.Center) { FlIcon(icon, null, Fl.ink, 30.dp) }
}

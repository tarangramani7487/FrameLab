package com.htr.collagemaker.feature.home

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htr.collagemaker.R
import com.htr.collagemaker.data.CatalogState
import com.htr.collagemaker.data.catalog.BuiltInCatalog
import com.htr.collagemaker.data.model.Template
import com.htr.collagemaker.nav.LocalAppState
import com.htr.collagemaker.ui.components.BtnKind
import com.htr.collagemaker.ui.components.FlButton
import com.htr.collagemaker.ui.components.FlIcon
import com.htr.collagemaker.ui.components.FlSheet
import com.htr.collagemaker.ui.components.IconButtonFl
import com.htr.collagemaker.ui.components.SearchField
import com.htr.collagemaker.ui.components.Segmented
import com.htr.collagemaker.ui.components.SheetHeader
import com.htr.collagemaker.ui.components.Skeleton
import com.htr.collagemaker.ui.theme.Fl
import com.htr.collagemaker.ui.theme.FlType

// ───────────────────────────── Filters & sort model ─────────────────────────────

enum class FilterGroup(@StringRes val label: Int) {
    Slides(R.string.explore_slides), Photos(R.string.explore_photos), Ratio(R.string.explore_ratio),
    Style(R.string.explore_style), Topic(R.string.explore_topic);

    val options: List<String>
        get() = when (this) {
            Slides -> listOf("1", "3", "5–6")
            Photos -> listOf("≤5", "6–10", "10+")
            Ratio -> listOf("1:1", "4:5", "9:16")
            Style -> BuiltInCatalog.styles
            Topic -> BuiltInCatalog.topics
        }

    fun matches(t: Template, v: String): Boolean = when (this) {
        Slides -> when (v) { "1" -> t.slides == 1; "3" -> t.slides in 2..4; else -> t.slides >= 5 }
        Photos -> when (v) { "≤5" -> t.photos <= 5; "6–10" -> t.photos in 6..10; else -> t.photos >= 10 }
        Ratio -> t.ratio.label == v
        Style -> t.meta.style.equals(v, true)
        Topic -> t.meta.topic.equals(v, true)
    }

    /** Spoken value for "Remove filter …" labels. */
    fun spoken(v: String) = when (v) { "10+" -> "10 or more"; "≤5" -> "5 or fewer"; else -> v }
}

enum class SortMode(@StringRes val label: Int) { Trending(R.string.s_trending), New(R.string.s_new_2), MostUsed(R.string.s_most_used) }

fun List<Template>.sorted(mode: SortMode): List<Template> = when (mode) {
    SortMode.Trending -> sortedWith(compareByDescending<Template> { it.meta.isNew }.thenByDescending { it.meta.uses })
    SortMode.New -> sortedByDescending { it.meta.createdOrder }
    SortMode.MostUsed -> sortedByDescending { it.meta.uses }
}

fun List<Template>.filtered(filters: Map<FilterGroup, String>, q: String, state: CatalogState): List<Template> =
    filter { t -> filters.all { (g, v) -> g.matches(t, v) } && state.matches(t, q) }

private fun Map<FilterGroup, String>.toSaveable(): String = entries.joinToString("|") { "${it.key.name}=${it.value}" }
private fun String.toFilters(): Map<FilterGroup, String> = if (isBlank()) emptyMap() else split("|").mapNotNull {
    val (k, v) = it.split("=", limit = 2).takeIf { p -> p.size == 2 } ?: return@mapNotNull null
    runCatching { FilterGroup.valueOf(k) }.getOrNull()?.let { g -> g to v }
}.toMap()

// ───────────────────────────── Explore (N3 / N4) ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen() {
    val app = LocalAppState.current
    val c = app.container
    val state by c.templates.state.collectAsState()
    val favs by c.prefs.favourites.collectAsState()
    val profile by c.prefs.profile.collectAsState()
    val focus = LocalFocusManager.current

    var query by rememberSaveable { mutableStateOf("") }
    var filtersRaw by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SortMode.Trending) }
    var groupSheet by remember { mutableStateOf<FilterGroup?>(null) }
    var allSheet by remember { mutableStateOf(false) }
    var sortSheet by remember { mutableStateOf(false) }
    val filters = filtersRaw.toFilters()
    val setFilters: (Map<FilterGroup, String>) -> Unit = { filtersRaw = it.toSaveable() }
    val results = state.templates.filtered(filters, query, state).sorted(sort)
    val list = rememberLazyListState()
    LaunchedEffect(list.isScrollInProgress) { if (list.isScrollInProgress) focus.clearFocus() }
    BackHandler(query.isNotEmpty()) { query = ""; focus.clearFocus() }

    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize().background(Fl.paper),
        contentPadding = PaddingValues(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(), bottom = 24.dp),
    ) {
        item("title") {
            Text(stringResource(R.string.s_explore), style = FlType.screenTitle, color = Fl.ink, modifier = Modifier.padding(start = 20.dp, top = 6.dp))
        }
        item("search") {
            SearchField(
                query, { query = it }, stringResource(R.string.s_search_templates_styles_topics),
                Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp).fillMaxWidth(),
                clearLabel = stringResource(R.string.cd_clear_search), onSearch = { focus.clearFocus() },
            )
        }
        item("chips") {
            FilterChipRow(filters, onFilters = { allSheet = true }, onGroup = { groupSheet = it }, modifier = Modifier.padding(top = 12.dp))
        }
        item("meta") {
            MetaRow(
                if (state.loading) null else pluralStringResource(R.plurals.explore_count, results.size, results.size),
                sort, onSort = { sortSheet = true },
            )
        }
        when {
            state.loading -> item("skel") { SkeletonGrid() }
            results.isEmpty() -> item("empty") {
                NoMatches(filters, query, onRemove = { g -> setFilters(filters - g) }, onClear = { setFilters(emptyMap()); query = "" })
            }
            else -> item("grid") {
                TemplateGrid(results, favs, profile.isPro, { focus.clearFocus(); app.preview(it) }, Modifier.padding(top = 6.dp))
            }
        }
    }

    FilterGroupSheet(groupSheet, filters, setFilters) { groupSheet = null }
    AllFiltersSheet(allSheet, filters, setFilters, state.templates.filtered(filters, query, state).size) { allSheet = false }
    SortSheet(sortSheet, sort, { sort = it; sortSheet = false }) { sortSheet = false }
}

@Composable
private fun FilterChipRow(
    filters: Map<FilterGroup, String>,
    onFilters: () -> Unit,
    onGroup: (FilterGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().height(40.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Filters button + count badge.
        val shape = RoundedCornerShape(20.dp)
        Row(
            Modifier.height(40.dp).clip(shape).background(Fl.card).border(1.dp, Fl.line, shape)
                .clickable(role = Role.Button, onClick = onFilters).padding(start = 10.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FlIcon(R.drawable.ic_filters, null, Fl.ink, 18.dp)
            Text(stringResource(R.string.s_filters_2), style = FlType.chip, color = Fl.ink)
            if (filters.isNotEmpty()) {
                Box(Modifier.heightIn(18.dp).size(18.dp).background(Fl.ink, CircleShape), contentAlignment = Alignment.Center) {
                    Text("${filters.size}", style = FlType.label.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 18.sp), color = Fl.card)
                }
            }
        }
        // Active groups first (ink), then inactive outlined.
        val ordered = FilterGroup.entries.sortedBy { if (it in filters) 0 else 1 }
        ordered.forEach { g ->
            val v = filters[g]
            val active = v != null
            Row(
                Modifier.height(40.dp).clip(shape).background(if (active) Fl.ink else Fl.card)
                    .then(if (!active) Modifier.border(1.dp, Fl.line, shape) else Modifier)
                    .clickable(role = Role.Button) { onGroup(g) }
                    .semantics { selected = active }
                    .padding(start = 14.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    if (active) stringResource(R.string.explore_filter_value, stringResource(g.label), v!!) else stringResource(g.label),
                    style = FlType.chip, color = if (active) Fl.card else Fl.ink,
                )
                FlIcon(R.drawable.ic_chevron_down, null, if (active) Fl.card else Fl.ink, 16.dp)
            }
        }
    }
}

@Composable
private fun MetaRow(count: String?, sort: SortMode, onSort: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp).height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        if (count == null) Skeleton(Modifier.size(96.dp, 12.dp), 6.dp)
        else Text(count, style = FlType.secondary, color = Fl.muted)
        Spacer(Modifier.weight(1f))
        if (count == null) Skeleton(Modifier.padding(end = 8.dp).size(80.dp, 12.dp), 6.dp)
        else Row(
            Modifier.height(44.dp).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onSort)
                .semantics { contentDescription = "Sort" }.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FlIcon(R.drawable.ic_sort, null, Fl.ink, 18.dp)
            Text(stringResource(sort.label), style = FlType.chip, color = Fl.ink)
        }
    }
}

@Composable
fun SkeletonGrid() {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp)) {
        val w = (maxWidth - 12.dp) / 2
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(2) { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(2) { col ->
                        Column(Modifier.width(w)) {
                            Skeleton(Modifier.fillMaxWidth().height(w * 1.25f), 14.dp)
                            Skeleton(Modifier.padding(top = 10.dp).size(listOf(120, 104, 112, 98)[r * 2 + col].dp, 12.dp), 6.dp)
                            Skeleton(Modifier.padding(top = 6.dp).size(listOf(84, 92, 76, 80)[r * 2 + col].dp, 10.dp), 5.dp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoMatches(filters: Map<FilterGroup, String>, query: String, onRemove: (FilterGroup) -> Unit, onClear: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(top = 38.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        SunkCircleIcon(R.drawable.ic_filters)
        val n = filters.size
        Text(
            if (n == 0) stringResource(R.string.home_nothing_matches, query) else pluralStringResource(R.plurals.explore_no_match, n, n),
            style = FlType.title.copy(letterSpacing = (-0.4).sp), color = Fl.ink, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            stringResource(R.string.s_try_fewer_filters_remove_one_below_or_cl), style = FlType.bodyS, color = Fl.muted,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
        )
        FlowRow(
            Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterGroup.entries.filter { it in filters }.forEach { g ->
                val v = filters.getValue(g)
                val label = stringResource(g.label)
                OutlineChip(
                    stringResource(R.string.explore_filter_value, label, v), { onRemove(g) }, height = 36.dp,
                    trailing = R.drawable.ic_close, cd = stringResource(R.string.explore_remove_filter, label, g.spoken(v)),
                )
            }
        }
        DarkPill(stringResource(R.string.s_clear_all_filters), onClear, Modifier.padding(top = 20.dp))
    }
}

// ───────────────────────────── Sheets ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionChips(group: FilterGroup, value: String?, onPick: (String?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        group.options.forEach { o ->
            com.htr.collagemaker.ui.components.FlChip(o, { onPick(if (o == value) null else o) }, selected = o == value)
        }
    }
}

@Composable
private fun FilterGroupSheet(
    group: FilterGroup?,
    filters: Map<FilterGroup, String>,
    setFilters: (Map<FilterGroup, String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var last by remember { mutableStateOf(FilterGroup.Slides) }
    if (group != null) last = group
    val g = group ?: last
    FlSheet(group != null, onDismiss, paneTitle = stringResource(g.label)) {
        SheetHeader(stringResource(g.label), onClose = onDismiss, closeLabel = stringResource(R.string.cd_close))
        Spacer(Modifier.height(12.dp))
        OptionChips(g, filters[g]) { v -> setFilters(if (v == null) filters - g else filters + (g to v)) }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlButton(stringResource(R.string.explore_clear), { setFilters(filters - g) }, Modifier.weight(1f), kind = BtnKind.Secondary)
            FlButton(stringResource(R.string.explore_done), onDismiss, Modifier.weight(1f), kind = BtnKind.Dark)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun AllFiltersSheet(
    visible: Boolean,
    filters: Map<FilterGroup, String>,
    setFilters: (Map<FilterGroup, String>) -> Unit,
    count: Int,
    onDismiss: () -> Unit,
) {
    FlSheet(visible, onDismiss, paneTitle = stringResource(R.string.explore_all_filters)) {
        SheetHeader(stringResource(R.string.explore_all_filters), onClose = onDismiss, closeLabel = stringResource(R.string.cd_close))
        Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
            FilterGroup.entries.forEach { g ->
                Text(stringResource(g.label).uppercase(), style = FlType.monoLabel, color = Fl.muted, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                OptionChips(g, filters[g]) { v -> setFilters(if (v == null) filters - g else filters + (g to v)) }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlButton(stringResource(R.string.explore_clear), { setFilters(emptyMap()) }, Modifier.weight(1f), kind = BtnKind.Secondary)
            FlButton(stringResource(R.string.explore_show_results, count), onDismiss, Modifier.weight(1.6f), kind = BtnKind.Dark)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun SortSheet(visible: Boolean, current: SortMode, onPick: (SortMode) -> Unit, onDismiss: () -> Unit) {
    FlSheet(visible, onDismiss, paneTitle = stringResource(R.string.explore_sort_by)) {
        SheetHeader(stringResource(R.string.explore_sort_by), onClose = onDismiss, closeLabel = stringResource(R.string.cd_close))
        Spacer(Modifier.height(4.dp))
        SortMode.entries.forEachIndexed { i, m ->
            Row(
                Modifier.fillMaxWidth().height(56.dp).clickable(role = Role.RadioButton) { onPick(m) }.semantics { selected = m == current },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(m.label), style = FlType.body.copy(fontWeight = if (m == current) FontWeight.SemiBold else FontWeight.Normal), color = Fl.ink, modifier = Modifier.weight(1f))
                if (m == current) FlIcon(R.drawable.ic_check, null, Fl.accentInk, 22.dp)
            }
            if (i < SortMode.entries.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(Fl.sunk))
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ───────────────────────────── See all (N5) ─────────────────────────────

@Composable
fun SeeAllScreen(categoryId: String) {
    val app = LocalAppState.current
    val c = app.container
    val state by c.templates.state.collectAsState()
    val favs by c.prefs.favourites.collectAsState()
    val profile by c.prefs.profile.collectAsState()
    var sort by rememberSaveable { mutableStateOf(SortMode.Trending) }
    var filtersRaw by rememberSaveable { mutableStateOf("") }
    var allSheet by remember { mutableStateOf(false) }
    val filters = filtersRaw.toFilters()

    val cat = state.categoryById(categoryId)
    val title = cat?.title ?: if (categoryId == ORIGINALS_ID) stringResource(R.string.s_framelab_originals) else ""
    val base = when {
        cat != null -> state.templatesOf(cat)
        categoryId == ORIGINALS_ID -> state.originals()
        else -> emptyList()
    }
    val total = cat?.total ?: base.size
    val items = base.filtered(filters, "", state).sorted(sort)

    LazyColumn(
        Modifier.fillMaxSize().background(Fl.paper),
        contentPadding = PaddingValues(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(), bottom = 24.dp),
    ) {
        item("bar") {
            Row(Modifier.padding(start = 4.dp, end = 8.dp, top = 4.dp).fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButtonFl(R.drawable.ic_back, stringResource(R.string.cd_back), { app.back() }, size = 44.dp, iconSize = 26.dp)
                Column(Modifier.weight(1f)) {
                    Text(title, style = FlType.section, color = Fl.ink, maxLines = 1)
                    Text(
                        when {
                            cat?.subtitle.isNullOrBlank() -> stringResource(R.string.seeall_subtitle_plain, total)
                            cat!!.subtitle.first().isDigit() -> cat.subtitle
                            else -> stringResource(R.string.seeall_subtitle, total, cat.subtitle)
                        },
                        style = FlType.caption, color = Fl.muted, maxLines = 1,
                    )
                }
                Box {
                    IconButtonFl(R.drawable.ic_filters, stringResource(R.string.cd_filters), { allSheet = true }, size = 44.dp)
                    if (filters.isNotEmpty()) Box(
                        Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp).size(16.dp).background(Fl.ink, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("${filters.size}", style = FlType.label.copy(fontSize = 10.sp, lineHeight = 16.sp), color = Fl.card) }
                }
            }
        }
        item("sort") {
            Segmented(
                SortMode.entries.map { stringResource(it.label) }, sort.ordinal, { sort = SortMode.entries[it] },
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).fillMaxWidth().semantics { contentDescription = "Sort" },
                height = 40.dp, textStyle = FlType.chip, small = true,
            )
        }
        when {
            state.loading -> item("skel") { Box(Modifier.padding(top = 10.dp)) { SkeletonGrid() } }
            items.isEmpty() && filters.isNotEmpty() -> item("empty") {
                NoMatches(filters, "", onRemove = { g -> filtersRaw = (filters - g).toSaveable() }, onClear = { filtersRaw = "" })
            }
            else -> item("grid") {
                TemplateGrid(items, favs, profile.isPro, { app.preview(it) }, Modifier.padding(top = 16.dp))
            }
        }
    }
    AllFiltersSheet(allSheet, filters, { filtersRaw = it.toSaveable() }, base.filtered(filters, "", state).size) { allSheet = false }
}

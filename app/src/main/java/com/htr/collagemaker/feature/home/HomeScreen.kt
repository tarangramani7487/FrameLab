package com.htr.collagemaker.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htr.collagemaker.R
import com.htr.collagemaker.data.CatalogState
import com.htr.collagemaker.data.catalog.Category
import com.htr.collagemaker.data.model.Template
import com.htr.collagemaker.nav.LocalAppState
import com.htr.collagemaker.nav.Routes
import com.htr.collagemaker.ui.components.CardSkeleton
import com.htr.collagemaker.ui.components.FlIcon
import com.htr.collagemaker.ui.components.IconWell
import com.htr.collagemaker.ui.components.SearchField
import com.htr.collagemaker.ui.components.Segmented
import com.htr.collagemaker.ui.components.Skeleton
import com.htr.collagemaker.ui.components.Tab
import com.htr.collagemaker.ui.components.Wordmark
import com.htr.collagemaker.ui.theme.Bricolage
import com.htr.collagemaker.ui.theme.DmMono
import com.htr.collagemaker.ui.theme.Fl
import com.htr.collagemaker.ui.theme.FlType

/** Search result row: whole category (title matched) or only matching cards. */
private data class ResultRow(val category: Category, val items: List<Template>, val titleMatch: Boolean)

private fun CatalogState.searchRows(q: String, frames: Boolean): List<ResultRow> {
    val cats = (if (frames) frameCategories else categories).sortedBy { it.sortOrder }
    return cats.mapNotNull { cat ->
        val all = templatesOf(cat)
        if (cat.title.contains(q.trim(), true)) ResultRow(cat, all, true)
        else all.filter { matches(it, q) }.takeIf { it.isNotEmpty() }?.let { ResultRow(cat, it, false) }
    }
}

private fun CatalogState.matchCount(q: String, frames: Boolean): Int =
    (if (frames) this.frames else templates).count { matches(it, q) }

private fun CatalogState.suggestions(q: String, matched: List<Template>): List<String> {
    val s = q.trim()
    val topics = com.htr.collagemaker.data.catalog.BuiltInCatalog.topics
    val cats = categories.sortedBy { it.sortOrder }.map { it.title }
    val pool = if (matched.isNotEmpty()) {
        matched.map { it.meta.topic } + matched.map { it.meta.style } + topics + cats
    } else {
        cats + topics
    }
    return pool.filter { it.isNotBlank() && !it.contains(s, true) && !s.contains(it, true) }.distinct().take(4)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen() {
    val app = LocalAppState.current
    val c = app.container
    val state by c.templates.state.collectAsState()
    val favs by c.prefs.favourites.collectAsState()
    val profile by c.prefs.profile.collectAsState()
    val ctx = LocalContext.current
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    var query by rememberSaveable { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val frames = HomeUi.showFrames
    val searching = focused || query.isNotBlank()
    val q = query.trim()
    var online by remember { mutableStateOf(true) }
    LaunchedEffect(state) { online = isOnline(ctx) }
    val showError = !state.loading && state.error != null && online
    val showOffline = !state.loading && !online
    val fr = remember { FocusRequester() }
    val list = rememberLazyListState()

    LaunchedEffect(list.isScrollInProgress) {
        if (list.isScrollInProgress && focused) { focus.clearFocus(); keyboard?.hide() }
    }
    BackHandler(searching) { query = ""; focus.clearFocus() }

    val open: (Template) -> Unit = { focus.clearFocus(); app.preview(it) }
    val retry: () -> Unit = { online = isOnline(ctx); c.templates.refresh() }

    // Notification permission (first visit) / Play in-app review (later visits).
    com.htr.collagemaker.engage.HomeEngagement()

    Box(Modifier.fillMaxSize().background(Fl.paper)) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(), bottom = if (showOffline) 92.dp else 24.dp),
        ) {
            // Header y 48–98: wordmark + subtitle, count pill.
            item("header") {
                Row(Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Wordmark()
                        Text(stringResource(R.string.s_seamless_carousels_frames_collages), style = FlType.caption.copy(lineHeight = 17.sp), color = Fl.muted, maxLines = 1)
                    }
                    CountPill(
                        loading = state.loading,
                        count = if (searching && q.isNotEmpty()) state.matchCount(q, frames) else state.totalDesigns,
                    )
                }
            }
            item("search") {
                SearchField(
                    query, { query = it },
                    stringResource(if (frames) R.string.s_search_frames else R.string.s_search_templates),
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp).fillMaxWidth(),
                    focusRequester = fr,
                    onFocusChange = { focused = it },
                    clearLabel = stringResource(R.string.cd_clear_search),
                    onSearch = { focus.clearFocus() },
                )
            }
            item("tabs") {
                val labels = if (searching && q.isNotEmpty()) listOf(
                    stringResource(R.string.home_tab_templates_count, state.matchCount(q, false)),
                    stringResource(R.string.home_tab_frames_count, state.matchCount(q, true)),
                ) else listOf(stringResource(R.string.s_templates), stringResource(R.string.s_frames))
                Segmented(
                    labels, if (frames) 1 else 0, { HomeUi.showFrames = it == 1 },
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp).fillMaxWidth()
                        .semantics { contentDescription = "Library" },
                )
            }

            when {
                searching && q.isNotEmpty() -> {
                    val rows = state.searchRows(q, frames)
                    val matched = rows.flatMap { it.items }
                    val sugg = state.suggestions(q, matched)
                    if (rows.isEmpty()) {
                        item("noresults") {
                            NoResults(q, sugg, onPick = { query = it }, onClear = { query = ""; focus.clearFocus() })
                        }
                    } else {
                        if (sugg.isNotEmpty()) item("also") { AlsoTry(sugg) { query = it } }
                        rows.forEachIndexed { i, r ->
                            item("r-${r.category.id}") {
                                Column(Modifier.padding(top = if (i == 0) 20.dp else 24.dp)) {
                                    HomeRowHeader(
                                        r.category.title,
                                        isNew = r.category.isNew && r.titleMatch,
                                        count = if (r.titleMatch) r.category.total else null,
                                        onSeeAll = if (r.titleMatch) ({ app.navigate(Routes.seeAll(r.category.id, Tab.Home)) }) else null,
                                        highlight = if (r.titleMatch) q else null,
                                        trailing = if (r.titleMatch) null else pluralStringResource(R.plurals.home_row_matches, r.items.size, r.items.size),
                                    )
                                    CardRow(r.items, favs, profile.isPro, open, if (r.titleMatch) null else q)
                                }
                            }
                        }
                    }
                }
                searching -> {
                    // Focused with an empty query: show the regular content minus the hero.
                    rows(state, frames, favs, profile.isPro, open, app::navigate, top = 22.dp)
                }
                state.loading -> {
                    if (!frames) item("hero") { Hero(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) { openFilmRoll(state, app::openPreview) } }
                    item("skeleton") { LoadingRows(if (frames) 22.dp else 12.dp) }
                }
                showError -> {
                    val originals = state.originals()
                    item("originals") {
                        Column(Modifier.padding(top = 22.dp)) {
                            HomeRowHeader(
                                stringResource(R.string.s_framelab_originals),
                                tag = stringResource(R.string.s_built_in),
                                count = state.categoryById(ORIGINALS_ID)?.total ?: originals.size,
                                onSeeAll = { app.navigate(Routes.seeAll(ORIGINALS_ID, Tab.Home)) },
                            )
                            CardRow(originals, favs, profile.isPro, open)
                        }
                    }
                    item("error") { ErrorCard(state.error ?: "", retry, Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) }
                }
                else -> {
                    if (!frames) item("hero") { Hero(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) { openFilmRoll(state, app::openPreview) } }
                    rows(state, frames, favs, profile.isPro, open, app::navigate, top = if (frames) 22.dp else 24.dp)
                }
            }
        }

        if (showOffline) OfflineBanner(retry, Modifier.align(Alignment.BottomCenter).padding(12.dp))
    }
}

/** N24 · persistent ink banner above the tab bar: 56 dp, radius 16, 13/18 text, accent Retry. */
@Composable
private fun OfflineBanner(onRetry: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().height(56.dp)
            .shadow(14.dp, shape, ambientColor = Fl.ink.copy(alpha = .3f), spotColor = Fl.ink.copy(alpha = .3f))
            .background(Fl.ink, shape).padding(start = 16.dp, end = 6.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlIcon(R.drawable.ic_offline, null, Color.White, 20.dp)
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(R.string.home_offline_lead)) }
                append(" "); append(stringResource(R.string.home_offline_text))
            },
            style = FlType.secondary, color = Color.White, modifier = Modifier.weight(1f), maxLines = 2,
        )
        Box(
            Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onRetry).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) { Text(stringResource(R.string.s_retry), style = FlType.chipHome.copy(fontWeight = FontWeight.Bold), color = Fl.accent) }
    }
}

private fun openFilmRoll(state: CatalogState, open: (String) -> Unit) {
    val t = state.templates.firstOrNull { it.id == "fl:film-roll" }
        ?: state.findTemplate("film-roll")
        ?: state.allItems().firstOrNull { it.name.equals("Film Roll", true) }
        ?: state.originals().firstOrNull()
        ?: state.templates.firstOrNull()
    t?.let { open(it.id) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.rows(
    state: CatalogState,
    frames: Boolean,
    favs: Set<String>,
    isPro: Boolean,
    open: (Template) -> Unit,
    navigate: (String) -> Unit,
    top: androidx.compose.ui.unit.Dp,
) {
    val cats = (if (frames) state.frameCategories else state.categories).sortedBy { it.sortOrder }
    cats.forEachIndexed { i, cat ->
        item("row-${cat.id}") {
            val items = state.templatesOf(cat)
            Column(Modifier.padding(top = if (i == 0) top else 24.dp)) {
                HomeRowHeader(cat.title, isNew = cat.isNew, count = cat.total, onSeeAll = { navigate(Routes.seeAll(cat.id, Tab.Home)) })
                CardRow(items, favs, isPro, open)
            }
        }
    }
}

@Composable
private fun CardRow(items: List<Template>, favs: Set<String>, isPro: Boolean, open: (Template) -> Unit, highlight: String? = null) {
    LazyRow(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.id }) { HomeCard(it, favs, isPro, open, highlight = highlight) }
    }
}

@Composable
private fun CountPill(loading: Boolean, count: Int) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.size(104.dp, 40.dp).background(Fl.card, shape).border(1.dp, Fl.line, shape),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) Skeleton(Modifier.size(64.dp, 10.dp), 5.dp)
        else {
            Text("$count", style = FlType.section.copy(fontSize = 17.sp, lineHeight = 20.sp, letterSpacing = 0.sp), color = Fl.ink)
            Text(stringResource(R.string.s_designs), style = FlType.caption, color = Fl.muted)
        }
    }
}

/** Hero card (H1): ink, radius 22, 350 × 216. */
@Composable
private fun Hero(modifier: Modifier, onTry: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(216.dp).clip(RoundedCornerShape(22.dp)).background(Fl.ink)
            .semantics { contentDescription = "FrameLab Originals" },
    ) {
        Image(
            painterResource(R.drawable.img_home_hero_film_roll), null,
            Modifier.align(Alignment.TopEnd).padding(top = 44.dp).size(164.dp, 132.dp),
            contentScale = ContentScale.Fit,
        )
        Column(Modifier.padding(start = 20.dp, top = 20.dp).width(176.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.s_new_framelab_originals), style = FlType.monoSmall.copy(letterSpacing = 1.2.sp), color = Fl.accent, maxLines = 1, softWrap = false)
            Text(
                stringResource(R.string.s_one_photo_three_slides),
                style = FlType.title.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 26.sp, letterSpacing = (-0.5).sp),
                color = Color.White,
            )
            Text(stringResource(R.string.s_photos_that_flow_across_every_swipe), style = FlType.secondary, color = Color.White.copy(alpha = 0.74f))
        }
        Row(
            Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 20.dp).height(44.dp)
                .clip(RoundedCornerShape(22.dp)).background(Fl.accent).clickable(role = Role.Button, onClick = onTry)
                .padding(start = 18.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.s_try_film_roll), style = FlType.chipHome.copy(fontWeight = FontWeight.Bold), color = Fl.ink)
            FlIcon(R.drawable.ic_arrow_right, null, Fl.ink, 18.dp)
        }
    }
}

/** H2 skeleton rows: row title 150 × 16, see-all 64 × 12, three 128 × 160 cards, next row title. */
@Composable
private fun LoadingRows(top: androidx.compose.ui.unit.Dp) {
    Column(Modifier.padding(top = top).fillMaxWidth().semantics { contentDescription = "Loading templates" }) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Skeleton(Modifier.size(150.dp, 16.dp), 6.dp)
            Spacer(Modifier.weight(1f))
            Skeleton(Modifier.size(64.dp, 12.dp), 6.dp)
        }
        Row(Modifier.padding(start = 20.dp, top = 16.dp).horizontalScroll(rememberScrollState(), enabled = false), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) { CardSkeleton() }
        }
        Skeleton(Modifier.padding(start = 20.dp, top = 28.dp).size(170.dp, 16.dp), 6.dp)
    }
}

@Composable
private fun ErrorCard(error: String, onRetry: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxWidth().background(Fl.card, shape).border(1.dp, Fl.line, shape).padding(20.dp)
            .semantics(mergeDescendants = false) { contentDescription = "Couldn't load more templates" },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconWell(R.drawable.ic_offline, size = 40.dp, iconSize = 20.dp)
            Text(stringResource(R.string.s_couldn_t_load_more_templates), style = FlType.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = Fl.ink)
        }
        Text(stringResource(R.string.s_designs_built_into_the_app_still_work_ch), style = FlType.bodyS, color = Fl.muted)
        Text(
            if (error.isBlank()) stringResource(R.string.s_error_network_request_failed) else stringResource(R.string.home_error_line, error),
            style = FlType.monoLabel.copy(letterSpacing = 0.sp, fontFamily = DmMono), color = Fl.muted,
        )
        DarkPill(stringResource(R.string.s_try_again), onRetry, Modifier.padding(top = 4.dp), icon = R.drawable.ic_rotate)
    }
}

/** H5 "ALSO TRY" suggestions row. */
@Composable
private fun AlsoTry(items: List<String>, onPick: (String) -> Unit) {
    Row(
        Modifier.padding(top = 12.dp).fillMaxWidth().height(34.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.s_also_try), style = FlType.monoLabel.copy(lineHeight = 14.sp, letterSpacing = 0.6.sp), color = Fl.muted)
        items.forEach { OutlineChip(it, { onPick(it) }) }
    }
}

/** H6 no results. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoResults(q: String, suggestions: List<String>, onPick: (String) -> Unit, onClear: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 42.dp).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.img_empty_search), null, Modifier.size(180.dp, 174.dp), contentScale = ContentScale.Fit)
        Text(
            stringResource(R.string.home_nothing_matches, q),
            style = FlType.title.copy(letterSpacing = (-0.4).sp), color = Fl.ink, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp).widthIn(max = 326.dp),
        )
        Text(
            stringResource(R.string.s_try_a_shorter_word_or_start_from_one_of), style = FlType.bodyS, color = Fl.muted,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
        )
        FlowRow(
            Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { suggestions.forEach { OutlineChip(it, { onPick(it) }, height = 36.dp) } }
        DarkPill(stringResource(R.string.s_clear_search), onClear, Modifier.padding(top = 20.dp))
    }
}

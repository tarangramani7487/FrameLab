package com.framelab.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.framelab.app.BuildConfig
import com.framelab.app.data.catalog.BuiltInCatalog
import com.framelab.app.data.catalog.Category
import com.framelab.app.data.model.Design
import com.framelab.app.data.model.Template
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private val Context.dataStore by preferencesDataStore("framelab")

val FlJson = Json { ignoreUnknownKeys = true; encodeDefaults = true; classDiscriminator = "type" }

// ───────────────────────────── Models ─────────────────────────────

@Serializable
data class Draft(
    val id: String,
    /** User-visible name (Rename draft). Defaults to the template name. */
    val name: String,
    val design: Design,
    val updatedAt: Long,
) {
    val filled get() = design.filledCount
    val total get() = design.photoCount
    val ready get() = total > 0 && filled == total
}

@Serializable
data class SavedPost(
    val id: String,
    val name: String,
    val design: Design,
    /** content:// URIs of the saved images, in order. */
    val images: List<String>,
    val savedAt: Long,
    val width: Int = 1080,
    val height: Int = 1350,
)

enum class Quality { High, Max }
enum class FileFormat { JPG, PNG }
enum class ThemeMode { Light, Dark, System }

data class Settings(
    val onboardingDone: Boolean = false,
    val coachDone: Boolean = false,
    val quality: Quality = Quality.High,
    val format: FileFormat = FileFormat.JPG,
    val numberFiles: Boolean = true,
    val album: Boolean = true,
    val alwaysNumbers: Boolean = false,
    val snapRotation: Boolean = true,
    val haptics: Boolean = true,
    val theme: ThemeMode = ThemeMode.Light,
    val includeDeviceInfo: Boolean = true,
)

data class Profile(
    val signedIn: Boolean = false,
    val name: String = "",
    val handle: String = "",
    val email: String = "",
    val avatarUri: String? = null,
    val isPro: Boolean = false,
) {
    val initials: String get() = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
}

data class CatalogState(
    val loading: Boolean = true,
    /** Backend unreachable → built-in templates + offline banner. */
    val offline: Boolean = false,
    val error: String? = null,
    val templates: List<Template> = emptyList(),
    val categories: List<Category> = emptyList(),
    val frames: List<Template> = emptyList(),
    val frameCategories: List<Category> = emptyList(),
) {
    val totalDesigns: Int get() = templates.size + frames.size
}

// ───────────────────────────── Repositories ─────────────────────────────

class TemplateRepository(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    fun template(id: String): Template? = BuiltInCatalog.byId(id)

    /** Loads the catalogue: Supabase read-only when configured, else / on failure the built-in copy. */
    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        scope.launch {
            val builtIn = CatalogState(
                loading = false,
                templates = BuiltInCatalog.templates, categories = BuiltInCatalog.categories,
                frames = BuiltInCatalog.frames, frameCategories = BuiltInCatalog.frameCategories,
            )
            if (BuildConfig.SUPABASE_URL.isBlank()) {
                kotlinx.coroutines.delay(450)
                _state.value = builtIn
                return@launch
            }
            val result = runCatching { withContext(Dispatchers.IO) { pingSupabase() } }
            _state.value = if (result.isSuccess) builtIn
            else builtIn.copy(offline = true, error = result.exceptionOrNull()?.let { "ERR · ${it.javaClass.simpleName}" })
        }
    }

    private fun pingSupabase() {
        val c = URL("${BuildConfig.SUPABASE_URL}/rest/v1/template_categories?select=id&limit=1").openConnection() as HttpURLConnection
        c.connectTimeout = 6000; c.readTimeout = 6000
        c.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
        c.setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
        try { if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}") } finally { c.disconnect() }
    }

    /** For previews/tests: force the offline state. */
    fun setOffline(offline: Boolean) = _state.update { it.copy(offline = offline) }
}

/** Stores a list of JSON documents as files in a folder. */
private class JsonFolder<T>(private val dir: File, private val ser: kotlinx.serialization.KSerializer<T>) {
    init { dir.mkdirs() }
    fun readAll(): List<T> = dir.listFiles()?.mapNotNull { f -> runCatching { FlJson.decodeFromString(ser, f.readText()) }.getOrNull() } ?: emptyList()
    fun write(id: String, v: T) = File(dir, "$id.json").writeText(FlJson.encodeToString(ser, v))
    fun delete(id: String) { File(dir, "$id.json").delete() }
    fun clear() { dir.listFiles()?.forEach { it.delete() } }
}

class DraftRepository(context: Context, private val scope: CoroutineScope) {
    private val folder = JsonFolder(File(context.filesDir, "drafts"), Draft.serializer())
    private val _drafts = MutableStateFlow<List<Draft>>(emptyList())
    val drafts: StateFlow<List<Draft>> = _drafts.asStateFlow()

    init { scope.launch(Dispatchers.IO) { _drafts.value = folder.readAll().sortedByDescending { it.updatedAt } } }

    fun get(id: String) = _drafts.value.firstOrNull { it.id == id }

    /** Creates or updates a draft and returns its id. */
    fun save(design: Design, id: String? = null, name: String? = null): String {
        val existing = id?.let { get(it) }
        val d = Draft(id ?: UUID.randomUUID().toString(), name ?: existing?.name ?: design.name, design, System.currentTimeMillis())
        _drafts.update { (listOf(d) + it.filter { x -> x.id != d.id }).sortedByDescending { x -> x.updatedAt } }
        scope.launch(Dispatchers.IO) { folder.write(d.id, d) }
        return d.id
    }

    fun rename(id: String, name: String) { get(id)?.let { save(it.design, id, name) } }

    fun duplicate(id: String): String? = get(id)?.let { save(it.design, null, it.name) }

    fun delete(ids: Collection<String>) {
        _drafts.update { it.filter { d -> d.id !in ids } }
        scope.launch(Dispatchers.IO) { ids.forEach(folder::delete) }
    }

    fun deleteAll() { _drafts.value = emptyList(); scope.launch(Dispatchers.IO) { folder.clear() } }
}

class SavedRepository(context: Context, private val scope: CoroutineScope) {
    private val folder = JsonFolder(File(context.filesDir, "saved"), SavedPost.serializer())
    private val _posts = MutableStateFlow<List<SavedPost>>(emptyList())
    val posts: StateFlow<List<SavedPost>> = _posts.asStateFlow()

    init { scope.launch(Dispatchers.IO) { _posts.value = folder.readAll().sortedByDescending { it.savedAt } } }

    fun get(id: String) = _posts.value.firstOrNull { it.id == id }

    fun add(post: SavedPost) {
        _posts.update { listOf(post) + it.filter { p -> p.id != post.id } }
        scope.launch(Dispatchers.IO) { folder.write(post.id, post) }
    }

    fun delete(id: String) {
        _posts.update { it.filter { p -> p.id != id } }
        scope.launch(Dispatchers.IO) { folder.delete(id) }
    }
}

class PrefsRepository(private val context: Context, private val scope: CoroutineScope) {
    private object K {
        val onboarding = booleanPreferencesKey("onboarding_done")
        val coach = booleanPreferencesKey("coach_done")
        val quality = stringPreferencesKey("quality")
        val format = stringPreferencesKey("format")
        val numberFiles = booleanPreferencesKey("number_files")
        val album = booleanPreferencesKey("album")
        val alwaysNumbers = booleanPreferencesKey("always_numbers")
        val snap = booleanPreferencesKey("snap_rotation")
        val haptics = booleanPreferencesKey("haptics")
        val theme = stringPreferencesKey("theme")
        val deviceInfo = booleanPreferencesKey("device_info")
        val favourites = stringSetPreferencesKey("favourites")
        val signedIn = booleanPreferencesKey("signed_in")
        val name = stringPreferencesKey("name")
        val handle = stringPreferencesKey("handle")
        val email = stringPreferencesKey("email")
        val avatar = stringPreferencesKey("avatar")
        val pro = booleanPreferencesKey("pro")
    }

    private val prefs = context.dataStore.data

    val settings: StateFlow<Settings?> = prefs.map { p ->
        Settings(
            onboardingDone = p[K.onboarding] ?: false,
            coachDone = p[K.coach] ?: false,
            quality = p[K.quality]?.let { runCatching { Quality.valueOf(it) }.getOrNull() } ?: Quality.High,
            format = p[K.format]?.let { runCatching { FileFormat.valueOf(it) }.getOrNull() } ?: FileFormat.JPG,
            numberFiles = p[K.numberFiles] ?: true,
            album = p[K.album] ?: true,
            alwaysNumbers = p[K.alwaysNumbers] ?: false,
            snapRotation = p[K.snap] ?: true,
            haptics = p[K.haptics] ?: true,
            theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.Light,
            includeDeviceInfo = p[K.deviceInfo] ?: true,
        )
    }.stateIn(scope, SharingStarted.Eagerly, null)

    val favourites: StateFlow<Set<String>> = prefs.map { it[K.favourites] ?: emptySet() }.stateIn(scope, SharingStarted.Eagerly, emptySet())

    val profile: StateFlow<Profile> = prefs.map { p ->
        Profile(
            signedIn = p[K.signedIn] ?: false, name = p[K.name] ?: "", handle = p[K.handle] ?: "",
            email = p[K.email] ?: "", avatarUri = p[K.avatar], isPro = p[K.pro] ?: false,
        )
    }.stateIn(scope, SharingStarted.Eagerly, Profile())

    private fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch { context.dataStore.edit { block(it) } }
    }

    fun setOnboardingDone() = edit { it[K.onboarding] = true }
    fun setCoachDone() = edit { it[K.coach] = true }
    fun updateSettings(s: Settings) = edit {
        it[K.quality] = s.quality.name; it[K.format] = s.format.name; it[K.numberFiles] = s.numberFiles
        it[K.album] = s.album; it[K.alwaysNumbers] = s.alwaysNumbers; it[K.snap] = s.snapRotation
        it[K.haptics] = s.haptics; it[K.theme] = s.theme.name; it[K.deviceInfo] = s.includeDeviceInfo
    }

    fun isFavourite(id: String) = id in favourites.value
    fun toggleFavourite(id: String) = edit {
        val cur = it[K.favourites] ?: emptySet()
        it[K.favourites] = if (id in cur) cur - id else cur + id
    }

    fun signIn(email: String, name: String = "", handle: String = "") = edit {
        it[K.signedIn] = true; it[K.email] = email
        if (name.isNotEmpty()) it[K.name] = name
        if (handle.isNotEmpty()) it[K.handle] = handle
    }
    fun updateProfile(name: String, handle: String, avatar: String?) = edit {
        it[K.name] = name; it[K.handle] = handle
        if (avatar == null) it.remove(K.avatar) else it[K.avatar] = avatar
    }
    fun signOut() = edit { it[K.signedIn] = false }
    fun deleteAccount() = edit { p ->
        listOf<Preferences.Key<*>>(K.signedIn, K.name, K.handle, K.email, K.avatar, K.favourites).forEach { p.remove(it) }
    }
    fun setPro(pro: Boolean) = edit { it[K.pro] = pro }
}

/** Simple service locator, created by [com.framelab.app.FrameLabApplication]. */
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val templates = TemplateRepository(scope)
    val drafts = DraftRepository(context, scope)
    val saved = SavedRepository(context, scope)
    val prefs = PrefsRepository(context, scope)

    init { templates.refresh() }
}

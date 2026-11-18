package com.framelab.app.feature.editor

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.framelab.app.BuildConfig

/**
 * Debug-only screenshot hook. A template id like `torn-diary!partial!sel!frame` opens the editor in a given state:
 * `full` / `partial` (photos from MediaStore), `sel` (select spot 2), `lock`, `crop`, and a panel or sheet name
 * (`frame position style color order text stickers slides draft precheck progress error clear preview coach`).
 * Ignored in release builds.
 */
data class DebugFlags(val baseId: String?, val flags: Set<String>) {
    fun has(f: String) = BuildConfig.DEBUG && f in flags

    companion object {
        fun parse(templateId: String?): DebugFlags {
            if (templateId == null || !templateId.contains('!')) return DebugFlags(templateId, emptySet())
            val parts = templateId.split('!')
            return DebugFlags(parts.first(), if (BuildConfig.DEBUG) parts.drop(1).toSet() else emptySet())
        }
    }
}

/** Newest images in MediaStore (needs READ_MEDIA_IMAGES granted via adb for the hook). */
fun debugImages(context: Context, n: Int): List<String> = runCatching {
    val out = mutableListOf<String>()
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media._ID), null, null,
        "${MediaStore.Images.Media.DATE_ADDED} DESC",
    )?.use { c -> while (c.moveToNext() && out.size < n) out += ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0)).toString() }
    out
}.getOrDefault(emptyList())

/** Applies debug flags once the editor has opened. */
fun applyDebug(st: EditorState, flags: DebugFlags, context: Context) {
    if (flags.flags.isEmpty()) return
    val order = st.design.spotsInFillOrder
    val imgs = debugImages(context, 12)
    if (imgs.isNotEmpty() && (flags.has("full") || flags.has("partial"))) {
        val pick = if (flags.has("full")) order.indices.toList() else listOf(0, 2, 4, 5, 7)
        var d = st.design
        pick.forEachIndexed { k, i -> order.getOrNull(i)?.let { s -> d = d.replace(s.copy(uri = imgs[k % imgs.size])) } }
        st.reset(d)
        if (flags.has("sel")) {
            // E7 shows the "first swim" polaroid photo filled and selected.
            val b = order.getOrNull(1)
            if (b != null) { st.reset(st.design.replace(b.copy(uri = imgs[1 % imgs.size]))); st.select(b.id) }
        }
    }
    if (flags.has("lock")) st.selectedId?.let { id -> st.updateSpot(id) { it.copy(locked = true) } }
    when {
        flags.has("frame") -> st.panel = Panel.Frame
        flags.has("position") -> st.panel = Panel.Position
        flags.has("style") -> st.panel = Panel.Style
        flags.has("color") -> { st.panel = Panel.Color; st.setBackground(0xFF16130F) }
        flags.has("order") -> st.panel = Panel.Order
        flags.has("stickers") -> st.sheet = EdSheet.Stickers
        flags.has("slides") -> { st.slide = 1; st.sheet = EdSheet.Slides }
        flags.has("draft") -> st.sheet = EdSheet.Draft
        flags.has("precheck") -> st.sheet = EdSheet.Precheck
        flags.has("error") -> { st.failedSlide = 3; st.errorMessage = "Not enough storage space"; st.sheet = EdSheet.Error }
        flags.has("progress") -> { st.saving = true; st.savedCount = 2; st.sheet = EdSheet.Progress }
        flags.has("clear") -> st.sheet = EdSheet.Clear
        flags.has("preview") -> st.preview = true
        flags.has("text") -> {
            val t = st.design.elements.filterIsInstance<com.framelab.app.data.model.El.Text>().firstOrNull()
            if (t != null) { st.editingTextId = t.id; st.beginLive(); st.sheet = EdSheet.Text }
        }
    }
    if (flags.has("crop")) { st.panel = Panel.Position; st.crop = true; st.selectedId?.let { id -> st.updateSpot(id) { it.copy(zoom = 1.5f) } } }
    if (flags.has("coach")) st.coachStep = 0
}

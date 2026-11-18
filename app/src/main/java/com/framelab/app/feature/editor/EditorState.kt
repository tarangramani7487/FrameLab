package com.framelab.app.feature.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextMeasurer
import com.framelab.app.data.model.Design
import com.framelab.app.data.model.El
import com.framelab.app.data.model.PhotoFit
import com.framelab.app.data.model.SLIDE_W
import com.framelab.app.data.model.SpotShape
import com.framelab.app.data.model.withId
import com.framelab.app.data.model.withPos
import com.framelab.app.render.contains
import com.framelab.app.render.measureText
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Panels that replace the hint line (HANDOFF §5.20). */
enum class Panel { None, Color, Order, Frame, Position, Style }

/** Bottom sheets / dialogs owned by the editor. */
enum class EdSheet { None, Text, Stickers, Slides, Draft, Precheck, Progress, Error, Clear }

/** Loading state of the editor (E1/E2). */
enum class OpenState { Opening, Open, Failed }

/**
 * Editor document + UI state. Every committed edit pushes the previous [Design] onto the undo stack (50 steps).
 * Continuous gestures call [beginLive] → [live] … → [commitLive] so a drag is one undo step.
 */
@Stable
class EditorState(initial: Design?, val draftIdInitial: String?) {
    var design by mutableStateOf(initial ?: Design("", "", slides = 1, background = 0xFFEFE6D6, elements = emptyList()))
        private set
    var open by mutableStateOf(if (initial == null) OpenState.Failed else OpenState.Opening)
    var draftId by mutableStateOf(draftIdInitial)
    var dirty by mutableStateOf(false)

    private val undoStack = ArrayDeque<Design>()
    private val redoStack = ArrayDeque<Design>()
    var canUndo by mutableStateOf(false); private set
    var canRedo by mutableStateOf(false); private set

    var slide by mutableIntStateOf(0)
    var selectedId by mutableStateOf<String?>(null)
    var panel by mutableStateOf(Panel.None)
    var sheet by mutableStateOf(EdSheet.None)
    var crop by mutableStateOf(false)
    var preview by mutableStateOf(false)
    var editingTextId by mutableStateOf<String?>(null)
    var coachStep by mutableIntStateOf(-1)

    // Saving
    var saving by mutableStateOf(false)
    var savedCount by mutableIntStateOf(0)
    var failedSlide by mutableIntStateOf(0)
    var errorMessage by mutableStateOf("")

    private var liveBase: Design? = null

    val selected: El? get() = selectedId?.let { design.element(it) }
    val selectedSpot: El.Spot? get() = selected as? El.Spot

    // ───────────── history ─────────────

    fun edit(transform: (Design) -> Design) {
        val before = design
        val after = transform(before)
        if (after == before) return
        push(before)
        design = after
    }

    private fun push(before: Design) {
        undoStack.addLast(before)
        while (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
        dirty = true
        sync()
    }

    fun beginLive() { if (liveBase == null) liveBase = design }
    fun live(d: Design) { design = d }
    fun commitLive() {
        val base = liveBase ?: return
        liveBase = null
        if (base != design) push(base)
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(design); design = prev; dirty = true; validateSelection(); sync()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(design); design = next; dirty = true; validateSelection(); sync()
    }

    private fun sync() { canUndo = undoStack.isNotEmpty(); canRedo = redoStack.isNotEmpty() }

    private fun validateSelection() {
        if (selectedId != null && design.element(selectedId!!) == null) deselect()
        slide = slide.coerceIn(0, design.slides - 1)
    }

    /** Replaces the document without history (used after loading). */
    fun reset(d: Design) { design = d; undoStack.clear(); redoStack.clear(); sync() }

    // ───────────── selection ─────────────

    fun select(id: String) {
        selectedId = id; crop = false
        if (panel == Panel.Color || panel == Panel.Order) panel = Panel.None
    }

    fun deselect() { selectedId = null; crop = false; if (panel in listOf(Panel.Frame, Panel.Position, Panel.Style)) panel = Panel.None }

    // ───────────── photos ─────────────

    /** Fills empty spots (fill order) starting at [startId] (or the first empty) with [uris]. Full → refill all. */
    fun fillPhotos(uris: List<String>, startId: String? = null) {
        if (uris.isEmpty()) return
        edit { d ->
            val order = d.spotsInFillOrder
            val targets: List<El.Spot> = when {
                startId != null -> {
                    val i = order.indexOfFirst { it.id == startId }.coerceAtLeast(0)
                    (order.drop(i) + order.take(i)).filter { it.uri == null || it.id == startId }
                }
                order.any { it.uri == null } -> order.filter { it.uri == null }
                else -> order.filter { !it.locked }
            }
            var out = d
            targets.zip(uris).forEach { (s, u) -> out = out.replace(s.copy(uri = u, zoom = 1f, panX = 0f, panY = 0f, turns = 0, flip = false)) }
            out
        }
    }

    fun replacePhoto(id: String, uri: String) = edit { d ->
        val s = d.element(id) as? El.Spot ?: return@edit d
        d.replace(s.copy(uri = uri, zoom = 1f, panX = 0f, panY = 0f, turns = 0))
    }

    /** Swaps photos between unlocked filled spots and resets their position. */
    fun shuffle() = edit { d ->
        val spots = d.spots.filter { !it.locked && it.uri != null }
        if (spots.size < 2) return@edit d
        var uris = spots.map { it.uri!! }
        var tries = 0
        while (tries++ < 8) { val s = uris.shuffled(); if (s != uris) { uris = s; break } }
        var out = d
        spots.zip(uris).forEach { (s, u) -> out = out.replace(s.copy(uri = u, zoom = 1f, panX = 0f, panY = 0f, turns = 0, flip = false)) }
        out
    }

    /** Removes all unlocked photos; returns how many were removed. */
    fun clearPhotos(): Int {
        val n = design.spots.count { it.uri != null && !it.locked }
        edit { d -> d.copy(elements = d.elements.map { if (it is El.Spot && it.uri != null && !it.locked) it.copy(uri = null, zoom = 1f, panX = 0f, panY = 0f, turns = 0, flip = false) else it }) }
        deselect()
        return n
    }

    fun setBackground(color: Long) = edit { it.copy(background = color) }

    // ───────────── selected element ops ─────────────

    fun updateSpot(id: String, f: (El.Spot) -> El.Spot) = edit { d ->
        val s = d.element(id) as? El.Spot ?: return@edit d
        d.replace(f(s))
    }

    fun setShape(shape: SpotShape) = selectedId?.let { id -> updateSpot(id) { it.copy(shape = shape) } }
    fun setFit(fit: PhotoFit) = selectedId?.let { id -> updateSpot(id) { it.copy(fit = fit) } }
    fun toggleFlip() = selectedId?.let { id -> updateSpot(id) { it.copy(flip = !it.flip) } }
    fun zoomBy(f: Float) = selectedId?.let { id -> updateSpot(id) { it.copy(zoom = (it.zoom * f).coerceIn(1f, 6f)) } }
    fun turnPhoto() = selectedId?.let { id -> updateSpot(id) { it.copy(turns = (it.turns + 1) % 4) } }
    fun centerPhoto() = selectedId?.let { id -> updateSpot(id) { it.copy(panX = 0f, panY = 0f) } }
    fun toggleLock() = selectedId?.let { id -> updateSpot(id) { it.copy(locked = !it.locked) } }

    fun resetSize() {
        val s = selectedSpot ?: return
        edit { d -> rigTransform(d, s, s.x + s.w / 2f, s.y + s.h / 2f, s.baseW, s.baseH, s.rotation) }
    }

    /** Rotates the selected element by +90°. */
    fun rotate90() {
        val e = selected ?: return
        if ((e as? El.Spot)?.locked == true) return
        val target = (Math.round(e.rotation / 90f) * 90f + 90f).let { if (it >= 360f) it - 360f else it }
        edit { d -> rigTransform(d, e, e.x + e.w / 2, e.y + e.h / 2, e.w, e.h, target) }
    }

    /** Delete: clears a template spot's photo, removes duplicated spots and stickers. */
    fun deleteSelected() {
        val e = selected ?: return
        edit { d ->
            when {
                e is El.Spot && e.locked -> d
                e is El.Spot && !e.id.contains("~") -> d.replace(e.copy(uri = null, zoom = 1f, panX = 0f, panY = 0f, turns = 0, flip = false))
                else -> {
                    val rig = attachedIds(d, e)
                    d.copy(elements = d.elements.filter { it.id != e.id && it.id !in rig })
                }
            }
        }
        deselect()
    }

    fun duplicateSelected() {
        val e = selected ?: return
        val stamp = System.nanoTime().toString(36)
        val rig = attachedIds(design, e)
        val group = design.elements.filter { it.id in rig || it.id == e.id }
        val copies = group.map { it.withId("${it.id}~$stamp").withPos(x = it.x + 16f, y = it.y + 16f) }
        edit { d -> d.copy(elements = d.elements + copies) }
        selectedId = "${e.id}~$stamp"
    }

    fun addSticker(art: String) {
        val id = "st~" + System.nanoTime().toString(36)
        val cx = slide * SLIDE_W + SLIDE_W / 2f
        val cy = design.slideH / 2f
        edit { d -> d.copy(elements = d.elements + El.Sticker(id, cx - 40f, cy - 40f, 80f, 80f, 0f, art)) }
        select(id)
    }

    fun updateText(id: String, f: (El.Text) -> El.Text) {
        val t = design.element(id) as? El.Text ?: return
        design = design.replace(f(t))
    }

    // ───────────── slides (N22) ─────────────

    /** Consecutive slides joined by spanning photos move together. */
    fun blocks(d: Design = design): List<IntRange> {
        val links = d.linkedPairs()
        val out = mutableListOf<IntRange>()
        var start = 0
        for (i in 0 until d.slides) {
            if (i !in links) { out += start..i; start = i + 1 }
        }
        return out
    }

    fun blockOf(i: Int, d: Design = design) = blocks(d).first { i in it }

    private fun slideOfEl(e: El): Int = ((e.x + e.w / 2f) / SLIDE_W).toInt()

    fun moveSlide(i: Int, dir: Int) {
        val bl = blocks()
        val bi = bl.indexOfFirst { i in it }
        val oi = bi + dir
        if (oi !in bl.indices) return
        val a = if (dir < 0) bl[oi] else bl[bi]
        val b = if (dir < 0) bl[bi] else bl[oi]
        val la = a.count(); val lb = b.count()
        edit { d ->
            d.copy(elements = d.elements.map { e ->
                when (slideOfEl(e).coerceIn(0, d.slides - 1)) {
                    in a -> e.withPos(x = e.x + lb * SLIDE_W)
                    in b -> e.withPos(x = e.x - la * SLIDE_W)
                    else -> e
                }
            })
        }
        slide = if (dir < 0) a.first + (i - b.first) else (i - a.first) + a.first + lb
    }

    fun duplicateSlide(i: Int) {
        val b = blockOf(i)
        val len = b.count()
        val stamp = System.nanoTime().toString(36)
        edit { d ->
            val shifted = d.elements.map { e -> if (slideOfEl(e) > b.last) e.withPos(x = e.x + len * SLIDE_W) else e }
            val copies = d.elements.filter { slideOfEl(it) in b }.map { it.withId("${it.id}~$stamp").withPos(x = it.x + len * SLIDE_W) }
            d.copy(slides = d.slides + len, elements = shifted + copies)
        }
        slide = b.last + 1
    }

    fun deleteSlide(i: Int) {
        if (design.slides <= 1) return
        val b = blockOf(i)
        val len = b.count()
        if (len >= design.slides) return
        edit { d ->
            d.copy(
                slides = d.slides - len,
                elements = d.elements.filter { slideOfEl(it) !in b }.map { e -> if (slideOfEl(e) > b.last) e.withPos(x = e.x - len * SLIDE_W) else e },
            )
        }
        deselect()
        slide = slide.coerceIn(0, design.slides - 1)
    }

    /** Appends a copy of the last slide's layout with empty photo spots. */
    fun addSlide() {
        val last = design.slides - 1
        val stamp = System.nanoTime().toString(36)
        edit { d ->
            val copies = d.elements.filter { slideOfEl(it) == last && it.x >= last * SLIDE_W - 40f }.map {
                val c = it.withId("${it.id}~$stamp").withPos(x = it.x + SLIDE_W)
                if (c is El.Spot) c.copy(uri = null, locked = false, zoom = 1f, panX = 0f, panY = 0f, turns = 0) else c
            }
            d.copy(slides = d.slides + 1, elements = d.elements + copies)
        }
        slide = design.slides - 1
    }

    // ───────────── text ─────────────

    fun textSize(t: El.Text, measurer: TextMeasurer): Pair<Float, Float> {
        val s = measureText(t, measurer)
        return s.width to s.height
    }

    companion object {
        /** Elements that ride with a photo spot (polaroid frame + caption). */
        fun attachedIds(d: Design, e: El): Set<String> {
            if (e !is El.Spot) return emptySet()
            val c = Offset(e.x + e.w / 2f, e.y + e.h / 2f)
            val frames = d.elements.filterIsInstance<El.Box>().filter { b ->
                !b.oval && b.polygon == null && abs(b.rotation - e.rotation) < 0.6f &&
                    b.w >= e.w - 1f && b.h >= e.h - 1f && b.w * b.h < 2.6f * e.w * e.h && b.contains(c)
            }
            val ids = frames.map { it.id }.toMutableSet()
            frames.forEach { f ->
                d.elements.filterIsInstance<El.Text>().forEach { t ->
                    val tc = Offset(t.x + (if (t.w > 0) t.w else t.size * 3) / 2f, t.y + t.size * 0.6f)
                    if (abs(t.rotation - f.rotation) < 0.6f && f.contains(tc)) ids += t.id
                }
            }
            return ids
        }

        /**
         * Moves/resizes/rotates element [e0] (as it was in [base]) to centre (cx, cy), size (w, h), [rotation], carrying
         * its attached frame/caption along (positions mapped through the element's local frame).
         */
        fun rigTransform(base: Design, e0: El, cx: Float, cy: Float, w: Float, h: Float, rotation: Float): Design {
            val rig = attachedIds(base, e0)
            val c0 = Offset(e0.x + e0.w / 2f, e0.y + e0.h / 2f)
            val sx = w / e0.w.coerceAtLeast(1f); val sy = h / e0.h.coerceAtLeast(1f)
            val r0 = -e0.rotation * PI.toFloat() / 180f
            val r1 = rotation * PI.toFloat() / 180f
            val dr = rotation - e0.rotation
            return base.copy(elements = base.elements.map { el ->
                when {
                    el.id == e0.id -> el.withPos(x = cx - w / 2f, y = cy - h / 2f, w = w, h = h, rotation = rotation)
                    el.id in rig -> {
                        val ew = if (el is El.Box) el.w * sx else el.w
                        val eh = if (el is El.Box) el.h * sy else el.h
                        val ec = Offset(el.x + el.w / 2f, el.y + el.h / 2f) - c0
                        val lx = (ec.x * cos(r0) - ec.y * sin(r0)) * sx
                        val ly = (ec.x * sin(r0) + ec.y * cos(r0)) * sy
                        val wx = cx + lx * cos(r1) - ly * sin(r1)
                        val wy = cy + lx * sin(r1) + ly * cos(r1)
                        el.withPos(x = wx - ew / 2f, y = wy - eh / 2f, w = ew, h = eh, rotation = el.rotation + dr)
                    }
                    else -> el
                }
            })
        }
    }
}

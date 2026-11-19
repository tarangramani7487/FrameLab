package com.htr.collagemaker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.sin

/**
 * Template / design document model (HANDOFF §8).
 *
 * Coordinates are "design units": one slide is [SLIDE_W] units wide (342, the editor slide width in dp)
 * and [Ratio.slideHeight] tall. A carousel is N slides side by side on one long canvas, so slide k
 * spans x ∈ [k·342, (k+1)·342). Elements may cross slide edges (seamless carousels).
 * Every element is top-level, in world coordinates, and rotates around its own centre.
 * Colours are ARGB longs so the whole document is serialisable (drafts).
 */
const val SLIDE_W = 342f

@Serializable
enum class Ratio(val label: String, val pxW: Int, val pxH: Int) {
    @SerialName("1:1") Square("1:1", 1080, 1080),
    @SerialName("4:5") Portrait("4:5", 1080, 1350),
    @SerialName("3:4") ThreeFour("3:4", 1080, 1440),
    @SerialName("9:16") Story("9:16", 1080, 1920);

    /** Slide height in design units (4:5 → 428 like the mockups). */
    val slideHeight: Float get() = if (this == Portrait) 428f else SLIDE_W * pxH / pxW
}

@Serializable
enum class FontKind { Sans, Serif, Mono }

@Serializable
enum class TextAlignKind { Left, Center, Right }

@Serializable
enum class SpotShape { Original, Square, Rounded, Circle, Arch }

@Serializable
enum class PhotoFit { Fill, Fit }

@Serializable
data class Shadow(val dx: Float = 0f, val dy: Float = 3f, val blur: Float = 8f, val color: Long = 0x40281C0F)

@Serializable
sealed interface Fill {
    @Serializable @SerialName("solid")
    data class Solid(val color: Long) : Fill

    /** Diary paper: base colour + horizontal rules every [spacing] units, optional red margin line at [marginX]. */
    @Serializable @SerialName("ruled")
    data class Ruled(
        val base: Long = 0xFFFBF6EC,
        val rule: Long = 0x26786046,
        val spacing: Float = 18f,
        val marginX: Float? = null,
        val margin: Long = 0x59D65C50,
    ) : Fill

    /** Masking tape / film holes: base colour with repeating stripes along x (or y when [vertical]). */
    @Serializable @SerialName("stripes")
    data class Stripes(
        val base: Long,
        val stripe: Long,
        val on: Float = 3f,
        val off: Float = 4f,
        val vertical: Boolean = false,
        val inset: Float = 0f,
    ) : Fill

    /** Linear gradient top → bottom (or at [angleDeg], CSS convention: 180 = downwards). */
    @Serializable @SerialName("linear")
    data class Linear(val colors: List<Long>, val stops: List<Float>? = null, val angleDeg: Float = 180f) : Fill

    /** Film-strip sprocket holes: rectangles of [hole] colour on [base]. */
    @Serializable @SerialName("sprockets")
    data class Sprockets(val base: Long, val hole: Long, val holeW: Float = 6f, val holeH: Float = 6f, val pitch: Float = 12f) : Fill
}

@Serializable
enum class Edge { None, Perforated }

@Serializable
sealed interface El {
    val id: String
    val x: Float
    val y: Float
    val w: Float
    val h: Float
    val rotation: Float

    /** Shape element: paper, polaroid frame, tape, film strip, circle mark… */
    @Serializable @SerialName("box")
    data class Box(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val w: Float,
        override val h: Float,
        override val rotation: Float = 0f,
        val fill: Fill? = null,
        val radius: Float = 0f,
        /** Polygon clip in fractions of w/h (0..1), e.g. torn paper edges. */
        val polygon: List<Float>? = null,
        val borderColor: Long? = null,
        val borderWidth: Float = 0f,
        val dashed: Boolean = false,
        val shadow: Shadow? = null,
        val alpha: Float = 1f,
        val edge: Edge = Edge.None,
        val oval: Boolean = false,
        /** Per-corner radii [tl, tr, br, bl] (backend layouts); overrides [radius] when set. */
        val radii: List<Float>? = null,
    ) : El

    /** Template text. Editable from the editor (N20). */
    @Serializable @SerialName("text")
    data class Text(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val w: Float,
        override val h: Float = 0f,
        override val rotation: Float = 0f,
        val text: String,
        val font: FontKind = FontKind.Serif,
        val size: Float = 14f,
        val lineHeight: Float = 0f,
        val weight: Int = 400,
        val italic: Boolean = false,
        val align: TextAlignKind = TextAlignKind.Left,
        val tracking: Float = 0f,
        val color: Long = 0xFF2B2621,
        val glow: Long? = null,
        val caps: Boolean = false,
        /** Vertical position of the text inside [h]: 0 top, 0.5 centre, 1 bottom (used when h > 0). */
        val vAlign: Float = 0f,
    ) : El

    /** Photo spot. Holds the user's photo and its transform. */
    @Serializable @SerialName("spot")
    data class Spot(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val w: Float,
        override val h: Float,
        override val rotation: Float = 0f,
        val radius: Float = 0f,
        val shape: SpotShape = SpotShape.Original,
        /** Pastel placeholder index for thumbnails (0..5) or explicit colour. */
        val pastel: Long = 0xFFF3CFC3,
        val uri: String? = null,
        val zoom: Float = 1f,
        /** Pan of the image inside the box, in fractions of the overflow (-1..1). */
        val panX: Float = 0f,
        val panY: Float = 0f,
        val fit: PhotoFit = PhotoFit.Fill,
        val flip: Boolean = false,
        val locked: Boolean = false,
        /** Quarter turns applied to the image inside the box ("Turn photo"). */
        val turns: Int = 0,
        /** Original geometry from the template, for "Reset size". */
        val baseW: Float = w,
        val baseH: Float = h,
        val label: String? = null,
        /** Per-corner radii [tl, tr, br, bl] (backend layouts); overrides [radius] when set. */
        val radii: List<Float>? = null,
        /** Fill order from the template (backend `slotIndex`); null = slide → top → left. */
        val order: Int? = null,
    ) : El

    /** Sticker (bundled art). */
    @Serializable @SerialName("sticker")
    data class Sticker(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val w: Float,
        override val h: Float,
        override val rotation: Float = 0f,
        val art: String,
        val tint: Long? = null,
    ) : El

    /** Polyline doodle (postmark waves, arrows) in local fractions. */
    @Serializable @SerialName("lines")
    data class Lines(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val w: Float,
        override val h: Float,
        override val rotation: Float = 0f,
        val paths: List<List<Float>>,
        val color: Long,
        val stroke: Float = 1.5f,
    ) : El
}

fun El.withPos(x: Float = this.x, y: Float = this.y, w: Float = this.w, h: Float = this.h, rotation: Float = this.rotation): El = when (this) {
    is El.Box -> copy(x = x, y = y, w = w, h = h, rotation = rotation)
    is El.Text -> copy(x = x, y = y, w = w, h = h, rotation = rotation)
    is El.Spot -> copy(x = x, y = y, w = w, h = h, rotation = rotation)
    is El.Sticker -> copy(x = x, y = y, w = w, h = h, rotation = rotation)
    is El.Lines -> copy(x = x, y = y, w = w, h = h, rotation = rotation)
}

fun El.withId(id: String): El = when (this) {
    is El.Box -> copy(id = id)
    is El.Text -> copy(id = id)
    is El.Spot -> copy(id = id)
    is El.Sticker -> copy(id = id)
    is El.Lines -> copy(id = id)
}

val El.cx get() = x + w / 2f
val El.cy get() = y + h / 2f

/** Index of the slide that contains this element's left edge (its "home" slide). */
fun El.homeSlide(): Int = (x / SLIDE_W).toInt().coerceAtLeast(0)

/** Slides this element touches. */
fun El.slideRange(slides: Int): IntRange {
    val a = (x / SLIDE_W).toInt().coerceIn(0, slides - 1)
    val b = ((x + w - 0.5f) / SLIDE_W).toInt().coerceIn(0, slides - 1)
    return a..b
}

/**
 * Places a child rect, given in the local coordinates of a rotated parent, into world space.
 * Returns (x, y, w, h, rotation) of the child — child keeps its size and inherits the parent rotation.
 */
fun nested(
    px: Float, py: Float, pw: Float, ph: Float, protation: Float,
    lx: Float, ly: Float, lw: Float, lh: Float, lrotation: Float = 0f,
): FloatArray {
    val pcx = px + pw / 2f
    val pcy = py + ph / 2f
    val dx = lx + lw / 2f - pw / 2f
    val dy = ly + lh / 2f - ph / 2f
    val r = Math.toRadians(protation.toDouble())
    val wx = pcx + (dx * cos(r) - dy * sin(r)).toFloat()
    val wy = pcy + (dx * sin(r) + dy * cos(r)).toFloat()
    return floatArrayOf(wx - lw / 2f, wy - lh / 2f, lw, lh, protation + lrotation)
}

@Serializable
enum class TemplateKind { Template, Frame }

/** Catalogue entry metadata. */
@Serializable
data class TemplateMeta(
    val id: String,
    val name: String,
    val kind: TemplateKind = TemplateKind.Template,
    val category: String,
    val style: String,
    val topic: String,
    val isNew: Boolean = false,
    val isPro: Boolean = false,
    val framelabMade: Boolean = false,
    /** Popularity for "Trending"/"Most used" sorts. */
    val uses: Int = 0,
    val createdOrder: Int = 0,
    /** Backend slide images (index = slide). Empty = cards and the details page draw the template live. */
    val thumbnails: List<String> = emptyList(),
) {
    /** Card image: the first slide's thumbnail, if any. */
    val thumbnail: String? get() = thumbnails.firstOrNull()

    /** Backend image for [slide], or null to draw that slide live. */
    fun thumbnailFor(slide: Int): String? = thumbnails.getOrNull(slide)
}

/** The editable design document (template copy + the user's edits). Drafts store this whole object. */
@Serializable
data class Design(
    val templateId: String,
    val name: String,
    val ratio: Ratio = Ratio.Portrait,
    val slides: Int,
    val background: Long,
    val templateBackground: Long = background,
    val elements: List<El>,
) {
    val canvasW: Float get() = slides * SLIDE_W
    val slideH: Float get() = ratio.slideHeight
    val spots: List<El.Spot> get() = elements.filterIsInstance<El.Spot>()

    /** Spots in fill order: slide → top → left (HANDOFF §8.5). */
    val spotsInFillOrder: List<El.Spot>
        get() = spots.let { all ->
            if (all.isNotEmpty() && all.all { it.order != null }) all.sortedBy { it.order }
            else all.sortedWith(compareBy({ it.homeSlide() }, { it.y }, { it.x }))
        }

    val photoCount: Int get() = spots.size
    val filledCount: Int get() = spots.count { it.uri != null }
    val emptyCount: Int get() = photoCount - filledCount

    /** Number (1-based) of a spot in fill order. */
    fun orderOf(id: String): Int = spotsInFillOrder.indexOfFirst { it.id == id } + 1

    fun element(id: String): El? = elements.firstOrNull { it.id == id }

    fun replace(el: El): Design = copy(elements = elements.map { if (it.id == el.id) el else it })

    /** Pairs of slides (a, a+1) that share a spanning photo (link badges in N22). */
    fun linkedPairs(): Set<Int> = spots.flatMap { s ->
        val r = s.slideRange(slides); if (r.first != r.last) (r.first until r.last).toList() else emptyList()
    }.toSet()
}

/** A template in the catalogue = metadata + the document it opens as. */
data class Template(val meta: TemplateMeta, val design: Design) {
    val id get() = meta.id
    val name get() = meta.name
    val slides get() = design.slides
    val photos get() = design.photoCount
    val ratio get() = design.ratio
}

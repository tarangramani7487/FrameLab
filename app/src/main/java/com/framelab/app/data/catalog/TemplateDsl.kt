package com.framelab.app.data.catalog

import com.framelab.app.data.model.Design
import com.framelab.app.data.model.El
import com.framelab.app.data.model.Fill
import com.framelab.app.data.model.FontKind
import com.framelab.app.data.model.Ratio
import com.framelab.app.data.model.Shadow
import com.framelab.app.data.model.TextAlignKind
import com.framelab.app.data.model.nested

/** Shared template-palette values (colors.xml tpl_* / pastel_*). */
object Tpl {
    const val KRAFT = 0xFFEFE6D6L
    const val PAPER = 0xFFFBF6ECL
    const val LAB_PRINT = 0xFFF4ECDCL
    const val INK = 0xFF2B2621L
    const val INK_SOFT = 0xFF4A4238L
    const val LABEL = 0xFF7A6E60L
    const val DATE = 0xFFFF8A3DL
    const val TAPE = 0xE6ECD6A3L
    const val RULE = 0x26786046L
    const val MARGIN = 0x59D65C50L
    const val WHITE = 0xFFFFFFFFL
    const val FL_INK = 0xFF16130FL
    val PASTELS = longArrayOf(0xFFF3CFC3L, 0xFFCDE7EAL, 0xFFE3E0F3L, 0xFFFDE7C4L, 0xFFD7EBDDL, 0xFFF6D3DCL)
    val PRINT_SHADOW = Shadow(0f, 3f, 8f, 0x40281C0FL)
    val PAPER_SHADOW = Shadow(0f, 3f, 4f, 0x38281C0FL)
    const val TAPE_STRIPE = 0x33FFFFFFL
}

/** Small builder so templates read like the design files (absolute dp positions on the long canvas). */
/** Card thumbnails are drawn at 128 × 160; slides are 342 wide. Multiply thumbnail px by [U]. */
const val U = 342f / 128f

class DesignBuilder(private val idPrefix: String) {
    val els = mutableListOf<El>()
    private var n = 0
    private var spotN = 0
    private fun nid(kind: String) = "$idPrefix-$kind-${n++}"

    fun box(
        x: Float, y: Float, w: Float, h: Float, rot: Float = 0f, fill: Fill? = null, radius: Float = 0f,
        polygon: List<Float>? = null, border: Long? = null, borderW: Float = 0f, dashed: Boolean = false,
        shadow: Shadow? = null, alpha: Float = 1f, oval: Boolean = false, edge: com.framelab.app.data.model.Edge = com.framelab.app.data.model.Edge.None,
    ): El.Box = El.Box(nid("box"), x, y, w, h, rot, fill, radius, polygon, border, borderW, dashed, shadow, alpha, edge, oval).also { els += it }

    fun text(
        x: Float, y: Float, text: String, size: Float, w: Float = 0f, rot: Float = 0f, font: FontKind = FontKind.Serif,
        italic: Boolean = font == FontKind.Serif, weight: Int = 400, line: Float = 0f, tracking: Float = 0f,
        color: Long = Tpl.INK, align: TextAlignKind = TextAlignKind.Left, glow: Long? = null,
    ): El.Text = El.Text(nid("text"), x, y, w, 0f, rot, text, font, size, line, weight, italic, align, tracking, color, glow).also { els += it }

    fun spot(
        x: Float, y: Float, w: Float, h: Float, rot: Float = 0f, radius: Float = 0f, pastel: Long? = null, label: String? = null,
        shape: com.framelab.app.data.model.SpotShape = com.framelab.app.data.model.SpotShape.Original,
    ): El.Spot {
        val p = pastel ?: Tpl.PASTELS[spotN % Tpl.PASTELS.size]
        spotN++
        return El.Spot(nid("spot"), x, y, w, h, rot, radius, shape = shape, pastel = p, label = label).also { els += it }
    }

    /** Perforated postage stamp with a photo inset ([inset] on every side). */
    fun stamp(x: Float, y: Float, w: Float, h: Float, rot: Float = 0f, inset: Float = 16f, value: String? = null, pastel: Long? = null): El.Spot {
        box(x, y, w, h, rot, Fill.Solid(0xFFFFFDF8L), edge = com.framelab.app.data.model.Edge.Perforated, shadow = Shadow(0f, 2f, 4f, 0x381E283CL))
        val k = nested(x, y, w, h, rot, inset, inset, w - 2 * inset, h - 2 * inset)
        val sp = spot(k[0], k[1], k[2], k[3], k[4], pastel = pastel)
        if (value != null) textIn(x, y, w, h, rot, w - inset - 34f, h - inset - 26f, value, 21f, font = FontKind.Mono, italic = false, weight = 500, line = 26f, color = Tpl.FL_INK)
        return sp
    }

    /** Postmark: ring + three wavy cancellation lines. */
    fun postmark(x: Float, y: Float, d: Float, color: Long = 0x80283450L) {
        box(x, y, d, d, border = color, borderW = 1.5f, oval = true)
        val wave = { yy: Float -> listOf(0f, yy, 0.17f, yy - 0.15f, 0.33f, yy, 0.5f, yy - 0.15f, 0.67f, yy, 0.83f, yy - 0.15f, 1f, yy) }
        lines(x + d * 0.45f, y + d * 0.2f, d * 1.1f, d * 0.55f, color, 1.5f, wave(0.2f), wave(0.5f), wave(0.8f))
    }

    fun sticker(x: Float, y: Float, w: Float, h: Float, art: String, rot: Float = 0f): El.Sticker =
        El.Sticker(nid("st"), x, y, w, h, rot, art).also { els += it }

    fun lines(x: Float, y: Float, w: Float, h: Float, color: Long, stroke: Float, vararg paths: List<Float>) =
        El.Lines(nid("ln"), x, y, w, h, 0f, paths.toList(), color, stroke).also { els += it }

    /** Tape strip (masking tape, 90% tape colour with light stripes). */
    fun tape(x: Float, y: Float, w: Float, h: Float, rot: Float, striped: Boolean = false, color: Long = Tpl.TAPE) = box(
        x, y, w, h, rot,
        fill = if (striped) Fill.Stripes(color, Tpl.TAPE_STRIPE, 3f, 4f) else Fill.Solid(color),
        polygon = if (striped) listOf(0.02f, 0f, 0.98f, 0.06f, 1f, 0.5f, 0.97f, 1f, 0.01f, 0.94f, 0f, 0.5f) else null,
    )

    /**
     * Polaroid / print: white (or [paper]) frame with a photo spot inset and an optional caption below,
     * all rotated together.
     */
    fun print(
        x: Float, y: Float, w: Float, h: Float, rot: Float,
        inset: Float, photoW: Float, photoH: Float,
        caption: String? = null, captionTop: Float = 0f, captionSize: Float = 13f,
        paper: Long = Tpl.WHITE, shadow: Shadow = Tpl.PRINT_SHADOW, spotRadius: Float = 0f, pastel: Long? = null,
    ): El.Spot {
        box(x, y, w, h, rot, Fill.Solid(paper), shadow = shadow)
        val p = nested(x, y, w, h, rot, inset, inset, photoW, photoH)
        val s = spot(p[0], p[1], p[2], p[3], p[4], spotRadius, pastel)
        if (caption != null) {
            val c = nested(x, y, w, h, rot, 0f, captionTop, w, captionSize + 3f)
            text(c[0], c[1], caption, captionSize, w = w, rot = c[4], line = captionSize + 3f, color = Tpl.INK_SOFT, align = TextAlignKind.Center)
        }
        return s
    }

    /** Text placed in a rotated parent's local coordinates. */
    fun textIn(
        px: Float, py: Float, pw: Float, ph: Float, prot: Float, lx: Float, ly: Float, text: String, size: Float,
        w: Float = 0f, font: FontKind = FontKind.Serif, italic: Boolean = font == FontKind.Serif, weight: Int = 400,
        line: Float = 0f, tracking: Float = 0f, color: Long = Tpl.INK,
    ): El.Text {
        val estW = if (w > 0f) w else text.length * size * 0.6f
        val estH = if (line > 0f) line else size * 1.2f
        val c = nested(px, py, pw, ph, prot, lx, ly, estW, estH)
        return text(c[0], c[1], text, size, w, c[4], font, italic, weight, line, tracking, color)
    }

    fun build(templateId: String, name: String, slides: Int, background: Long, ratio: Ratio = Ratio.Portrait) =
        Design(templateId, name, ratio, slides, background, background, els.toList())
}

fun design(id: String, name: String, slides: Int, background: Long, ratio: Ratio = Ratio.Portrait, block: DesignBuilder.() -> Unit): Design =
    DesignBuilder(id).apply(block).build(id, name, slides, background, ratio)

/** CSS clip-path polygon helper: percentages → fractions. */
fun poly(vararg pct: Float): List<Float> = pct.map { it / 100f }

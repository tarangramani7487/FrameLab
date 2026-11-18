package com.htr.collagemaker.data.catalog

import com.htr.collagemaker.data.model.Design
import com.htr.collagemaker.data.model.Fill
import com.htr.collagemaker.data.model.FontKind
import com.htr.collagemaker.data.model.Shadow
import com.htr.collagemaker.data.model.SpotShape
import com.htr.collagemaker.data.model.nested

// Frame art transcribed from design-source/Card.dc.html (fpolaroid, ffilm, fstamp, flab, ftorn, camera).
private const val FRAME_BG = 0xFFEFEAE2L
private const val LINE = 0xFFCFC6B8L
private const val FILM_DARK = 0xFF14110EL
private const val LAB_DATE = 0xFF9C8D74L
private const val TORN_PAPER = 0xFFFFFDF8L
private val FRAME_SHADOW = Shadow(0f, 3f * U / 2f, 8f, 0x33281C0FL)

/** Polaroid — Frame · 1 photo. */
fun polaroidFrame(): Design = design("frame-polaroid", "Polaroid", 1, FRAME_BG) {
    val x = 20 * U; val y = 20 * U; val w = 88 * U; val h = 106 * U; val r = -3f
    box(x, y, w, h, r, Fill.Solid(Tpl.WHITE), shadow = FRAME_SHADOW)
    val p = nested(x, y, w, h, r, 6 * U, 6 * U, 76 * U, 76 * U); spot(p[0], p[1], p[2], p[3], p[4])
    val l = nested(x, y, w, h, r, 10 * U, 90 * U, 46 * U, 1 * U); box(l[0], l[1], l[2], l[3], l[4], Fill.Solid(LINE))
}

/** Cream lab print — Frame · 1 photo. */
fun labPrintFrame(): Design = design("frame-lab-print", "Cream lab print", 1, FRAME_BG) {
    val x = 20 * U; val y = 20 * U; val w = 88 * U; val h = 116 * U; val r = -2f
    box(x, y, w, h, r, Fill.Solid(Tpl.LAB_PRINT), shadow = FRAME_SHADOW)
    val p = nested(x, y, w, h, r, 7 * U, 7 * U, 74 * U, 90 * U); spot(p[0], p[1], p[2], p[3], p[4])
    textIn(x, y, w, h, r, w - 8 * U - 50f, 102 * U, "26.11", 6 * U, font = FontKind.Mono, weight = 500, line = 8 * U, color = LAB_DATE)
}

/** Torn edge — Frame · 1 photo. */
fun tornEdgeFrame(): Design = design("frame-torn-edge", "Torn edge", 1, FRAME_BG) {
    box(14 * U, 18 * U, 100 * U, 124 * U, fill = Fill.Solid(TORN_PAPER), shadow = Shadow(0f, 2f, 5f, 0x26281C0FL),
        polygon = poly(0f, 3f, 8f, 0f, 17f, 2f, 26f, 0f, 35f, 3f, 44f, 1f, 53f, 3f, 62f, 0f, 71f, 2f, 80f, 0f, 90f, 3f, 100f, 1f, 98f, 12f, 100f, 24f, 97f, 36f, 100f, 48f, 98f, 60f, 100f, 72f, 97f, 84f, 100f, 96f, 92f, 100f, 82f, 97f, 72f, 100f, 62f, 98f, 52f, 100f, 42f, 97f, 32f, 100f, 22f, 98f, 12f, 100f, 2f, 97f, 0f, 86f, 3f, 74f, 0f, 62f, 2f, 50f, 0f, 38f, 3f, 26f, 0f, 14f))
    spot(24 * U, 28 * U, 80 * U, 104 * U)
}

/** 35 mm film — Frame · 1 photo. */
fun film35Frame(): Design = design("frame-35mm", "35 mm film", 1, FRAME_BG) {
    val x = 16 * U; val y = 16 * U; val w = 96 * U; val h = 128 * U
    box(x, y, w, h, fill = Fill.Solid(FILM_DARK), radius = 3 * U)
    box(x + 5 * U, y + 6 * U, 6 * U, h - 12 * U, fill = Fill.Stripes(FILM_DARK, FRAME_BG, 6 * U, 5 * U, vertical = true))
    box(x + w - 11 * U, y + 6 * U, 6 * U, h - 12 * U, fill = Fill.Stripes(FILM_DARK, FRAME_BG, 6 * U, 5 * U, vertical = true))
    spot(x + 16 * U, y + 12 * U, 64 * U, 102 * U)
    text(x + 17 * U, y + 117 * U, "24A", 6 * U, font = FontKind.Mono, italic = false, weight = 500, line = 8 * U, color = Tpl.DATE)
}

/** Digicam — Frame · 2 photos (camera screen + print). Colours from the Camera Roll card. */
fun digicamFrame(): Design = design("frame-digicam", "Digicam", 1, FRAME_BG) {
    print(26 * U, 96 * U, 76 * U, 56 * U, -6f, 4 * U, 68 * U, 48 * U, shadow = Shadow(0f, 2f, 5f, 0x2E3C1E28L))
    val x = 12 * U; val y = 24 * U
    box(x, y, 104 * U, 74 * U, fill = Fill.Linear(listOf(0xFFF0F1F4L, 0xFFB8BBC3L)), radius = 12 * U, shadow = Shadow(0f, 3f, 8f, 0x383C1E28L))
    box(x + 8 * U, y + 9 * U, 62 * U, 50 * U, fill = Fill.Solid(0xFF1E1E22L), radius = 4 * U)
    spot(x + 11 * U, y + 12 * U, 56 * U, 44 * U)
    box(x + 78 * U, y + 12 * U, 18 * U, 18 * U, fill = Fill.Solid(0xFF2E323CL), oval = true, border = 0xFFD9DBE0L, borderW = 5f)
    box(x + 80 * U, y + 40 * U, 14 * U, 5 * U, fill = Fill.Solid(0xFF9A9EA8L), radius = 3 * U)
    box(x + 80 * U, y + 50 * U, 14 * U, 5 * U, fill = Fill.Solid(0xFF9A9EA8L), radius = 3 * U)
}

/** Postage stamp — Frame · 1 photo. */
fun stampFrame(): Design = design("frame-postage-stamp", "Postage stamp", 1, FRAME_BG) {
    stamp(24 * U, 28 * U, 80 * U, 100 * U, 3f, 7 * U)
}

/** Arch window — Frame · 1 photo. */
fun archFrame(): Design = design("frame-arch", "Arch window", 1, FRAME_BG) {
    box(56f, 46f, 230f, 316f, fill = Fill.Solid(Tpl.PAPER), radius = 0f, shadow = FRAME_SHADOW,
        polygon = null)
    spot(71f, 61f, 200f, 286f, shape = SpotShape.Arch)
    text(56f, 374f, "golden hour", 18f, w = 230f, line = 22f, color = Tpl.INK_SOFT, align = com.htr.collagemaker.data.model.TextAlignKind.Center)
}

/** Rounded print — Frame · 1 photo. */
fun roundedFrame(): Design = design("frame-rounded", "Rounded print", 1, FRAME_BG) {
    box(48f, 52f, 246f, 300f, 2f, Fill.Solid(Tpl.WHITE), radius = 24f, shadow = FRAME_SHADOW)
    val p = nested(48f, 52f, 246f, 300f, 2f, 14f, 14f, 218f, 236f)
    spot(p[0], p[1], p[2], p[3], p[4], shape = SpotShape.Rounded)
    text(64f, 300f, "SOFT CORNERS · '26", 10f, font = FontKind.Mono, italic = false, weight = 500, line = 14f, tracking = 1.4f, color = Tpl.LABEL, rot = 2f)
}

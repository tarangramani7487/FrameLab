package com.htr.collagemaker.data.catalog

import com.htr.collagemaker.data.model.Design
import com.htr.collagemaker.data.model.Fill
import com.htr.collagemaker.data.model.FontKind
import com.htr.collagemaker.data.model.Shadow

// Hexes below are taken literally from design-source/Card.dc.html and Main.dc.html (template art).
private const val FILM_BG = 0xFF2A2420L
private const val FILM_BAND = 0xFF14110EL
private const val FILM_HOLE = 0xFFEFE6D6L
private const val FILM_TITLE = 0xFFF3EDE3L
private const val POLA_BG = 0xFFEDE3D3L
private const val STAMP_BG = 0xFFEAF1F4L
private const val SHORE_BG = 0xFFF3EDE3L
private const val HERO_FRAME = 0xFF2A2520L
private const val HERO_HOLE = 0x99FAF7F2L

/**
 * Film Reel — 6 slides · 9 photos. A continuous film strip runs through slides 1–4 (frames cross slide edges);
 * slides 5–6 close the roll.
 */
fun filmReel(id: String = "film-reel", name: String = "Film Reel", title: String = "film reel", bg: Long = FILM_BG): Design =
    design(id, name, slides = 6, background = bg) {
        val bandY = 30 * U; val bandH = 102 * U
        box(0f, bandY, 1360f, bandH, fill = Fill.Solid(FILM_BAND))
        box(0f, 34 * U, 1360f, 6 * U, fill = Fill.Sprockets(FILM_BAND, FILM_HOLE, 6 * U, 6 * U, 12 * U))
        box(0f, 122 * U, 1360f, 6 * U, fill = Fill.Sprockets(FILM_BAND, FILM_HOLE, 6 * U, 6 * U, 12 * U))
        for (k in 0 until 9) {
            val x = -20 * U + k * 58 * U
            spot(x, 46 * U, 52 * U, 70 * U)
            if (k >= 3) text(x + 4f, 352f + 12f, "${k + 1}A", 13f, font = FontKind.Mono, italic = false, weight = 500, line = 16f, tracking = 1f, color = Tpl.DATE)
        }
        text(10 * U, 7 * U, title, 14 * U, line = 16 * U, color = FILM_TITLE)
        text(10 * U, 140 * U, "FL 400 · 24 EXP", 7 * U, font = FontKind.Mono, italic = false, weight = 500, line = 10 * U, tracking = 1 * U, color = Tpl.DATE)
        // Film tail into slide 5.
        box(1360f, bandY + 24f, 60f, bandH - 48f, fill = Fill.Solid(FILM_BAND), polygon = poly(0f, 0f, 100f, 18f, 100f, 82f, 0f, 100f))
        // Slides 5–6: closing card.
        text(1400f, 60f, "roll 01", 64f, line = 66f, tracking = -1.5f, color = FILM_TITLE)
        text(1404f, 140f, "24 EXP · DEVELOPED '26 11 17", 11f, font = FontKind.Mono, italic = false, weight = 500, line = 16f, tracking = 1.4f, color = Tpl.DATE)
        text(1400f, 200f, "every frame from the weekend, in the order it happened", 22f, w = 270f, line = 28f, color = FILM_TITLE)
        box(1760f, 70f, 250f, 300f, 3f, Fill.Solid(FILM_BAND), radius = 6f)
        box(1772f, 82f, 18f, 276f, 3f, Fill.Stripes(FILM_BAND, FILM_HOLE, 14f, 16f, vertical = true, inset = 2f))
        box(1980f, 82f, 18f, 276f, 3f, Fill.Stripes(FILM_BAND, FILM_HOLE, 14f, 16f, vertical = true, inset = 2f))
        text(1800f, 150f, "that's\na wrap", 46f, line = 48f, tracking = -1f, color = FILM_TITLE, rot = 3f)
        text(1806f, 300f, "SAVE · TAG · COMMENT", 10f, font = FontKind.Mono, italic = false, weight = 500, line = 14f, tracking = 1.6f, color = Tpl.DATE, rot = 3f)
        text(1732f, 396f, "@yourhandle", 10f, font = FontKind.Mono, italic = false, weight = 500, line = 14f, tracking = 1f, color = FILM_TITLE)
    }

/** Film Roll — 3 slides · 1 photo. One photo seen through three film frames (Home hero "One photo, three slides."). */
fun filmRoll(id: String = "film-roll", name: String = "Film Roll"): Design =
    design(id, name, slides = 3, background = Tpl.FL_INK) {
        spot(14f, 70f, 998f, 288f, label = "Photo · slides 1–3")
        // Film frame overlay with sprockets.
        box(0f, 0f, 1026f, 70f, fill = Fill.Solid(HERO_FRAME))
        box(0f, 26f, 1026f, 16f, fill = Fill.Sprockets(HERO_FRAME, HERO_HOLE, 14f, 14f, 30f))
        box(0f, 358f, 1026f, 70f, fill = Fill.Solid(HERO_FRAME))
        box(0f, 384f, 1026f, 16f, fill = Fill.Sprockets(HERO_FRAME, HERO_HOLE, 14f, 14f, 30f))
        box(0f, 70f, 14f, 288f, fill = Fill.Solid(HERO_FRAME))
        box(1012f, 70f, 14f, 288f, fill = Fill.Solid(HERO_FRAME))
        box(328f, 70f, 28f, 288f, fill = Fill.Solid(HERO_FRAME))
        box(670f, 70f, 28f, 288f, fill = Fill.Solid(HERO_FRAME))
        listOf("24A", "25A", "26A").forEachIndexed { i, n ->
            text(i * 342f + 24f, 406f, n, 12f, font = FontKind.Mono, italic = false, weight = 500, line = 16f, tracking = 1f, color = Tpl.DATE)
        }
        text(24f, 2f, "FL 400 · ONE PHOTO, THREE SLIDES", 10f, font = FontKind.Mono, italic = false, weight = 500, line = 22f, tracking = 1.4f, color = Tpl.DATE)
        text(870f, 406f, "'26 11 17", 12f, font = FontKind.Mono, italic = false, weight = 500, line = 16f, tracking = 1f, color = Tpl.DATE, glow = 0xB3FF7828L)
    }

/** Polaroid Pile — 6 slides · 11 photos. */
fun polaroidPile(id: String = "polaroid-pile", name: String = "Polaroid Pile", bg: Long = POLA_BG, captions: List<String> = POLA_CAPTIONS): Design =
    design(id, name, slides = 6, background = bg) {
        val sh = Shadow(0f, 2f * U / 2f, 5f, 0x38281C0FL)
        // Slide 1 (transcribed from the card).
        print(8 * U, 14 * U, 64 * U, 76 * U, -8f, 5 * U, 54 * U, 54 * U, shadow = sh)
        print(56 * U, 30 * U, 64 * U, 76 * U, 7f, 5 * U, 54 * U, 54 * U, shadow = sh)
        print(28 * U, 74 * U, 64 * U, 76 * U, -2f, 5 * U, 54 * U, 54 * U, captions[0], captionTop = 61 * U, captionSize = 8 * U, shadow = sh)
        tape(48 * U, 68 * U, 30 * U, 9 * U, -14f)
        // Slide 2.
        print(372f, 40f, 190f, 226f, 6f, 15f, 160f, 160f, captions[1], captionTop = 184f, captionSize = 18f, shadow = sh)
        print(478f, 186f, 190f, 226f, -5f, 15f, 160f, 160f, captions[2], captionTop = 184f, captionSize = 18f, shadow = sh)
        tape(520f, 176f, 70f, 20f, 8f)
        // Slide 3 + a print crossing into slide 4.
        print(716f, 56f, 180f, 214f, -7f, 14f, 152f, 152f, captions[3], captionTop = 174f, captionSize = 18f, shadow = sh)
        print(934f, 160f, 200f, 238f, 5f, 15f, 170f, 170f, captions[4], captionTop = 196f, captionSize = 18f, shadow = sh)
        tape(990f, 150f, 80f, 22f, -6f)
        text(712f, 340f, "swipe, it keeps going", 20f, w = 200f, line = 24f, color = Tpl.INK_SOFT)
        // Slide 4.
        print(1160f, 36f, 180f, 214f, 8f, 14f, 152f, 152f, captions[5], captionTop = 174f, captionSize = 18f, shadow = sh)
        // Slide 5.
        print(1392f, 70f, 180f, 214f, -4f, 14f, 152f, 152f, captions[6], captionTop = 174f, captionSize = 18f, shadow = sh)
        print(1508f, 196f, 180f, 214f, 6f, 14f, 152f, 152f, captions[7], captionTop = 174f, captionSize = 18f, shadow = sh)
        tape(1440f, 62f, 66f, 20f, -12f)
        // Slide 6.
        print(1758f, 60f, 228f, 270f, -3f, 16f, 196f, 196f, captions[8], captionTop = 222f, captionSize = 20f, shadow = sh)
        tape(1830f, 50f, 84f, 22f, 4f)
        text(1744f, 360f, captions[9], 24f, w = 280f, line = 28f, color = Tpl.INK)
        text(1746f, 398f, "@yourhandle", 10f, font = FontKind.Mono, italic = false, weight = 500, line = 14f, tracking = 1f, color = Tpl.LABEL)
    }

private val POLA_CAPTIONS = listOf(
    "us, again", "golden hour", "the long way home", "sunday", "between slides",
    "same again", "late", "later", "us, again", "see you next weekend",
)

/** Clean Grid — 1 slide · 4 photos (4:5). */
fun cleanGrid(id: String = "clean-grid", name: String = "Clean Grid", bg: Long = Tpl.WHITE): Design =
    design(id, name, slides = 1, background = bg) {
        val pad = 8 * U; val gap = 4 * U
        val cw = (342f - 2 * pad - gap) / 2f
        val ch = (428f - 2 * pad - gap) / 2f
        for (r in 0..1) for (c in 0..1) spot(pad + c * (cw + gap), pad + r * (ch + gap), cw, ch)
    }

/** Stamp Collection — 6 slides · 10 photos (Pro). */
fun stampCollection(id: String = "stamp-collection", name: String = "Stamp Collection", bg: Long = STAMP_BG): Design =
    design(id, name, slides = 6, background = bg) {
        stamp(18 * U, 30 * U, 70 * U, 90 * U, 0f, 6 * U, "26c")
        stamp(74 * U, 86 * U, 50 * U, 60 * U, 8f, 6 * U)
        postmark(62 * U, 12 * U, 44 * U)
        // Slide 2.
        stamp(380f, 60f, 170f, 214f, -4f, 16f, "12c")
        stamp(540f, 170f, 130f, 160f, 6f, 14f)
        text(376f, 360f, "AIR MAIL · PAR AVION", 11f, font = FontKind.Mono, italic = false, weight = 500, line = 16f, tracking = 1.6f, color = Tpl.FL_INK)
        // Slide 3 + one stamp crossing into slide 4.
        stamp(718f, 40f, 196f, 246f, 3f, 16f, "40c")
        text(720f, 330f, "wish you were here", 28f, w = 220f, line = 32f, color = Tpl.INK)
        stamp(942f, 186f, 150f, 190f, -5f, 14f)
        // Slide 4.
        stamp(1150f, 56f, 190f, 240f, 4f, 16f, "55c")
        postmark(1210f, 300f, 90f)
        // Slide 5.
        stamp(1398f, 86f, 150f, 190f, -6f, 14f)
        stamp(1540f, 200f, 150f, 190f, 5f, 14f, "9c")
        // Slide 6.
        stamp(1756f, 50f, 220f, 280f, -2f, 18f, "1€")
        postmark(1900f, 18f, 100f)
        text(1744f, 356f, "collected, not posted", 24f, w = 280f, line = 28f, color = Tpl.INK)
        text(1746f, 396f, "@yourhandle", 10f, font = FontKind.Mono, italic = false, weight = 500, line = 14f, tracking = 1f, color = Tpl.LABEL)
    }

/** Shoreline Prints — 5 slides · 8 photos. */
fun shorelinePrints(id: String = "shoreline-prints", name: String = "Shoreline Prints", title: String = "shoreline", bg: Long = SHORE_BG): Design =
    design(id, name, slides = 5, background = bg) {
        val sh = Shadow(0f, 2f * U / 2f, 6f, 0x33281C0FL)
        text(10 * U, 6 * U, title, 12 * U, line = 14 * U, color = Tpl.INK_SOFT)
        print(8 * U, 26 * U, 72 * U, 90 * U, -5f, 5 * U, 62 * U, 80 * U, shadow = sh)
        print(48 * U, 56 * U, 72 * U, 90 * U, 6f, 5 * U, 62 * U, 80 * U, shadow = sh)
        // Slide 2.
        text(370f, 22f, "ROLL 02 · LOW TIDE", 11f, font = FontKind.Mono, italic = false, weight = 500, line = 16f, tracking = 1.4f, color = Tpl.LABEL)
        print(368f, 54f, 192f, 240f, 4f, 13f, 166f, 214f, shadow = sh)
        print(478f, 168f, 192f, 240f, -6f, 13f, 166f, 214f, shadow = sh)
        // Wide print crossing slides 3 → 4.
        print(756f, 84f, 440f, 300f, -2f, 14f, 412f, 236f, "low tide, high spirits", captionTop = 262f, captionSize = 20f, shadow = sh)
        // Slide 4.
        print(1222f, 36f, 136f, 172f, 7f, 10f, 116f, 146f, shadow = sh)
        print(1232f, 232f, 124f, 158f, -5f, 10f, 104f, 132f, shadow = sh)
        // Slide 5.
        print(1416f, 52f, 246f, 306f, -3f, 14f, 218f, 248f, "until the next tide", captionTop = 270f, captionSize = 20f, shadow = sh)
        text(1400f, 396f, "SHORELINE PRINTS · 05", 10f, font = FontKind.Mono, italic = false, weight = 500, line = 14f, tracking = 1.4f, color = Tpl.LABEL)
    }

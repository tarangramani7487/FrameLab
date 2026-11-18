package com.framelab.app.data.catalog

import com.framelab.app.data.model.Design
import com.framelab.app.data.model.Fill
import com.framelab.app.data.model.FontKind
import com.framelab.app.data.model.Ratio
import com.framelab.app.data.model.Shadow
import com.framelab.app.data.model.SpotShape
import com.framelab.app.data.model.TextAlignKind
import com.framelab.app.data.model.nested

// Literal hexes from design-source/Card.dc.html template art + colors.xml swatches.
private const val NAVY = 0xFF0F1E2EL
private const val MIST = 0xFFEAF1F4L
private const val BLUSH = 0xFFFFE8ECL
private const val CREAM = 0xFFF3EDE3L
private const val PAPER = 0xFFFAF7F2L
private const val JOURNAL = 0xFFF6EFE2L
private const val JOURNAL_RULE = 0x24786046L
private const val CAMERA_LABEL = 0xFF8A4B58L
private const val CAMERA_TOP = 0xFFF0F1F4L
private const val CAMERA_BOTTOM = 0xFFB8BBC3L
private const val CAMERA_SCREEN = 0xFF1E1E22L
private const val CAMERA_LENS = 0xFF2E323CL
private const val CAMERA_RING = 0xFFD9DBE0L
private const val CAMERA_KEY = 0xFF9A9EA8L
private const val MUTED = 0xFF6F685EL
private const val DASH = 0xFFCFC6B8L

private fun DesignBuilder.mono(x: Float, y: Float, t: String, size: Float = 10f, color: Long = Tpl.LABEL, tracking: Float = 1.4f, rot: Float = 0f) =
    text(x, y, t, size, font = FontKind.Mono, italic = false, weight = 500, line = size * 1.4f, tracking = tracking, color = color, rot = rot)

private fun DesignBuilder.serif(x: Float, y: Float, t: String, size: Float, w: Float = 0f, color: Long = Tpl.INK, rot: Float = 0f) =
    text(x, y, t, size, w = w, line = size * 1.15f, tracking = if (size > 30f) -0.8f else 0f, color = color, rot = rot)

private fun DesignBuilder.sans(x: Float, y: Float, t: String, size: Float, color: Long, weight: Int = 800, w: Float = 0f, tracking: Float = -1f) =
    text(x, y, t, size, w = w, font = FontKind.Sans, italic = false, weight = weight, line = size * 0.98f, tracking = tracking, color = color)

private val SOFT = Shadow(0f, 3f, 7f, 0x33281C0FL)
private val FLASH = Shadow(0f, 8f, 16f, 0x73000000L)

/** Torn paper sheet with a ragged top edge. */
private fun DesignBuilder.tornSheet(x: Float, y: Float, w: Float, h: Float, rot: Float = 0f) = box(
    x, y, w, h, rot, Fill.Ruled(Tpl.PAPER, Tpl.RULE, 18f),
    polygon = poly(0f, 3f, 6f, 0f, 12f, 2f, 19f, 0f, 26f, 3f, 33f, 1f, 40f, 3f, 47f, 0f, 54f, 2f, 61f, 0f, 68f, 3f, 75f, 1f, 82f, 3f, 89f, 0f, 95f, 2f, 100f, 1f, 100f, 100f, 0f, 100f),
    shadow = Tpl.PAPER_SHADOW,
)

/** Booth Night — 5 slides · 8 photos (photo-booth strips on navy). */
fun boothNight(id: String = "booth-night", name: String = "Booth Night"): Design = design(id, name, slides = 5, background = NAVY) {
    fun strip(x: Float, y: Float, rot: Float) {
        box(x, y, 124f, 372f, rot, Fill.Solid(Tpl.WHITE), shadow = FLASH)
        for (k in 0 until 3) {
            val p = nested(x, y, 124f, 372f, rot, 10f, 10f + k * 118f, 104f, 108f)
            spot(p[0], p[1], p[2], p[3], p[4])
        }
    }
    serif(24f, 30f, "booth\nnight", 44f, color = MIST)
    mono(26f, 150f, "PHOTOBOOTH · 4 POSES", color = Tpl.DATE)
    strip(186f, 36f, 4f)
    print(372f, 46f, 272f, 330f, -3f, 10f, 252f, 290f, paper = Tpl.WHITE, shadow = FLASH)
    mono(560f, 350f, "'26 11 17", 12f, color = Tpl.DATE, rot = -3f)
    strip(716f, 30f, -3f)
    serif(866f, 260f, "flash on,\nalways", 30f, color = MIST)
    print(1058f, 52f, 270f, 320f, 4f, 10f, 250f, 280f, shadow = FLASH)
    serif(1394f, 120f, "same time next week?", 34f, w = 280f, color = MIST)
    mono(1396f, 300f, "SAVE · TAG · COMMENT", 10f, color = Tpl.DATE, tracking = 1.6f)
    mono(1396f, 396f, "@yourhandle", 10f, color = MIST)
}

/** Postcard — 4 slides · 6 photos. The first postcard crosses slides 1 → 2. */
fun postcard(id: String = "postcard", name: String = "Postcard"): Design = design(id, name, slides = 4, background = CREAM) {
    mono(24f, 34f, "GREETINGS FROM SOMEWHERE WARM", 10f)
    box(28f, 84f, 620f, 300f, -2f, Fill.Solid(Tpl.WHITE), shadow = SOFT)
    val p = nested(28f, 84f, 620f, 300f, -2f, 14f, 14f, 360f, 272f)
    spot(p[0], p[1], p[2], p[3], p[4], label = "Photo · slides 1–2")
    textIn(28f, 84f, 620f, 300f, -2f, 392f, 28f, "greetings\nfrom", 26f, line = 28f)
    textIn(28f, 84f, 620f, 300f, -2f, 394f, 92f, "THE COAST · '26", 10f, font = FontKind.Mono, weight = 500, line = 14f, tracking = 1.4f, color = Tpl.LABEL)
    for (i in 0..3) box(422f, 200f + i * 22f, 150f, 1f, -2f, Fill.Solid(Tpl.RULE))
    stamp(560f, 102f, 64f, 78f, -2f, 8f)
    // Slide 3.
    print(700f, 40f, 200f, 250f, -4f, 12f, 176f, 196f, "from nowhere", captionTop = 216f, captionSize = 16f, shadow = SOFT)
    print(842f, 176f, 170f, 214f, 5f, 12f, 146f, 160f, shadow = SOFT)
    // Slide 4.
    print(1048f, 44f, 290f, 240f, 3f, 12f, 266f, 186f, "wish you were here", captionTop = 206f, captionSize = 18f, shadow = SOFT)
    stamp(1174f, 286f, 120f, 140f, -6f, 12f, "20c")
    postmark(1080f, 310f, 80f)
}

/** Travel Journal — 6 slides · 10 photos (ruled journal, boarding pass, tickets). */
fun travelJournal(id: String = "travel-journal", name: String = "Travel Journal"): Design = design(id, name, slides = 6, background = JOURNAL) {
    box(0f, 0f, 2052f, 428f, fill = Fill.Ruled(JOURNAL, JOURNAL_RULE, 12 * U))
    // Slide 1 (from the card).
    print(12 * U, 14 * U, 104 * U, 74 * U, -2f, 3 * U, 98 * U, 68 * U, shadow = SOFT)
    tape(44 * U, 8 * U, 40 * U, 11 * U, -4f)
    val bx = 10 * U; val by = 98 * U; val bw = 108 * U; val bh = 46 * U; val br = 1.5f
    box(bx, by, bw, bh, br, Fill.Solid(Tpl.WHITE), radius = 4 * U, shadow = Shadow(0f, 1f, 3f, 0x2E281C0FL))
    textIn(bx, by, bw, bh, br, 8 * U, 7 * U, "BOARDING PASS", 6 * U, font = FontKind.Mono, weight = 500, line = 8 * U, tracking = 0.8f * U, color = MUTED)
    textIn(bx, by, bw, bh, br, 8 * U, 16 * U, "BOM · LIS", 14 * U, font = FontKind.Sans, weight = 700, line = 16 * U, color = Tpl.FL_INK)
    textIn(bx, by, bw, bh, br, 8 * U, 34 * U, "GATE 12 · SEAT 4A", 6 * U, font = FontKind.Mono, weight = 500, line = 8 * U, color = MUTED)
    val d = nested(bx, by, bw, bh, br, 80 * U, 4 * U, 1.5f, 38 * U)
    box(d[0], d[1], 1f, d[3], d[4], border = DASH, borderW = 1f, dashed = true)
    val bc = nested(bx, by, bw, bh, br, 86 * U, 10 * U, 16 * U, 26 * U)
    box(bc[0], bc[1], bc[2], bc[3], bc[4], Fill.Stripes(Tpl.WHITE, Tpl.FL_INK, 1 * U, 2 * U))
    // Slide 2.
    print(366f, 40f, 190f, 150f, -3f, 8f, 174f, 134f, shadow = SOFT)
    print(560f, 30f, 100f, 120f, 8f, 6f, 88f, 88f, shadow = SOFT)
    serif(368f, 210f, "day 01", 30f)
    mono(370f, 250f, "LISBON · TRAM 28", 10f)
    print(470f, 200f, 190f, 190f, 4f, 8f, 174f, 150f, "pastel de nata", captionTop = 164f, captionSize = 14f, shadow = SOFT)
    // Slide 3.
    print(708f, 46f, 150f, 190f, -5f, 8f, 134f, 150f, "miradouro", captionTop = 164f, captionSize = 14f, shadow = SOFT)
    print(862f, 60f, 150f, 190f, 6f, 8f, 134f, 150f, shadow = SOFT)
    box(720f, 290f, 210f, 88f, 2f, Fill.Solid(Tpl.WHITE), radius = 8f, shadow = SOFT)
    textIn(720f, 290f, 210f, 88f, 2f, 14f, 12f, "TICKET · ADMIT ONE", 9f, font = FontKind.Mono, weight = 500, line = 12f, tracking = 1.2f, color = MUTED)
    textIn(720f, 290f, 210f, 88f, 2f, 14f, 32f, "TRAM 28", 26f, font = FontKind.Sans, weight = 700, line = 30f, color = Tpl.FL_INK)
    // Slides 4–5: wide print crossing the edge.
    print(1050f, 40f, 520f, 300f, -1f, 10f, 500f, 250f, "the view from the top", captionTop = 266f, captionSize = 18f, shadow = SOFT)
    tape(1250f, 28f, 90f, 24f, 2f)
    print(1586f, 200f, 118f, 150f, 6f, 7f, 104f, 112f, shadow = SOFT)
    mono(1400f, 380f, "DAY 03 · ALFAMA → BELÉM", 10f)
    // Slide 6.
    print(1728f, 40f, 170f, 210f, -4f, 8f, 154f, 170f, shadow = SOFT)
    print(1870f, 150f, 160f, 200f, 5f, 8f, 144f, 160f, shadow = SOFT)
    serif(1730f, 366f, "home, but different", 24f)
    mono(1732f, 400f, "@yourhandle", 9f)
}

/** Night Flash — 6 slides · 6 photos (on-camera flash prints on navy). */
fun nightFlash(id: String = "night-flash", name: String = "Night Flash"): Design = design(id, name, slides = 6, background = NAVY) {
    val rots = listOf(-3f, 4f, -2f, 3f, -4f, 2f)
    for (i in 0 until 6) {
        val x = i * 342f + 18 * U; val y = 18 * U; val r = rots[i]
        box(x, y, 92 * U, 114 * U, r, Fill.Solid(Tpl.WHITE), shadow = FLASH)
        val p = nested(x, y, 92 * U, 114 * U, r, 4 * U, 4 * U, 84 * U, 106 * U)
        spot(p[0], p[1], p[2], p[3], p[4])
        textIn(x, y, 92 * U, 114 * U, r, 92 * U - 9 * U - 92f, 114 * U - 8 * U - 26f, "'26 11 17", 6 * U, font = FontKind.Mono, weight = 500, line = 10 * U, color = Tpl.DATE)
        mono(i * 342f + 10 * U, 142 * U, if (i == 0) "NIGHT FLASH" else "NIGHT FLASH · 0${i + 1}/06", 7 * U, color = MIST, tracking = 1.4f * U)
    }
}

/** Camera Roll — 5 slides · 7 photos (digicam + prints on blush). */
fun cameraRoll(id: String = "camera-roll", name: String = "Camera Roll"): Design = design(id, name, slides = 5, background = BLUSH) {
    fun camera(x: Float, y: Float, w: Float, rot: Float) {
        val k = w / (104 * U)
        val h = 74 * U * k
        box(x, y, w, h, rot, Fill.Linear(listOf(CAMERA_TOP, CAMERA_BOTTOM)), radius = 12 * U * k, shadow = Shadow(0f, 3f, 8f, 0x383C1E28L))
        val sc = nested(x, y, w, h, rot, 8 * U * k, 9 * U * k, 62 * U * k, 50 * U * k)
        box(sc[0], sc[1], sc[2], sc[3], sc[4], Fill.Solid(CAMERA_SCREEN), radius = 4 * U * k)
        val sp = nested(x, y, w, h, rot, 11 * U * k, 12 * U * k, 56 * U * k, 44 * U * k)
        spot(sp[0], sp[1], sp[2], sp[3], sp[4])
        val l = nested(x, y, w, h, rot, 78 * U * k, 12 * U * k, 18 * U * k, 18 * U * k)
        box(l[0], l[1], l[2], l[3], l[4], Fill.Solid(CAMERA_LENS), oval = true, border = CAMERA_RING, borderW = 5f * k)
        for (yy in listOf(40f, 50f)) {
            val b = nested(x, y, w, h, rot, 80 * U * k, yy * U * k, 14 * U * k, 5 * U * k)
            box(b[0], b[1], b[2], b[3], b[4], Fill.Solid(CAMERA_KEY), radius = 3 * U * k)
        }
    }
    mono(9 * U, 8 * U, "CAMERA ROLL", 7 * U, color = CAMERA_LABEL, tracking = 1 * U)
    print(26 * U, 96 * U, 76 * U, 56 * U, -6f, 4 * U, 68 * U, 48 * U, shadow = Shadow(0f, 2f, 5f, 0x2E3C1E28L))
    camera(12 * U, 24 * U, 104 * U, 0f)
    // Slide 2.
    print(370f, 40f, 180f, 220f, -5f, 10f, 160f, 170f, "date night", captionTop = 186f, captionSize = 16f, shadow = SOFT)
    print(492f, 196f, 170f, 200f, 6f, 10f, 150f, 150f, shadow = SOFT)
    // Slides 3–4.
    print(740f, 70f, 470f, 290f, -2f, 12f, 446f, 236f, "the whole night in one frame", captionTop = 256f, captionSize = 18f, shadow = SOFT)
    mono(1240f, 380f, "IMG_0412.JPG", 10f, color = CAMERA_LABEL)
    print(1236f, 40f, 116f, 140f, 6f, 7f, 102f, 104f, shadow = SOFT)
    // Slide 5.
    camera(1396f, 110f, 286f, -4f)
    serif(1400f, 330f, "more on the camera roll", 24f, w = 280f, color = Tpl.INK)
}

/** Big Type — 5 slides · 6 photos. The headline runs across the slides: SUMMER WAS TOO SHORT. */
fun bigType(id: String = "big-type", name: String = "Big Type"): Design = design(id, name, slides = 5, background = CREAM) {
    val words = listOf("SUMMER", "WAS", "TOO", "SHORT")
    words.forEachIndexed { i, wd ->
        val x = i * 342f
        spot(x + 8 * U, 8 * U, 112 * U, 96 * U)
        sans(x + 7 * U, 112 * U, wd, 64f, Tpl.FL_INK, tracking = -3f)
        mono(x + 9 * U, 144 * U, "VOL. 03 · 2026 · 0${i + 1}", 7 * U, color = MUTED, tracking = 1 * U)
    }
    spot(1368f + 21f, 21f, 146f, 200f)
    spot(1368f + 175f, 21f, 146f, 200f)
    sans(1368f + 20f, 250f, "SEE YOU\nNEXT YEAR", 48f, Tpl.FL_INK, tracking = -2f)
    mono(1368f + 22f, 384f, "@yourhandle", 10f, color = MUTED)
}

/** Frame Mix — 5 slides · 9 photos (circle, arch, prints). */
fun frameMix(id: String = "frame-mix", name: String = "Frame Mix"): Design = design(id, name, slides = 5, background = PAPER) {
    spot(10 * U, 14 * U, 58 * U, 58 * U, shape = SpotShape.Circle)
    spot(74 * U, 10 * U, 44 * U, 70 * U, shape = SpotShape.Arch)
    print(26 * U, 84 * U, 72 * U, 64 * U, -5f, 4 * U, 64 * U, 56 * U, shadow = Shadow(0f, 2f, 6f, 0x2E281C0FL))
    // Slide 2.
    spot(370f, 40f, 140f, 220f, shape = SpotShape.Arch)
    spot(528f, 196f, 130f, 130f, shape = SpotShape.Circle)
    serif(372f, 290f, "shapes of\nthe weekend", 26f)
    // Slide 3.
    spot(704f, 40f, 300f, 200f, shape = SpotShape.Rounded)
    print(760f, 262f, 170f, 140f, 4f, 9f, 152f, 104f, shadow = SOFT)
    // Slides 4–5: a circle that crosses the edge.
    mono(1046f, 40f, "FRAME MIX · 04", 10f)
    serif(1046f, 64f, "round\nthe corner", 34f)
    spot(1258f, 104f, 220f, 220f, shape = SpotShape.Circle, label = "Photo · slides 4–5")
    spot(1520f, 60f, 160f, 260f, shape = SpotShape.Arch)
    mono(1520f, 360f, "@yourhandle", 10f)
}

/** Coffee Run — 5 slides · 9 photos (torn paper, food). */
fun coffeeRun(id: String = "coffee-run", name: String = "Coffee Run"): Design = design(id, name, slides = 5, background = Tpl.KRAFT) {
    spot(210f, 0f, 280f, 428f, label = "Background · slides 1–2")
    val px = 16f; val py = 46f; val pw = 196f; val ph = 330f; val pr = -2f
    box(px, py, pw, ph, pr, Fill.Ruled(Tpl.PAPER, Tpl.RULE, 18f, marginX = 20f, margin = Tpl.MARGIN),
        polygon = poly(0f, 0f, 93f, 0f, 97f, 4f, 94f, 9f, 99f, 14f, 95f, 19f, 98f, 25f, 94f, 30f, 97f, 36f, 93f, 41f, 98f, 47f, 95f, 52f, 99f, 58f, 94f, 63f, 97f, 69f, 93f, 74f, 98f, 80f, 95f, 85f, 99f, 91f, 94f, 96f, 97f, 100f, 0f, 100f),
        shadow = Tpl.PAPER_SHADOW)
    textIn(px, py, pw, ph, pr, 26f, 22f, "coffee\nrun", 44f, line = 42f, tracking = -1.5f)
    textIn(px, py, pw, ph, pr, 28f, 116f, "STOP 01 · 7:58 AM", 9f, font = FontKind.Mono, weight = 500, line = 12f, tracking = 1.4f, color = Tpl.LABEL)
    print(40f, 228f, 120f, 140f, 4f, 7f, 106f, 106f, "first sip", captionTop = 116f, captionSize = 13f)
    tape(60f, 34f, 76f, 22f, -6f, striped = true)
    // Slide 2.
    box(500f, 0f, 184f, 428f, fill = Fill.Ruled(Tpl.PAPER, Tpl.RULE, 18f),
        polygon = poly(4f, 0f, 100f, 0f, 100f, 100f, 5f, 100f, 1f, 96f, 6f, 91f, 2f, 86f, 6f, 81f, 1f, 76f, 5f, 71f, 2f, 66f, 7f, 61f, 2f, 56f, 6f, 51f, 1f, 46f, 5f, 41f, 2f, 36f, 7f, 31f, 1f, 26f, 5f, 21f, 2f, 16f, 6f, 11f, 1f, 6f),
        shadow = Shadow(-2f, 0f, 3f, 0x33281C0FL))
    print(522f, 40f, 140f, 166f, -4f, 8f, 124f, 124f, "flat white", captionTop = 138f, captionSize = 13f)
    serif(524f, 236f, "9:04 am", 28f)
    mono(526f, 276f, "OAT · EXTRA HOT\nNO LID", 9f)
    // Slide 3.
    print(700f, 40f, 150f, 176f, -6f, 8f, 134f, 134f, "the queue", captionTop = 150f, captionSize = 13f)
    print(852f, 170f, 150f, 176f, 5f, 8f, 134f, 134f, "worth it", captionTop = 150f, captionSize = 13f)
    tape(744f, 32f, 60f, 18f, -10f)
    serif(706f, 370f, "two shots, one bad decision", 15f, color = Tpl.INK_SOFT)
    // Slide 4.
    spot(1026f, 170f, 342f, 258f)
    box(1026f, 0f, 342f, 190f, fill = Fill.Ruled(Tpl.PAPER, Tpl.RULE, 18f),
        polygon = poly(0f, 0f, 100f, 0f, 100f, 88f, 95f, 92f, 90f, 87f, 84f, 93f, 78f, 88f, 72f, 94f, 66f, 89f, 60f, 95f, 54f, 90f, 48f, 96f, 42f, 90f, 36f, 95f, 30f, 89f, 24f, 94f, 18f, 88f, 12f, 93f, 6f, 88f, 0f, 92f),
        shadow = Tpl.PAPER_SHADOW)
    serif(1046f, 22f, "stop 02", 32f)
    mono(1048f, 66f, "CROISSANT — ESPRESSO — REPEAT", 9f)
    print(1252f, 30f, 92f, 92f, 4f, 5f, 82f, 82f)
    // Slide 5.
    print(1390f, 56f, 150f, 176f, -4f, 8f, 134f, 134f, "last sip", captionTop = 150f, captionSize = 13f)
    print(1528f, 178f, 160f, 190f, 6f, 8f, 144f, 146f, "see you tomorrow", captionTop = 162f, captionSize = 13f)
    mono(1392f, 398f, "@yourhandle", 9f)
}

/** Square Duo — 1 slide · 2 photos (1:1). */
fun squareDuo(id: String = "square-duo", name: String = "Square Duo"): Design =
    design(id, name, slides = 1, background = PAPER, ratio = Ratio.Square) {
        spot(16f, 16f, 151f, 266f)
        spot(175f, 16f, 151f, 266f)
        mono(16f, 302f, "TWO OF US · '26", 10f)
        text(176f, 296f, "then & now", 22f, w = 150f, line = 26f, color = Tpl.INK, align = TextAlignKind.Right)
    }

/** Story Strip — 3 slides · 5 photos (9:16). */
fun storyStrip(id: String = "story-strip", name: String = "Story Strip"): Design =
    design(id, name, slides = 3, background = Tpl.WHITE, ratio = Ratio.Story) {
        spot(24f, 24f, 294f, 420f)
        serif(24f, 466f, "48 hours in lisbon", 32f, w = 294f)
        mono(26f, 566f, "STORY 01/03 · SWIPE →", 10f)
        spot(366f, 24f, 500f, 300f, label = "Photo · slides 2–3")
        spot(366f, 350f, 294f, 234f)
        spot(882f, 24f, 136f, 300f)
        spot(708f, 350f, 294f, 234f)
    }

/** Birthday Confetti — 3 slides · 5 photos. */
fun birthdayConfetti(id: String = "birthday-confetti", name: String = "Birthday Confetti"): Design =
    design(id, name, slides = 3, background = BLUSH) {
        val r = java.util.Random(7)
        val dots = longArrayOf(Tpl.PASTELS[0], Tpl.PASTELS[1], Tpl.PASTELS[2], Tpl.PASTELS[3], Tpl.PASTELS[4], Tpl.DATE)
        repeat(46) {
            val s = 6f + r.nextFloat() * 8f
            box(r.nextFloat() * 1010f, r.nextFloat() * 410f, s, s * (if (it % 3 == 0) 0.45f else 1f), r.nextFloat() * 90f, Fill.Solid(dots[it % dots.size]), oval = it % 3 != 0)
        }
        spot(60f, 40f, 222f, 270f, shape = SpotShape.Arch)
        serif(40f, 326f, "it's my\nbirthday", 40f)
        print(370f, 40f, 170f, 206f, -5f, 10f, 150f, 150f, "cake first", captionTop = 168f, captionSize = 16f, shadow = SOFT)
        print(488f, 196f, 170f, 206f, 6f, 10f, 150f, 150f, "then dancing", captionTop = 168f, captionSize = 16f, shadow = SOFT)
        spot(712f, 40f, 170f, 170f, shape = SpotShape.Circle)
        print(840f, 150f, 160f, 196f, 4f, 10f, 140f, 146f, shadow = SOFT)
        serif(712f, 360f, "another trip around the sun", 22f, w = 280f)
    }

/** Gym Log — 3 slides · 4 photos (dark). */
fun gymLog(id: String = "gym-log", name: String = "Gym Log"): Design = design(id, name, slides = 3, background = Tpl.FL_INK) {
    spot(0f, 0f, 342f, 300f)
    mono(20f, 316f, "WEEK 12 · LOG", 10f, color = Tpl.DATE)
    sans(18f, 338f, "PR DAY", 64f, Tpl.WHITE)
    spot(362f, 20f, 302f, 190f)
    mono(364f, 220f, "SET 01 · 5 × 5", 10f, color = Tpl.DATE)
    spot(362f, 238f, 302f, 170f)
    spot(704f, 20f, 302f, 300f)
    sans(704f, 336f, "consistency > intensity", 26f, Tpl.WHITE, weight = 700, w = 300f, tracking = -0.5f)
    mono(706f, 398f, "@yourhandle", 10f, color = MIST)
}

/** Menu Board — 5 slides · 7 photos (food). */
fun menuBoard(id: String = "menu-board", name: String = "Menu Board"): Design = design(id, name, slides = 5, background = CREAM) {
    spot(24f, 24f, 294f, 260f)
    serif(24f, 300f, "sunday\nbrunch", 40f)
    mono(26f, 396f, "COURSE 01 / 05", 10f)
    print(366f, 36f, 150f, 190f, -4f, 8f, 134f, 150f, "eggs, obviously", captionTop = 164f, captionSize = 13f, shadow = SOFT)
    print(512f, 180f, 150f, 190f, 5f, 8f, 134f, 150f, "the good bread", captionTop = 164f, captionSize = 13f, shadow = SOFT)
    mono(368f, 396f, "COURSE 02 / 05", 10f)
    spot(730f, 40f, 560f, 300f, label = "Photo · slides 3–4")
    mono(712f, 396f, "COURSE 03 / 05", 10f)
    print(1296f, 196f, 64f, 78f, 6f, 5f, 54f, 54f)
    mono(1046f, 396f, "COURSE 04 / 05", 10f)
    spot(1390f, 30f, 146f, 180f, radius = 10f)
    spot(1544f, 30f, 146f, 180f, radius = 10f)
    serif(1390f, 240f, "full, happy,\nsleepy", 34f)
    mono(1392f, 396f, "COURSE 05 / 05 · @yourhandle", 10f)
}

/** Torn Paper Stories variants. */
fun summerDiary() = tornDiary(
    "summer-diary", "Summer Diary",
    DiaryCopy(kicker = "VOL. 01 · LONG WEEKENDS", sub = "iced coffee, sea air & no alarms", c1 = "lazy sunday", outro = "save this for the next long weekend", date = "'26 07 19"),
    bg = CREAM,
).let { d -> d.copy(elements = d.elements.map { if (it is com.framelab.app.data.model.El.Spot && it.label != null) it.copy(pastel = Tpl.PASTELS[1]) else it }) }
fun parisNotes() = tornDiary(
    "paris-notes", "Paris Notes",
    DiaryCopy("paris", "notes", "VOL. 01 · RAIN AND CROISSANTS", "long walks, short naps & too much bread", "first café", "gare du nord", "jour 01",
        "LATE TRAIN\nEARLY CRÊPE\nWALKED IT ALL", "us, soaked", "seine", "the two of us, mostly lost", "jour 04", "MARCHÉ — MUSÉE — METRO", "best croissant of my life",
        "à bientôt", "save this for your next long weekend", "'26 04 12"),
    bg = Tpl.PASTELS[2], dropSmallPrint = true,
)
fun petDiary() = tornDiary(
    "pet-diary", "Pet Diary",
    DiaryCopy("pet", "diary", "VOL. 02 · GOOD BOY DAYS", "zoomies, naps & one stolen sock", "first walk", "home", "day 01",
        "SNIFFED EVERYTHING\nATE EVERYTHING\nSLEPT ANYWAY", "best friends", "park day", "the two of us, mostly muddy", "day 04", "PARK — BATH — TREATS",
        "best stick of his life", "same time tomorrow", "save this for every good boy", "'26 03 09"),
    bg = Tpl.PASTELS[4],
)
fun wedding() = tornDiary(
    "wedding", "Wedding",
    DiaryCopy("our", "wedding", "VOL. 01 · THE BIG DAY", "vows, tears & a very long dance floor", "first look", "the venue", "the day",
        "SAID YES\nCRIED TWICE\nDANCED ALL NIGHT", "just married", "first dance", "the two of us, finally", "after", "CAKE — TOASTS — CONFETTI",
        "best day of our lives", "forever", "save this for the anniversary", "'26 06 20"),
    bg = Tpl.PASTELS[5],
)
fun japanNotes() = tornDiary(
    "japan-notes", "Japan Notes",
    DiaryCopy("japan", "notes", "VOL. 04 · TEN DAYS EAST", "trains on time, ramen at midnight", "first temple", "tokyo", "day 01",
        "LANDED LATE\nATE RAMEN\nWALKED SHIBUYA", "us, jet-lagged", "kyoto", "the two of us, mostly on trains", "day 04", "SHRINE — MARKET — ONSEN",
        "best mochi of my life", "mata ne", "save this for your next long trip", "'26 10 03"),
    bg = Tpl.PASTELS[1],
)
fun loveLetters() = polaroidPile(
    "love-letters", "Love Letters", bg = BLUSH,
    captions = listOf("you, again", "first date", "the long way home", "sunday", "always", "same again", "late", "later", "us", "to many more"),
)

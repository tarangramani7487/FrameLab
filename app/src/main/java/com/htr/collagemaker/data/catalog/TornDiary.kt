package com.htr.collagemaker.data.catalog

import com.htr.collagemaker.data.model.Design
import com.htr.collagemaker.data.model.Edge
import com.htr.collagemaker.data.model.Fill
import com.htr.collagemaker.data.model.FontKind

/** Torn Diary — 6 slides · 12 photos · 4:5. Transcribed from design-source/Diary.dc.html. */
/** Copy for a Torn Diary variant (Torn Paper Stories row). */
data class DiaryCopy(
    val l1: String = "summer",
    val l2: String = "diary",
    val kicker: String = "VOL. 03 · THE SLOW DAYS",
    val sub: String = "salt hair, late lunches & zero plans",
    val c1: String = "first swim",
    val c2: String = "arrival",
    val day1: String = "day 01",
    val day1Notes: String = "ARRIVED LATE\nATE EARLY\nSWAM ANYWAY",
    val c3: String = "us, sunburnt",
    val c4: String = "boat day",
    val line3: String = "the two of us, mostly sunburnt",
    val day4: String = "day 04",
    val day4Kicker: String = "MARKET — BEACH — NOODLES",
    val day4Line: String = "best mango of my life",
    val c5: String = "until next year",
    val outro: String = "save this for your next slow summer",
    val date: String = "'26 11 17",
)

fun tornDiary(
    id: String = "torn-diary", name: String = "Torn Diary", copy: DiaryCopy = DiaryCopy(), bg: Long = Tpl.KRAFT,
    /** Drop the small print on slide 2 (11-photo variants). */
    dropSmallPrint: Boolean = false,
): Design =
    design(id, name, slides = 6, background = bg) {
        val l1 = copy.l1; val l2 = copy.l2
        // ── Slides 1–2: background photo A spans both slides.
        spot(124f, 0f, 368f, 428f, pastel = 0xFFF3CFC3L, label = "Background · slides 1–2")

        // Torn diary page (rotated −2°), ruled with red margin.
        val px = 14f; val py = 50f; val pw = 182f; val ph = 336f; val pr = -2f
        box(
            px, py, pw, ph, pr, Fill.Ruled(Tpl.PAPER, Tpl.RULE, 18f, marginX = 20f, margin = Tpl.MARGIN),
            polygon = poly(0f, 0f, 93f, 0f, 97f, 3f, 94f, 7f, 99f, 11f, 95f, 15f, 98f, 20f, 94f, 24f, 97f, 29f, 93f, 33f, 98f, 38f, 95f, 42f, 99f, 47f, 94f, 51f, 97f, 56f, 93f, 60f, 98f, 65f, 95f, 69f, 99f, 74f, 94f, 78f, 97f, 83f, 93f, 87f, 98f, 92f, 95f, 96f, 97f, 100f, 0f, 100f),
            shadow = Tpl.PAPER_SHADOW,
        )
        textIn(px, py, pw, ph, pr, 26f, 24f, "$l1\n$l2", 46f, line = 42f, tracking = -1.5f)
        textIn(px, py, pw, ph, pr, 28f, 118f, copy.kicker, 9f, w = 140f, font = FontKind.Mono, weight = 500, line = 12f, tracking = 1.4f, color = Tpl.LABEL)
        textIn(px, py, pw, ph, pr, 28f, 140f, copy.sub, 13f, w = 132f, line = 18f, color = Tpl.INK_SOFT)
        tape(58f, 38f, 76f, 22f, -7f, striped = true)

        // Polaroid B copy.c1.
        print(44f, 214f, 112f, 132f, 5f, 7f, 98f, 98f, "first swim", captionTop = 110f, pastel = 0xFFCDE7EAL)

        // Swipe chip + date stamp.
        box(252f, 20f, 70f, 26f, fill = Fill.Solid(Tpl.WHITE), radius = 13f, shadow = com.htr.collagemaker.data.model.Shadow(0f, 2f, 6f, 0x26000000L))
        text(264f, 26f, "swipe", 11f, font = FontKind.Sans, italic = false, weight = 600, line = 14f, color = Tpl.FL_INK)
        lines(298f, 28f, 12f, 10f, Tpl.FL_INK, 1.75f, listOf(0f, 0.5f, 1f, 0.5f), listOf(0.55f, 0f, 1f, 0.5f, 0.55f, 1f))
        text(256f, 398f, copy.date, 14f, font = FontKind.Mono, italic = false, weight = 500, line = 16f, tracking = 1f, color = Tpl.DATE, glow = 0xB3FF7828L)

        // ── Slide 2 right half: torn paper strip.
        box(
            486f, 0f, 198f, 428f, fill = Fill.Ruled(Tpl.PAPER, Tpl.RULE, 18f),
            polygon = poly(4f, 0f, 100f, 0f, 100f, 100f, 5f, 100f, 1f, 97f, 6f, 93f, 2f, 89f, 6f, 85f, 1f, 81f, 5f, 77f, 2f, 73f, 7f, 69f, 2f, 65f, 6f, 61f, 1f, 57f, 5f, 53f, 2f, 49f, 7f, 45f, 1f, 41f, 5f, 37f, 2f, 33f, 6f, 29f, 1f, 25f, 5f, 21f, 2f, 17f, 7f, 13f, 1f, 9f, 5f, 5f),
            shadow = com.htr.collagemaker.data.model.Shadow(-2f, 0f, 3f, 0x33281C0FL),
        )
        print(512f, 34f, 138f, 160f, -4f, 8f, 122f, 118f, copy.c2, captionTop = 132f, pastel = 0xFFF6E1C3L)
        text(514f, 212f, copy.day1, 30f, line = 34f, tracking = -0.5f)
        text(516f, 250f, copy.day1Notes, 9f, font = FontKind.Mono, italic = false, weight = 500, line = 17f, tracking = 1.2f, color = Tpl.LABEL)
        if (!dropSmallPrint) {
            print(574f, 312f, 96f, 96f, 6f, 5f, 86f, 86f, pastel = 0xFFF6D3DCL)
            tape(600f, 302f, 46f, 15f, 3f)
        }

        // ── Slide 3: ruled sheet with two prints.
        box(
            698f, 22f, 314f, 386f, -1f, Fill.Ruled(Tpl.PAPER, Tpl.RULE, 18f),
            polygon = poly(0f, 3f, 6f, 0f, 12f, 2f, 19f, 0f, 26f, 3f, 33f, 1f, 40f, 3f, 47f, 0f, 54f, 2f, 61f, 0f, 68f, 3f, 75f, 1f, 82f, 3f, 89f, 0f, 95f, 2f, 100f, 1f, 100f, 100f, 0f, 100f),
            shadow = com.htr.collagemaker.data.model.Shadow(0f, 3f, 4f, 0x33281C0FL),
        )
        print(716f, 64f, 150f, 176f, -6f, 8f, 134f, 134f, copy.c3, captionTop = 150f, pastel = 0xFFE3E0F3L)
        print(852f, 168f, 150f, 176f, 5f, 8f, 134f, 134f, copy.c4, captionTop = 150f, pastel = 0xFFD3E6F2L)
        tape(758f, 56f, 60f, 18f, -10f)
        tape(900f, 160f, 60f, 18f, 8f)
        text(714f, 362f, copy.line3, 15f, line = 20f, color = Tpl.INK_SOFT)

        // ── Slide 4: photo G under a torn header.
        spot(1026f, 156f, 342f, 272f, pastel = 0xFFE9DCF0L)
        box(
            1026f, 0f, 342f, 178f, fill = Fill.Ruled(Tpl.PAPER, Tpl.RULE, 18f),
            polygon = poly(0f, 0f, 100f, 0f, 100f, 88f, 95f, 92f, 90f, 87f, 84f, 93f, 78f, 88f, 72f, 94f, 66f, 89f, 60f, 95f, 54f, 90f, 48f, 96f, 42f, 90f, 36f, 95f, 30f, 89f, 24f, 94f, 18f, 88f, 12f, 93f, 6f, 88f, 0f, 92f),
            shadow = Tpl.PAPER_SHADOW,
        )
        text(1046f, 22f, copy.day4, 32f, line = 36f, tracking = -0.5f)
        text(1048f, 66f, copy.day4Kicker, 9f, font = FontKind.Mono, italic = false, weight = 500, line = 12f, tracking = 1.2f, color = Tpl.LABEL)
        text(1046f, 88f, copy.day4Line, 14f, line = 18f, color = Tpl.INK_SOFT)
        print(1252f, 30f, 92f, 92f, 4f, 5f, 82f, 82f, pastel = 0xFFE2EED3L)
        tape(1244f, 26f, 40f, 14f, -38f)

        // ── Slides 5–6: photo I spans both.
        spot(1368f, 0f, 470f, 428f, pastel = 0xFFCFE6ECL)
        print(1390f, 246f, 118f, 148f, -5f, 8f, 102f, 114f, paper = Tpl.LAB_PRINT, pastel = 0xFFE7DDEBL)
        text(1480f, 372f, "26.11", 8f, font = FontKind.Mono, italic = false, weight = 500, line = 10f, color = 0xFF9C8D74L, rot = -5f)
        // Postage stamp with perforated edge + postmark.
        box(1572f, 36f, 100f, 120f, 6f, Fill.Solid(0xFFFFFDF8L), edge = Edge.Perforated, shadow = com.htr.collagemaker.data.model.Shadow(0f, 2f, 4f, 0x4D1E283CL))
        val k = com.htr.collagemaker.data.model.nested(1572f, 36f, 100f, 120f, 6f, 6f, 6f, 88f, 108f)
        spot(k[0], k[1], k[2], k[3], k[4], pastel = 0xFFD7EBDDL)
        box(1630f, 16f, 64f, 64f, border = 0x8C28283CL, borderW = 1.5f, oval = true)
        lines(1660f, 30f, 70f, 36f, 0x8C28283CL, 1.5f,
            listOf(0f, 0.2f, 0.17f, 0.05f, 0.33f, 0.2f, 0.5f, 0.05f, 0.67f, 0.2f, 0.83f, 0.05f, 1f, 0.2f),
            listOf(0f, 0.5f, 0.17f, 0.35f, 0.33f, 0.5f, 0.5f, 0.35f, 0.67f, 0.5f, 0.83f, 0.35f, 1f, 0.5f),
            listOf(0f, 0.8f, 0.17f, 0.65f, 0.33f, 0.8f, 0.5f, 0.65f, 0.67f, 0.8f, 0.83f, 0.65f, 1f, 0.8f),
        )
        print(1868f, 30f, 150f, 176f, 4f, 8f, 134f, 134f, copy.c5, captionTop = 150f, pastel = 0xFFF8E2C2L)
        box(
            1710f, 246f, 342f, 182f, fill = Fill.Ruled(Tpl.PAPER, Tpl.RULE, 18f),
            polygon = poly(0f, 8f, 5f, 3f, 10f, 9f, 16f, 2f, 22f, 8f, 28f, 3f, 34f, 9f, 40f, 2f, 46f, 8f, 52f, 3f, 58f, 9f, 64f, 2f, 70f, 8f, 76f, 3f, 82f, 9f, 88f, 2f, 94f, 8f, 100f, 3f, 100f, 100f, 0f, 100f),
            shadow = com.htr.collagemaker.data.model.Shadow(0f, -2f, 4f, 0x33281C0FL),
        )
        text(1730f, 280f, copy.outro, 22f, w = 250f, line = 26f, tracking = -0.3f)
        text(1732f, 362f, "SAVE · TAG · COMMENT", 10f, font = FontKind.Mono, italic = false, weight = 500, line = 12f, tracking = 1.6f, color = Tpl.INK)
        text(1732f, 386f, "@yourhandle", 9f, font = FontKind.Mono, italic = false, weight = 500, line = 12f, tracking = 1f, color = Tpl.LABEL)
    }

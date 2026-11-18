package com.framelab.app.data.catalog

import com.framelab.app.data.model.Design
import com.framelab.app.data.model.Template
import com.framelab.app.data.model.TemplateKind

/**
 * Built-in templates and frames (HANDOFF §8.3 fallback). Category ids, totals and subtitles mirror the designs;
 * totals are the sample counts from the mockups (HANDOFF §10) and never smaller than the real list.
 */
object BuiltInTemplates {
    private fun t(
        design: Design, category: String, style: String, topic: String, uses: Int, order: Int,
        isNew: Boolean = false, isPro: Boolean = false, made: Boolean = false,
    ) = Template(meta(design.templateId, design.name, category, style, topic, isNew, isPro, made, uses, order), design)

    private fun f(design: Design, category: String, style: String, uses: Int, order: Int, isNew: Boolean = false) =
        Template(meta(design.templateId, design.name, category, style, "Travel", isNew, false, true, uses, order, TemplateKind.Frame), design)

    fun all(): List<Template> = listOf(
        // Instagram Trending (FrameLab originals first).
        t(tornDiary(), "instagram-trending", "Torn paper", "Travel", 980, 30, isNew = true, made = true),
        t(filmReel(), "instagram-trending", "Film", "Travel", 940, 29, isNew = true, made = true),
        t(polaroidPile(), "instagram-trending", "Polaroid", "Love", 910, 18),
        t(travelJournal(), "travel", "Torn paper", "Travel", 880, 22),
        t(shorelinePrints(), "travel", "Polaroid", "Travel", 820, 17),
        t(nightFlash(), "instagram-trending", "Dark", "Birthday", 790, 21),
        t(cameraRoll(), "instagram-trending", "Film", "Love", 760, 20),
        t(bigType(), "instagram-trending", "Minimal", "Travel", 700, 19),
        t(frameMix(), "instagram-trending", "Minimal", "Love", 640, 16),
        t(filmRoll(), "framelab-originals", "Film", "Travel", 860, 28, isNew = true, made = true),
        t(boothNight(), "framelab-originals", "Dark", "Birthday", 610, 27, isNew = true, made = true),
        t(postcard(), "travel", "Stamp", "Travel", 580, 15),
        t(stampCollection(), "travel", "Stamp", "Travel", 560, 14, isPro = true),
        t(cleanGrid(), "clean-minimal", "Minimal", "Food", 540, 6),
        // Torn Paper Stories.
        t(summerDiary(), "torn-paper-stories", "Torn paper", "Travel", 720, 26, isNew = true, made = true),
        t(parisNotes(), "torn-paper-stories", "Torn paper", "Travel", 680, 13),
        t(coffeeRun(), "torn-paper-stories", "Torn paper", "Food", 520, 12),
        t(petDiary(), "torn-paper-stories", "Torn paper", "Pets", 500, 11),
        t(wedding(), "torn-paper-stories", "Torn paper", "Love", 480, 10, isPro = true),
        t(japanNotes(), "torn-paper-stories", "Torn paper", "Travel", 460, 9),
        // Celebrations / minimal.
        t(birthdayConfetti(), "celebrations", "Minimal", "Birthday", 430, 25, isNew = true, made = true),
        t(loveLetters(), "celebrations", "Polaroid", "Love", 400, 8, isPro = true),
        t(menuBoard(), "celebrations", "Minimal", "Food", 380, 7),
        t(gymLog(), "clean-minimal", "Dark", "Fitness", 360, 24, isNew = true, made = true),
        t(squareDuo(), "clean-minimal", "Minimal", "Love", 340, 5),
        t(storyStrip(), "clean-minimal", "Minimal", "Travel", 320, 23),
    )

    fun frames(): List<Template> = listOf(
        f(polaroidFrame(), "prints-polaroids", "Polaroid", 900, 8),
        f(labPrintFrame(), "prints-polaroids", "Polaroid", 820, 7),
        f(tornEdgeFrame(), "prints-polaroids", "Torn paper", 780, 6),
        f(roundedFrame(), "prints-polaroids", "Minimal", 500, 3),
        f(archFrame(), "prints-polaroids", "Minimal", 450, 2),
        f(film35Frame(), "film-cameras", "Film", 870, 10, isNew = true),
        f(digicamFrame(), "film-cameras", "Film", 760, 9, isNew = true),
        f(stampFrame(), "film-cameras", "Stamp", 640, 5),
    )

    private fun ids(all: List<Template>, vararg id: String) = id.filter { x -> all.any { it.id == x } }

    fun categories(t: List<Template>): List<Category> = listOf(
        Category("instagram-trending", "Instagram Trending", isNew = true,
            templateIds = ids(t, "torn-diary", "film-reel", "polaroid-pile", "travel-journal", "shoreline-prints", "night-flash", "camera-roll", "big-type", "frame-mix", "booth-night"),
            total = 10, sortOrder = 0, subtitle = "10 templates · what's working right now"),
        Category("torn-paper-stories", "Torn Paper Stories",
            templateIds = ids(t, "summer-diary", "paris-notes", "coffee-run", "pet-diary", "wedding", "japan-notes", "torn-diary"),
            total = 40, sortOrder = 1, subtitle = "40 templates · diary pages, torn edges"),
        Category("travel", "Travel",
            templateIds = ids(t, "travel-journal", "postcard", "shoreline-prints", "story-strip", "japan-notes", "paris-notes", "stamp-collection", "film-roll"),
            total = 12, sortOrder = 2, subtitle = "12 templates · trips, postcards, journals"),
        Category("celebrations", "Celebrations",
            templateIds = ids(t, "birthday-confetti", "booth-night", "wedding", "love-letters", "menu-board", "night-flash"),
            total = 14, sortOrder = 3, subtitle = "14 templates · birthdays, weddings, nights out"),
        Category("clean-minimal", "Clean & Minimal",
            templateIds = ids(t, "clean-grid", "square-duo", "story-strip", "big-type", "gym-log", "frame-mix"),
            total = 18, sortOrder = 4, subtitle = "18 templates · grids, white space, big type"),
        // Shown as the built-in row when the backend can't be reached (H3: "FrameLab Originals · BUILT-IN").
        Category("framelab-originals", "FrameLab Originals",
            templateIds = t.filter { it.meta.framelabMade }.sortedByDescending { it.meta.createdOrder }.map { it.id } +
                ids(t, "postcard", "clean-grid", "stamp-collection", "polaroid-pile"),
            total = 26, sortOrder = 5, subtitle = "26 templates · built into the app"),
    ).map { it.copy(total = maxOf(it.total, it.templateIds.size)) }

    fun frameCategories(f: List<Template>): List<Category> = listOf(
        Category("prints-polaroids", "Prints & Polaroids",
            templateIds = ids(f, "frame-polaroid", "frame-lab-print", "frame-torn-edge", "frame-rounded", "frame-arch"),
            total = 12, sortOrder = 0, subtitle = "12 frames · polaroids, lab prints, torn edges"),
        Category("film-cameras", "Film & cameras", isNew = true,
            templateIds = ids(f, "frame-35mm", "frame-digicam", "frame-postage-stamp"),
            total = 9, sortOrder = 1, subtitle = "9 frames · film, digicams, stamps"),
    ).map { it.copy(total = maxOf(it.total, it.templateIds.size)) }
}

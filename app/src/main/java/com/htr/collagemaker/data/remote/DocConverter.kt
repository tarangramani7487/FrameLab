package com.htr.collagemaker.data.remote

import com.htr.collagemaker.data.catalog.Category
import com.htr.collagemaker.data.catalog.Tpl
import com.htr.collagemaker.data.catalog.meta
import com.htr.collagemaker.data.model.Design
import com.htr.collagemaker.data.model.El
import com.htr.collagemaker.data.model.Fill
import com.htr.collagemaker.data.model.FontKind
import com.htr.collagemaker.data.model.Ratio
import com.htr.collagemaker.data.model.SLIDE_W
import com.htr.collagemaker.data.model.Template
import com.htr.collagemaker.data.model.TextAlignKind
import com.htr.collagemaker.data.model.nested
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull

/** Turns Supabase rows into the app's catalogue (Home rows + templates). */
object RemoteCatalog {

    data class Result(val templates: List<Template>, val categories: List<Category>)

    fun build(rows: List<SbCategory>, templates: List<SbTemplate>): Result {
        // Category id → template ids, rows sorted by sort_order (negative = pinned), empty rows hidden (§4.1).
        val cats = rows.sortedBy { it.sortOrder }.map { c ->
            c to c.links.sortedBy { it.sortOrder }.mapNotNull { it.templates?.templateId }.distinct()
        }
        val memberOf = mutableMapOf<String, MutableList<SbCategory>>()
        cats.forEach { (c, ids) -> ids.forEach { memberOf.getOrPut(it) { mutableListOf() } += c } }
        val sizeOf = cats.associate { (c, ids) -> c.id to ids.size }

        // Templates in Home order first, then any that are in no row.
        val order = (cats.flatMap { it.second } + templates.map { it.templateId }).distinct().withIndex().associate { it.value to it.index }
        val converted = templates.sortedBy { order[it.templateId] ?: Int.MAX_VALUE }.mapIndexedNotNull { i, row ->
            val design = row.preview?.let { runCatching { DocConverter.toDesign(row.templateId, row.title, it) }.getOrNull() } ?: return@mapIndexedNotNull null
            val inCats = memberOf[row.templateId].orEmpty()
            // The most specific row (fewest templates) is the template's category.
            val home = inCats.minByOrNull { sizeOf[it.id] ?: Int.MAX_VALUE }
            val titles = inCats.map { it.title }
            Template(
                meta(
                    id = row.templateId, name = row.title,
                    category = home?.let { categoryId(it.id) } ?: "",
                    style = styleFor(titles), topic = topicFor(titles),
                    isPro = row.tier.equals("pro", ignoreCase = true),
                    framelabMade = row.source == null || row.source == "ai",
                    order = templates.size - i, // earlier in Home order = newer
                ),
                design,
            )
        }
        val loaded = converted.map { it.id }.toSet()
        val categories = cats.mapNotNull { (c, ids) ->
            val shown = ids.filter { it in loaded }
            if (shown.isEmpty()) null
            else Category(id = categoryId(c.id), title = c.title, templateIds = shown, total = shown.size, sortOrder = c.sortOrder)
        }
        return Result(converted, categories)
    }

    fun categoryId(id: Long) = "sb-$id"

    /** Explore filter groups (BuiltInCatalog.styles / topics) derived from the rows a template is in. */
    private fun styleFor(titles: List<String>): String {
        val t = titles.joinToString(" ").lowercase()
        return when {
            "torn" in t -> "Torn paper"
            "film" in t -> "Film"
            "instant" in t || "polaroid" in t -> "Polaroid"
            "stationery" in t || "stamp" in t -> "Stamp"
            "clean" in t || "grid" in t || "minimal" in t -> "Minimal"
            else -> ""
        }
    }

    private fun topicFor(titles: List<String>): String {
        val t = titles.joinToString(" ").lowercase()
        return when {
            "travel" in t -> "Travel"
            "wedding" in t || "love" in t -> "Love"
            "birthday" in t || "celebration" in t -> "Birthday"
            "food" in t || "thanksgiving" in t -> "Food"
            "pet" in t -> "Pets"
            "fitness" in t -> "Fitness"
            else -> ""
        }
    }
}

/**
 * Backend `Doc` (design px, nested nodes) → app [Design] (342-unit slides, flat world-space elements).
 * Supports what the live data uses: boxes, text, photo slots, per-corner radii, borders, shadows, opacity
 * and nesting. `imageUrl` / `maskUrl` / `bgImage` nodes are skipped (no live template uses them yet).
 */
object DocConverter {

    fun ratioOf(slideW: Float, slideH: Float): Ratio? {
        val r = slideH / slideW
        return Ratio.entries.firstOrNull { kotlin.math.abs(it.pxH.toFloat() / it.pxW - r) < 0.01f }
    }

    fun toDesign(templateId: String, name: String, doc: SbDoc): Design {
        val ratio = ratioOf(doc.slideW, doc.slideH) ?: error("Unsupported slide size ${doc.slideW}×${doc.slideH}")
        val fx = SLIDE_W / doc.slideW
        val fy = ratio.slideHeight / doc.slideH
        val out = mutableListOf<El>()
        var n = 0
        fun id(kind: String) = "$templateId-$kind-${n++}"

        // World rect in design px: x, y, w, h, rotation.
        fun emit(node: SbNode, world: FloatArray, alpha: Float) {
            val (x, y, w, h, rot) = world.let { arrayOf(it[0] * fx, it[1] * fy, it[2] * fx, it[3] * fy, it[4]) }
            val radii = radiiOf(node.radius, fx)
            val single = radii?.takeIf { r -> r.all { it == r[0] } }?.get(0) ?: 0f
            val multi = radii?.takeIf { r -> r.any { it != r[0] } }
            val bg = node.bgColor?.let(::parseColor)
            val shadow = if (node.shadow) Tpl.PRINT_SHADOW else null
            val border = node.border?.takeIf { it.width > 0f }

            if (node.slotIndex != null) {
                val pastel = Tpl.PASTELS[node.slotIndex.mod(Tpl.PASTELS.size)]
                var sx = x; var sy = y; var sw = w; var sh = h
                if (border != null || shadow != null) {
                    // Frame under the photo (border colour), photo inset by the border width.
                    out += El.Box(id("box"), x, y, w, h, rot, Fill.Solid(border?.let { parseColor(it.color) } ?: bg ?: pastel),
                        radius = single, radii = multi, shadow = shadow, alpha = alpha)
                    if (border != null) {
                        val bw = border.width * fx
                        val k = nested(x, y, w, h, rot, bw, bw, (w - 2 * bw).coerceAtLeast(1f), (h - 2 * bw).coerceAtLeast(1f))
                        sx = k[0]; sy = k[1]; sw = k[2]; sh = k[3]
                    }
                }
                out += El.Spot(id("spot"), sx, sy, sw, sh, rot, radius = single, pastel = pastel, radii = multi, order = node.slotIndex)
            } else {
                if (bg != null || border != null || shadow != null) {
                    out += El.Box(
                        id("box"), x, y, w, h, rot,
                        fill = bg?.let { Fill.Solid(it) }, radius = single, radii = multi,
                        borderColor = border?.let { parseColor(it.color) }, borderWidth = (border?.width ?: 0f) * fx,
                        dashed = border?.style == "dashed" || border?.style == "dotted",
                        shadow = shadow, alpha = alpha,
                    )
                }
                val ts = node.textStyle
                if (!node.text.isNullOrEmpty() && ts != null) {
                    val size = ts.size * fx
                    val color = parseColor(ts.color)
                    out += El.Text(
                        id("text"), x, y, w, h, rot,
                        text = node.text,
                        font = when (ts.font) { "serif" -> FontKind.Serif; "mono" -> FontKind.Mono; else -> FontKind.Sans },
                        size = size,
                        lineHeight = ts.lineHeight?.let { it * size } ?: 0f,
                        weight = weightOf(ts),
                        italic = ts.italic,
                        align = when (ts.align) { "center" -> TextAlignKind.Center; "right" -> TextAlignKind.Right; else -> TextAlignKind.Left },
                        tracking = ts.spacing * fx,
                        color = withAlpha(color, alpha),
                        vAlign = when (ts.valign) { "center" -> 0.5f; "bottom" -> 1f; else -> 0f },
                    )
                }
            }
            // Children are relative to this node's top-left, in its rotated frame.
            for (child in node.children) {
                val c = scaled(child)
                emit(child, nested(world[0], world[1], world[2], world[3], world[4], c[0], c[1], c[2], c[3], c[4]), alpha * child.opacity)
            }
        }

        for (node in doc.root) emit(node, scaled(node), node.opacity)

        val background = doc.bgColor?.let(::parseColor) ?: Tpl.WHITE
        return Design(templateId, name, ratio, doc.slides.coerceAtLeast(1), background, background, out)
    }

    /** Node rect with its own scale applied around the centre. */
    private fun scaled(n: SbNode): FloatArray {
        val s = if (n.scale > 0f) n.scale else 1f
        val w = n.w * s; val h = n.h * s
        return floatArrayOf(n.x + (n.w - w) / 2f, n.y + (n.h - h) / 2f, w, h, n.rotate)
    }

    private fun radiiOf(e: kotlinx.serialization.json.JsonElement?, f: Float): List<Float>? = when (e) {
        is JsonPrimitive -> e.floatOrNull?.takeIf { it > 0f }?.let { r -> List(4) { r * f } }
        is JsonArray -> e.mapNotNull { (it as? JsonPrimitive)?.floatOrNull?.times(f) }.takeIf { it.size == 4 && it.any { r -> r > 0f } }
        else -> null
    }

    private fun weightOf(ts: SbTextStyle): Int =
        ((ts.weight as? JsonPrimitive)?.content?.toIntOrNull() ?: 400).coerceIn(100, 900)

    private fun withAlpha(argb: Long, alpha: Float): Long {
        if (alpha >= 1f) return argb
        val a = (((argb ushr 24) and 0xFF) * alpha.coerceIn(0f, 1f)).toLong()
        return (a shl 24) or (argb and 0xFFFFFF)
    }

    /** "#RGB", "#RRGGBB", "#RRGGBBAA", "rgb(r,g,b)", "rgba(r,g,b,a)", "transparent" → ARGB. */
    fun parseColor(s: String): Long {
        val v = s.trim().lowercase()
        if (v == "transparent") return 0x00000000L
        if (v.startsWith("#")) {
            val hex = v.drop(1)
            return when (hex.length) {
                3 -> 0xFF000000L or hex.map { "$it$it" }.joinToString("").toLong(16)
                6 -> 0xFF000000L or hex.toLong(16)
                8 -> (hex.takeLast(2).toLong(16) shl 24) or hex.take(6).toLong(16)
                else -> 0xFF000000L
            }
        }
        if (v.startsWith("rgb")) {
            val p = v.substringAfter("(").substringBefore(")").split(",").map { it.trim() }
            val r = p.getOrNull(0)?.toFloatOrNull()?.toLong()?.coerceIn(0, 255) ?: 0
            val g = p.getOrNull(1)?.toFloatOrNull()?.toLong()?.coerceIn(0, 255) ?: 0
            val b = p.getOrNull(2)?.toFloatOrNull()?.toLong()?.coerceIn(0, 255) ?: 0
            val a = ((p.getOrNull(3)?.toFloatOrNull() ?: 1f).coerceIn(0f, 1f) * 255f + 0.5f).toLong()
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        return 0xFF000000L
    }
}

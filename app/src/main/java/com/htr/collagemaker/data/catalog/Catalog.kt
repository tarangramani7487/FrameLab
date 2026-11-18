package com.htr.collagemaker.data.catalog

import com.htr.collagemaker.data.model.Template
import com.htr.collagemaker.data.model.TemplateKind
import com.htr.collagemaker.data.model.TemplateMeta

/** A Home row (Supabase `template_categories`, ordered by `sort_order`). */
data class Category(
    val id: String,
    val title: String,
    val isNew: Boolean = false,
    val templateIds: List<String>,
    /** Count shown next to "See all" (the full category size in the backend). */
    val total: Int = templateIds.size,
    val sortOrder: Int = 0,
    val subtitle: String = "",
)

/** Built-in catalogue — used offline and as the fallback when the backend is unreachable (HANDOFF §8.3). */
object BuiltInCatalog {
    val templates: List<Template> by lazy { BuiltInTemplates.all() }
    val frames: List<Template> by lazy { BuiltInTemplates.frames() }
    val categories: List<Category> by lazy { BuiltInTemplates.categories(templates) }
    val frameCategories: List<Category> by lazy { BuiltInTemplates.frameCategories(frames) }

    private val byId by lazy { (templates + frames).associateBy { it.id } }
    fun byId(id: String): Template? = byId[id]

    /** Explore filter groups (HANDOFF §7.1 N3). */
    val styles = listOf("Torn paper", "Polaroid", "Film", "Stamp", "Minimal", "Dark")
    val topics = listOf("Travel", "Love", "Birthday", "Food", "Pets", "Fitness")
}

/** Template list helpers. */
fun Template.isFrame() = meta.kind == TemplateKind.Frame
fun meta(
    id: String, name: String, category: String, style: String, topic: String,
    isNew: Boolean = false, isPro: Boolean = false, framelabMade: Boolean = false, uses: Int = 0, order: Int = 0,
    kind: TemplateKind = TemplateKind.Template,
) = TemplateMeta(id, name, kind, category, style, topic, isNew, isPro, framelabMade, uses, order)

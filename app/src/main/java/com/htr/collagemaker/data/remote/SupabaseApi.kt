package com.htr.collagemaker.data.remote

import com.htr.collagemaker.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.net.HttpURLConnection
import java.net.URL

/**
 * Read-only Supabase (PostgREST) client — design/backend-handoff.md §4.
 * Uses only the publishable key; RLS allows SELECT and nothing else. Never put the secret key in the app.
 */
object SupabaseApi {
    val isConfigured: Boolean get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; explicitNulls = false }

    /** Home rows with the template ids in each row, in order (§4.1, without the heavy layout). */
    const val CATEGORIES_PATH =
        "template_categories?select=id,title,sort_order,category_templates(sort_order,templates(template_id))" +
            "&order=sort_order&category_templates.order=sort_order"

    /** Every template with its layout (`preview` is the same Doc as `source_json.doc`, §4.1–4.2). */
    const val TEMPLATES_PATH =
        "templates?select=id,template_id,title,description,size,number_of_panels,number_of_items,tier,source,preview" +
            "&order=id&limit=1000"

    /** GET [path] under /rest/v1 and return the raw JSON body. Throws on network or HTTP errors. */
    fun get(path: String): String {
        val c = URL("${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/$path").openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 30_000
        c.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
        c.setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
        c.setRequestProperty("Accept", "application/json")
        try {
            val code = c.responseCode
            if (code !in 200..299) error("HTTP $code")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    fun parseCategories(body: String): List<SbCategory> = json.decodeFromString(body)
    fun parseTemplates(body: String): List<SbTemplate> = json.decodeFromString(body)
}

// ───────────────────────────── Rows (§5.1) ─────────────────────────────

@Serializable
data class SbCategory(
    val id: Long,
    val title: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("category_templates") val links: List<SbLink> = emptyList(),
)

@Serializable
data class SbLink(
    @SerialName("sort_order") val sortOrder: Int = 0,
    val templates: SbTemplateRef? = null,
)

@Serializable
data class SbTemplateRef(@SerialName("template_id") val templateId: String)

@Serializable
data class SbTemplate(
    val id: Long,
    @SerialName("template_id") val templateId: String,
    val title: String,
    val description: String? = null,
    val size: String? = null,
    @SerialName("number_of_panels") val panels: Int? = null,
    @SerialName("number_of_items") val items: Int? = null,
    val tier: String? = null,
    val source: String? = null,
    val preview: SbDoc? = null,
)

// ───────────────────────────── Layout Doc (§5.3) ─────────────────────────────

@Serializable
data class SbDoc(
    val slideW: Float,
    val slideH: Float,
    val slides: Int,
    val bgColor: String? = null,
    val bgImage: String? = null,
    val root: List<SbNode> = emptyList(),
    val slotCount: Int = 0,
)

@Serializable
data class SbNode(
    val key: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val w: Float = 0f,
    val h: Float = 0f,
    val rotate: Float = 0f,
    val scale: Float = 1f,
    val imageUrl: String? = null,
    val maskUrl: String? = null,
    val slotIndex: Int? = null,
    val text: String? = null,
    val textStyle: SbTextStyle? = null,
    val bgColor: String? = null,
    /** Number or [tl, tr, br, bl]. */
    val radius: JsonElement? = null,
    val border: SbBorder? = null,
    val shadow: Boolean = false,
    val opacity: Float = 1f,
    val children: List<SbNode> = emptyList(),
)

@Serializable
data class SbTextStyle(
    val color: String = "#000000",
    val size: Float = 14f,
    /** "400".."900" (string in the data, number accepted too). */
    val weight: JsonElement? = null,
    val align: String? = null,
    val valign: String? = null,
    /** Letter spacing in design px. */
    val spacing: Float = 0f,
    /** Multiplier of [size]. */
    val lineHeight: Float? = null,
    val font: String? = null,
    val italic: Boolean = false,
)

@Serializable
data class SbBorder(val width: Float = 0f, val color: String = "#000000", val style: String? = null)

package com.framelab.app.render

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import com.framelab.app.R
import com.framelab.app.data.model.Design
import com.framelab.app.data.model.SLIDE_W
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Bundled sticker art (HANDOFF §4.3). Key = name used in [com.framelab.app.data.model.El.Sticker.art]. */
object StickerArt {
    val all: List<Pair<String, Int>> = listOf(
        "tape_masking" to R.drawable.img_sticker_tape_masking,
        "tape_pink" to R.drawable.img_sticker_tape_pink,
        "tape_gingham" to R.drawable.img_sticker_tape_gingham,
        "date_stamp" to R.drawable.img_sticker_date_stamp,
        "postmark" to R.drawable.img_sticker_postmark,
        "postage_stamp" to R.drawable.img_sticker_postage_stamp,
        "star" to R.drawable.img_sticker_star,
        "arrow" to R.drawable.img_sticker_arrow,
        "swipe_chip" to R.drawable.img_sticker_swipe_chip,
        "heart" to R.drawable.img_sticker_heart,
        "label" to R.drawable.img_sticker_label,
        "ticket" to R.drawable.img_sticker_ticket,
    )
    private val map = all.toMap()
    fun res(name: String): Int? = map[name]

    private val cache = HashMap<String, ImageBitmap>()
    fun bitmap(context: Context, name: String): ImageBitmap? = cache[name] ?: res(name)?.let { r ->
        androidx.core.content.ContextCompat.getDrawable(context, r)?.toBitmap()?.asImageBitmap()?.also { cache[name] = it }
    }
}

/** Decoded photo cache (keyed by uri + max size). */
object PhotoCache {
    private val cache = object : LruCache<String, ImageBitmap>(64 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    fun peek(uri: String, maxPx: Int): ImageBitmap? = cache.get("$uri@$maxPx")

    suspend fun load(context: Context, uri: String, maxPx: Int): ImageBitmap? {
        peek(uri, maxPx)?.let { return it }
        val req = ImageRequest.Builder(context).data(uri).size(maxPx).precision(Precision.INEXACT)
            .allowHardware(false).bitmapConfig(Bitmap.Config.ARGB_8888).build()
        val drawable = withContext(Dispatchers.IO) { context.imageLoader.execute(req).drawable } ?: return null
        val bmp = drawable.toBitmap().asImageBitmap()
        cache.put("$uri@$maxPx", bmp)
        return bmp
    }
}

/** A measurer where 1 sp = 1 design unit (templates are laid out in dp-like units; we scale the canvas). */
fun designTextMeasurer(context: Context): TextMeasurer =
    TextMeasurer(createFontFamilyResolver(context), Density(1f, 1f), LayoutDirection.Ltr, cacheSize = 64)

@Composable
fun rememberDesignTextMeasurer(): TextMeasurer {
    val ctx = LocalContext.current
    return remember { designTextMeasurer(ctx) }
}

/** Loads every photo used by [design] and recomposes as they arrive. */
@Composable
fun rememberDesignImages(design: Design, maxPx: Int = 1400): (String) -> ImageBitmap? {
    val ctx = LocalContext.current
    val loaded = remember { mutableStateMapOf<String, ImageBitmap>() }
    val uris = design.spots.mapNotNull { it.uri }.toSet()
    LaunchedEffect(uris, maxPx) {
        for (u in uris) if (u !in loaded) PhotoCache.load(ctx, u, maxPx)?.let { loaded[u] = it }
    }
    return { uri -> loaded[uri] ?: PhotoCache.peek(uri, maxPx) }
}

@Composable
fun rememberStickerArt(): (String) -> ImageBitmap? {
    val ctx = LocalContext.current
    return remember { { name: String -> StickerArt.bitmap(ctx, name) } }
}

/**
 * Draws one slide of [design], filling the modifier's width at the template's ratio.
 * Used for card thumbnails, previews, saved-post viewers and the contact sheet.
 */
@Composable
fun SlideView(
    design: Design,
    slide: Int,
    modifier: Modifier = Modifier,
    options: RenderOptions = RenderOptions(),
    keepRatio: Boolean = true,
) {
    val measurer = rememberDesignTextMeasurer()
    val images = rememberDesignImages(design, maxPx = 900)
    val art = rememberStickerArt()
    val m = if (keepRatio) modifier.aspectRatio(SLIDE_W / design.slideH) else modifier
    Canvas(m) {
        // Thumbnails can be wider/taller than the ratio when keepRatio=false: cover the box.
        val s = maxOf(size.width / SLIDE_W, size.height / design.slideH)
        val ox = (size.width - SLIDE_W * s) / 2f
        val oy = (size.height - design.slideH * s) / 2f
        clipRect {
            translate(ox, oy) {
                scale(s, s, androidx.compose.ui.geometry.Offset.Zero) {
                    translate(-slide * SLIDE_W, 0f) {
                        drawDesign(design, measurer, images, art, options)
                    }
                }
            }
        }
    }
}

/** The whole carousel as one long strip (seamless preview, "ALL 6 SLIDES, JOINED"). */
@Composable
fun StripView(design: Design, modifier: Modifier = Modifier, options: RenderOptions = RenderOptions()) {
    val measurer = rememberDesignTextMeasurer()
    val images = rememberDesignImages(design, maxPx = 600)
    val art = rememberStickerArt()
    Canvas(modifier.aspectRatio(design.canvasW / design.slideH)) {
        val s = size.width / design.canvasW
        clipRect {
            scale(s, s, androidx.compose.ui.geometry.Offset.Zero) {
                drawDesign(design, measurer, images, art, options)
            }
        }
    }
}


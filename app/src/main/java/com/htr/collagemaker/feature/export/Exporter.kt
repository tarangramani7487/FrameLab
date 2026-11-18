package com.htr.collagemaker.feature.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.htr.collagemaker.data.FileFormat
import com.htr.collagemaker.data.Quality
import com.htr.collagemaker.data.Settings
import com.htr.collagemaker.data.model.Design
import com.htr.collagemaker.render.EmptyStyle
import com.htr.collagemaker.render.PhotoCache
import com.htr.collagemaker.render.RenderOptions
import com.htr.collagemaker.render.StickerArt
import com.htr.collagemaker.render.designTextMeasurer
import com.htr.collagemaker.render.drawSlide
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.Normalizer

/**
 * Saves every slide of a design at full size into Pictures/FrameLab via MediaStore (HANDOFF §8.9).
 */
object Exporter {
    /** Output size in px for the design's ratio (1080 × 1350 for 4:5). */
    fun pixelSize(design: Design): Pair<Int, Int> = design.ratio.pxW to design.ratio.pxH

    /**
     * Renders and saves slides [from]..last. Calls [onProgress] (savedCount, total) on the main thread after each
     * slide — `done` is the 1-based number of the slide just saved, `total` the slide count.
     * Returns the content URIs saved in order, or throws [ExportException] with the slides saved so far.
     */
    suspend fun saveSlides(
        context: Context,
        design: Design,
        settings: Settings,
        from: Int = 0,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): List<Uri> = withContext(Dispatchers.Default) {
        val app = context.applicationContext
        val measurer = designTextMeasurer(app)
        val images = HashMap<String, ImageBitmap>()
        for (u in design.spots.mapNotNull { it.uri }.distinct()) {
            runCatching { PhotoCache.load(app, u, 2400) }.getOrNull()?.let { images[u] = it }
        }
        val slug = slugify(design.name)
        val stamp = System.currentTimeMillis()
        val saved = mutableListOf<Uri>()
        for (k in from.coerceAtLeast(0) until design.slides) {
            try {
                val bmp = renderSlide(app, design, k, measurer, images)
                val ext = if (settings.format == FileFormat.PNG) "png" else "jpg"
                val name = if (settings.numberFiles) "%s-%02d.%s".format(slug, k + 1, ext) else "$slug-$stamp-${k + 1}.$ext"
                val uri = withContext(Dispatchers.IO) { write(app, bmp, name, settings) }
                bmp.asAndroidBitmap().recycle()
                saved += uri
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                throw ExportException(k, saved.toList(), e.message ?: e.javaClass.simpleName)
            }
            withContext(Dispatchers.Main) { onProgress(k + 1, design.slides) }
        }
        saved.toList()
    }

    /** Renders slide [index] offscreen at full export size. */
    fun renderSlide(
        context: Context,
        design: Design,
        index: Int,
        measurer: TextMeasurer = designTextMeasurer(context),
        images: Map<String, ImageBitmap> = emptyMap(),
        widthPx: Int = design.ratio.pxW,
    ): ImageBitmap {
        val heightPx = (widthPx.toLong() * design.ratio.pxH / design.ratio.pxW).toInt()
        val img = ImageBitmap(widthPx, heightPx, ImageBitmapConfig.Argb8888)
        val canvas = Canvas(img)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(widthPx.toFloat(), heightPx.toFloat())) {
            drawSlide(
                design, index, measurer,
                images = { images[it] },
                art = { StickerArt.bitmap(context, it) },
                options = RenderOptions(empty = EmptyStyle.Paper),
            )
        }
        return img
    }

    private fun write(context: Context, img: ImageBitmap, name: String, settings: Settings): Uri {
        val png = settings.format == FileFormat.PNG
        val mime = if (png) "image/png" else "image/jpeg"
        val quality = if (settings.quality == Quality.Max) 100 else 95
        val bitmap = img.asAndroidBitmap()
        val compress: (OutputStream) -> Unit = { out ->
            check(bitmap.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, quality, out)) { "Couldn't encode image" }
        }
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(MediaStore.Images.Media.RELATIVE_PATH, if (settings.album) "${Environment.DIRECTORY_PICTURES}/$ALBUM" else Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
                ?: error("MediaStore insert failed")
            try {
                resolver.openOutputStream(uri)?.use(compress) ?: error("Couldn't open output")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } catch (e: Throwable) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            @Suppress("DEPRECATION")
            val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val dir = if (settings.album) File(root, ALBUM) else root
            dir.mkdirs()
            var file = File(dir, name)
            var n = 1
            while (file.exists()) { file = File(dir, name.substringBeforeLast('.') + " ($n)." + name.substringAfterLast('.')); n++ }
            FileOutputStream(file).use(compress)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                @Suppress("DEPRECATION")
                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: Uri.fromFile(file)
        }
    }

    const val ALBUM = "FrameLab"

    /** "Torn Diary · Goa trip" → "torn-diary-goa-trip". */
    fun slugify(name: String): String {
        val s = Normalizer.normalize(name, Normalizer.Form.NFKD).replace(Regex("[^\\p{ASCII}]"), "")
            .lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return s.ifEmpty { "framelab" }.take(40).trim('-')
    }
}

class ExportException(val failedSlide: Int, val saved: List<Uri>, message: String) : Exception(message)

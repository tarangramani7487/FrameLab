package com.htr.collagemaker.feature.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.htr.collagemaker.feature.onboarding.photoPermissions

object ShareUtils {
    const val INSTAGRAM = "com.instagram.android"

    fun isInstagramInstalled(context: Context): Boolean =
        runCatching { context.packageManager.getPackageInfo(INSTAGRAM, 0); true }.getOrDefault(false)

    /**
     * Hands every image to another app, in order (multi-image share intent).
     * [toInstagram] targets Instagram when it is installed, otherwise shows the system chooser.
     * Returns false if nothing could handle the intent.
     */
    fun shareImages(context: Context, uris: List<Uri>, toInstagram: Boolean, chooserTitle: String? = null): Boolean {
        if (uris.isEmpty()) return false
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }.apply {
            type = "image/*"
            clipData = ClipData.newRawUri(null, uris.first()).also { c -> uris.drop(1).forEach { c.addItem(ClipData.Item(it)) } }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val target = if (toInstagram && isInstagramInstalled(context)) intent.setPackage(INSTAGRAM)
        else Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is android.app.Activity) target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(target); true }.getOrElse {
            if (toInstagram) shareImages(context, uris, false, chooserTitle) else false
        }
    }

    /** Shares the same images given as strings (saved posts store content:// URIs as strings). */
    fun shareImageStrings(context: Context, uris: List<String>, toInstagram: Boolean, chooserTitle: String? = null) =
        shareImages(context, uris.map(Uri::parse), toInstagram, chooserTitle)

    /** Opens Instagram (or its Play listing). */
    fun openInstagram(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(INSTAGRAM)
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$INSTAGRAM"))
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(launch) }
    }

    /** Opens this app's system settings page (Photo access is off → Open settings). */
    fun openAppSettings(context: Context) {
        val i = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(i) }
    }
}

/** True when FrameLab can read the gallery (full or partial "selected photos" access). */
fun hasPhotoPermission(context: Context): Boolean =
    photoPermissions().any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

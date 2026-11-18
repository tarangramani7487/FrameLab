package com.htr.collagemaker.engage

import android.os.Build
import com.htr.collagemaker.BuildConfig
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * "Are you getting issues?" reports → Firestore `issue_reports` (see firebase/firestore.rules).
 * The app can only create reports; nobody can read them from the app.
 */
object IssueReports {
    const val COLLECTION = "issue_reports"
    const val TITLE_MAX = 120
    const val DESCRIPTION_MAX = 2000

    enum class Result { Sent, Queued, Failed }

    /**
     * Sends a report. Firestore keeps writes made offline and sends them later, so a write that the server
     * hasn't confirmed within 10 s is reported as [Result.Queued] rather than failed.
     */
    suspend fun submit(title: String, description: String): Result {
        val doc = mapOf(
            "title" to title.trim().take(TITLE_MAX),
            "description" to description.trim().take(DESCRIPTION_MAX),
            "platform" to "android",
            "appVersion" to BuildConfig.VERSION_NAME,
            "versionCode" to BuildConfig.VERSION_CODE,
            "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "osVersion" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "locale" to Locale.getDefault().toLanguageTag(),
            "status" to "new",
            "createdAt" to FieldValue.serverTimestamp(),
        )
        return try {
            val task = FirebaseFirestore.getInstance().collection(COLLECTION).add(doc)
            if (withTimeoutOrNull(10_000) { task.await() } != null) Result.Sent else Result.Queued
        } catch (e: Exception) {
            Result.Failed
        }
    }
}

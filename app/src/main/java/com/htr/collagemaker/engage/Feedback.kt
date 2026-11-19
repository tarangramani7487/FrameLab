package com.htr.collagemaker.engage

import android.os.Build
import com.htr.collagemaker.BuildConfig
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * "Send feedback" (Profile → Help & feedback) → Firestore `feedback` (see firebase/firestore.rules).
 * The app can only create documents; read them in the Firebase console.
 */
object Feedback {
    const val COLLECTION = "feedback"
    const val MESSAGE_MAX = 2000

    /** Stored as a stable key so the console stays readable whatever the app language. */
    val TOPICS = listOf("broken", "idea", "template_request", "other")

    /**
     * Sends feedback. Device details are added only when [includeDeviceInfo] is on.
     * A write the server hasn't confirmed within 10 s is reported as [IssueReports.Result.Queued]:
     * Firestore keeps it and sends it when the phone is back online.
     */
    suspend fun submit(topicIndex: Int, message: String, includeDeviceInfo: Boolean): IssueReports.Result {
        val doc = buildMap<String, Any> {
            put("topic", TOPICS.getOrElse(topicIndex) { "other" })
            put("message", message.trim().take(MESSAGE_MAX))
            put("platform", "android")
            put("appVersion", BuildConfig.VERSION_NAME)
            put("locale", Locale.getDefault().toLanguageTag())
            put("includeDeviceInfo", includeDeviceInfo)
            if (includeDeviceInfo) {
                put("versionCode", BuildConfig.VERSION_CODE)
                put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                put("osVersion", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            }
            put("status", "new")
            put("createdAt", FieldValue.serverTimestamp())
        }
        return try {
            val task = FirebaseFirestore.getInstance().collection(COLLECTION).add(doc)
            if (withTimeoutOrNull(10_000) { task.await() } != null) IssueReports.Result.Sent else IssueReports.Result.Queued
        } catch (e: Exception) {
            IssueReports.Result.Failed
        }
    }
}

package com.htr.collagemaker.engage

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.tasks.await

/**
 * Google Play in-app review. Google decides whether the card actually appears (it has its own quota,
 * shows only for apps installed from Play and never tells us the result), so asking is always safe.
 */
object InAppReview {
    private var askedThisSession = false

    /** Asks once per app session. */
    suspend fun request(activity: Activity) {
        if (askedThisSession) return
        askedThisSession = true
        runCatching {
            val manager = ReviewManagerFactory.create(activity)
            val info = manager.requestReviewFlow().await()
            manager.launchReviewFlow(activity, info).await()
        }.onFailure { Log.w("FrameLabReview", "In-app review unavailable", it) }
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

package com.htr.collagemaker.engage

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/**
 * Runs when Home is shown.
 * - Android 13+: asks for notification permission once (needed for reminders and campaigns).
 * - Otherwise: asks Google Play for the in-app review card (once per app session; Play decides if it shows).
 * The two never run on the same visit, so the system prompts don't stack.
 */
@Composable
fun HomeEngagement() {
    val ctx = LocalContext.current
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) Notifications.registerToken(ctx)
    }
    LaunchedEffect(Unit) {
        if (Notifications.shouldAskPermission(ctx)) {
            delay(800)
            Notifications.markPermissionAsked(ctx)
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return@LaunchedEffect
        }
        if (AppUpdate.required.value != null || Engage.reportIssue.value) return@LaunchedEffect
        delay(1500)
        ctx.findActivity()?.let { InAppReview.request(it) }
    }
}

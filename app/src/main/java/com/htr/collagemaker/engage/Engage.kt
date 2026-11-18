package com.htr.collagemaker.engage

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.flow.MutableStateFlow

/** Wires the engagement features into the app (Application, MainActivity and the Compose root). */
object Engage {
    /** Intent extra used by the launcher quick action (res/xml/shortcuts.xml). */
    const val EXTRA_ACTION = "action"
    const val ACTION_REPORT_ISSUE = "report_issue"

    /** "Are you getting issues?" modal; set from Settings or the quick action. */
    val reportIssue = MutableStateFlow(false)

    fun init(ctx: Context) {
        Notifications.init(ctx)
        AppUpdate.init(ctx)
    }

    /** Handles quick-action intents (from onCreate and onNewIntent). */
    fun handleIntent(intent: Intent?) {
        if (intent?.getStringExtra(EXTRA_ACTION) == ACTION_REPORT_ISSUE) {
            reportIssue.value = true
            intent.removeExtra(EXTRA_ACTION)
        }
    }
}

/** Put once at the root of the UI: modals plus foreground bookkeeping. */
@Composable
fun EngageHost() {
    val ctx = LocalContext.current
    val report by Engage.reportIssue.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        Notifications.markOpened(ctx)
        AppUpdate.refresh()
    }
    ReportIssueDialog(report) { Engage.reportIssue.value = false }
    ForceUpdateDialog()
}

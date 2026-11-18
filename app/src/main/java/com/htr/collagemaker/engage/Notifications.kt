package com.htr.collagemaker.engage

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.htr.collagemaker.BuildConfig
import com.htr.collagemaker.MainActivity
import com.htr.collagemaker.R
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.concurrent.TimeUnit

object Notifications {
    /** Engagement reminders (local, from [ReminderWorker]). */
    const val CHANNEL_REMINDERS = "reminders"
    /** Campaigns sent from the Firebase console (also the FCM default channel in the manifest). */
    const val CHANNEL_NEWS = "news"
    /** Every install subscribes to this topic, so a campaign can target "all" by topic too. */
    const val TOPIC_ALL = "all"
    const val TAG = "FrameLabPush"

    private const val PREFS = "engage"
    private const val KEY_LAST_OPEN = "last_open"
    private const val KEY_LAST_REMINDER = "last_reminder"
    private const val KEY_REMINDER_INDEX = "reminder_index"
    private const val KEY_ASKED_PERMISSION = "asked_notification_permission"
    private const val WORK_NAME = "engagement-reminders"

    /** A reminder is sent only after this long without opening the app, and at most this often. */
    private val QUIET_MS = TimeUnit.DAYS.toMillis(2)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Called from Application.onCreate. */
    fun init(ctx: Context) {
        createChannels(ctx)
        scheduleReminders(ctx)
        registerToken(ctx)
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Call when the app comes to the foreground: resets the reminder clock. */
    fun markOpened(ctx: Context) = prefs(ctx).edit().putLong(KEY_LAST_OPEN, System.currentTimeMillis()).apply()

    fun canPost(ctx: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** True when the Android 13+ permission prompt should be shown (only asked once by the app). */
    fun shouldAskPermission(ctx: Context) = Build.VERSION.SDK_INT >= 33 && !canPost(ctx) && !prefs(ctx).getBoolean(KEY_ASKED_PERMISSION, false)
    fun markPermissionAsked(ctx: Context) = prefs(ctx).edit().putBoolean(KEY_ASKED_PERMISSION, true).apply()
    fun permissionAsked(ctx: Context) = Build.VERSION.SDK_INT < 33 || canPost(ctx) || prefs(ctx).getBoolean(KEY_ASKED_PERMISSION, false)

    private fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, ctx.getString(R.string.n_channel_reminders), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = ctx.getString(R.string.n_channel_reminders_desc) },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_NEWS, ctx.getString(R.string.n_channel_news), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = ctx.getString(R.string.n_channel_news_desc) },
        )
    }

    /** Checks twice a day; [ReminderWorker] decides whether a reminder is due. */
    private fun scheduleReminders(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS)
            .setInitialDelay(12, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    /** Sends the next engagement reminder if the user has been away for [QUIET_MS]. */
    internal fun maybeSendReminder(ctx: Context): Boolean {
        val p = prefs(ctx)
        val now = System.currentTimeMillis()
        val lastOpen = p.getLong(KEY_LAST_OPEN, now)
        val lastReminder = p.getLong(KEY_LAST_REMINDER, 0L)
        if (now - lastOpen < QUIET_MS || now - lastReminder < QUIET_MS || !canPost(ctx)) return false

        val titles = ctx.resources.getStringArray(R.array.n_reminder_titles)
        val bodies = ctx.resources.getStringArray(R.array.n_reminder_bodies)
        val i = p.getInt(KEY_REMINDER_INDEX, 0) % titles.size
        show(ctx, CHANNEL_REMINDERS, 1001, titles[i], bodies[i])
        p.edit().putLong(KEY_LAST_REMINDER, now).putInt(KEY_REMINDER_INDEX, i + 1).apply()
        return true
    }

    fun show(ctx: Context, channel: String, id: Int, title: String?, body: String?) {
        if (!canPost(ctx)) return
        val open = PendingIntent.getActivity(
            ctx, id,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(ctx, R.color.fl_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    /** Gets the FCM token, subscribes to [TOPIC_ALL] and stores the token in Firestore `devices/{token}`. */
    fun registerToken(ctx: Context) {
        scope.launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                saveToken(token)
                FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_ALL).await()
            }.onFailure { Log.w(TAG, "FCM registration failed", it) }
        }
    }

    internal fun saveToken(token: String) {
        // Shown in Logcat so you can paste it into "Send test message" in the Firebase console.
        Log.i(TAG, "FCM token: $token")
        val doc = mapOf(
            "token" to token,
            "platform" to "android",
            "appVersion" to BuildConfig.VERSION_NAME,
            "locale" to Locale.getDefault().toLanguageTag(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        FirebaseFirestore.getInstance().collection("devices").document(token).set(doc)
            .addOnFailureListener { Log.w(TAG, "Saving FCM token failed", it) }
    }
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Notifications.maybeSendReminder(applicationContext)
        return Result.success()
    }
}

/**
 * Firebase Cloud Messaging. While the app is in the background, console campaigns are shown by the system;
 * while it's open, they arrive here and are shown as a normal notification.
 */
class FrameLabMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Notifications.saveToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val n = message.notification
        val title = n?.title ?: message.data["title"]
        val body = n?.body ?: message.data["body"]
        if (title == null && body == null) return
        Notifications.show(applicationContext, n?.channelId ?: Notifications.CHANNEL_NEWS, (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), title, body)
    }
}

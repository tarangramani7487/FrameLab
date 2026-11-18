package com.htr.collagemaker.engage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.htr.collagemaker.BuildConfig
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Force update from Firebase Remote Config (firebase/remote_config.json).
 * `force_app_update` = the lowest version allowed, e.g. "1.0.1". If this app's versionName is lower,
 * a modal the user can't close asks them to update.
 */
object AppUpdate {
    const val KEY_VERSION = "force_app_update"
    const val KEY_TITLE = "force_app_update_title"
    const val KEY_MESSAGE = "force_app_update_message"
    const val KEY_URL = "force_app_update_url"

    data class Required(val minVersion: String, val title: String, val message: String, val url: String)

    private val _required = MutableStateFlow<Required?>(null)
    /** Non-null while this version is below `force_app_update`. */
    val required: StateFlow<Required?> = _required.asStateFlow()

    private val config get() = FirebaseRemoteConfig.getInstance()

    fun init(ctx: Context) {
        config.setConfigSettingsAsync(
            remoteConfigSettings {
                // Release: Firebase caches values for up to 1 h. Debug: always fetch.
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
            },
        )
        config.setDefaultsAsync(mapOf(KEY_VERSION to "0.0.0", KEY_TITLE to "", KEY_MESSAGE to "", KEY_URL to ""))
        evaluate()
        refresh()
        // Real-time updates: a change published in the console reaches open apps within seconds.
        config.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                config.activate().addOnCompleteListener { evaluate() }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.w("FrameLabUpdate", "Remote Config listener error", error)
            }
        })
    }

    /** Fetch + activate; also called when the app returns to the foreground. */
    fun refresh() {
        config.fetchAndActivate().addOnCompleteListener { evaluate() }
    }

    private fun evaluate() {
        val min = config.getString(KEY_VERSION).trim()
        _required.value = if (isOlder(BuildConfig.VERSION_NAME, min)) {
            Required(min, config.getString(KEY_TITLE).trim(), config.getString(KEY_MESSAGE).trim(), config.getString(KEY_URL).trim())
        } else null
    }

    /** "1.2.3" style compare; missing parts count as 0, non-numeric suffixes ("-beta") are ignored. */
    fun isOlder(current: String, required: String): Boolean {
        fun parts(v: String) = v.split('.').map { p -> p.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
        val a = parts(current); val b = parts(required)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x < y
        }
        return false
    }

    /** Opens the Play Store page (or `force_app_update_url` when set). */
    fun openStore(ctx: Context, url: String) {
        val pkg = ctx.packageName
        val intents = buildList {
            if (url.isNotBlank()) add(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            add(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).setPackage("com.android.vending"))
            add(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
        }
        for (i in intents) {
            if (runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }
    }
}

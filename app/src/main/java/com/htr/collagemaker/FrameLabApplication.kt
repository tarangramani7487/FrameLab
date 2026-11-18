package com.htr.collagemaker

import android.app.Application
import com.htr.collagemaker.data.AppContainer
import com.htr.collagemaker.engage.Engage

class FrameLabApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Reminders, FCM token, Remote Config force update.
        Engage.init(this)
    }
}

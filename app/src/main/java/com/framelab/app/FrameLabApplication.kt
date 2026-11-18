package com.framelab.app

import android.app.Application
import com.framelab.app.data.AppContainer

class FrameLabApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

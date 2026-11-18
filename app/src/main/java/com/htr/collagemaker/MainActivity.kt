package com.htr.collagemaker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.htr.collagemaker.engage.Engage
import com.htr.collagemaker.nav.FrameLabRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val container = (application as FrameLabApplication).container
        // Debug/dev hook: `adb shell am start -n com.htr.collagemaker/.MainActivity --es route "profile"` opens a route directly.
        val startRoute = intent?.getStringExtra("route")
        // Launcher quick action ("Are you getting issues?").
        if (savedInstanceState == null) Engage.handleIntent(intent)
        setContent { FrameLabRoot(container, startRoute) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Engage.handleIntent(intent)
    }
}

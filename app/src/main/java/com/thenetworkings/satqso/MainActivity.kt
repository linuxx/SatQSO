package com.thenetworkings.satqso

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.thenetworkings.satqso.ui.passes.PassListRoute
import com.thenetworkings.satqso.ui.theme.SatQSOTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dependencies = SatQsoDependencies(applicationContext)
        applyDisableSleep(dependencies.passDisplayPreferences.disableSleep())
        applyDisableRotation(dependencies.passDisplayPreferences.disableRotation())
        enableEdgeToEdge()
        setContent {
            SatQSOTheme {
                PassListRoute(
                    dependencies = dependencies,
                    onDisableSleepChanged = ::applyDisableSleep,
                    onDisableRotationChanged = ::applyDisableRotation,
                )
            }
        }
    }

    private fun applyDisableSleep(disableSleep: Boolean) {
        if (disableSleep) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun applyDisableRotation(disableRotation: Boolean) {
        requestedOrientation = if (disableRotation) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

package com.thenetworkings.satqso

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.thenetworkings.satqso.ui.passes.PassListRoute
import com.thenetworkings.satqso.ui.theme.SatQSOTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            val dependencies = remember { SatQsoDependencies(applicationContext) }
            SatQSOTheme {
                PassListRoute(dependencies)
            }
        }
    }
}

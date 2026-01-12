package com.xenonware.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import com.xenonware.phone.data.SharedPreferenceManager
import com.xenonware.phone.ui.layouts.CallHistoryLayout
import com.xenonware.phone.ui.theme.ScreenEnvironment

class CallHistoryActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val sharedPreferenceManager = SharedPreferenceManager(applicationContext)

            val themePreference = sharedPreferenceManager.theme
            val blackedOutModeEnabled = sharedPreferenceManager.blackedOutModeEnabled

            val containerSize = LocalWindowInfo.current.containerSize
            val applyCoverTheme = sharedPreferenceManager.isCoverThemeApplied(containerSize)

            ScreenEnvironment(
                themePreference = themePreference,
                coverTheme = applyCoverTheme,
                blackedOutModeEnabled = blackedOutModeEnabled
            ) { layoutType, isLandscape ->
                Surface(color = Color.Transparent, modifier = Modifier.fillMaxSize()) {
                    CallHistoryLayout(
                        onNavigateBack = { finish() },
                        isLandscape = isLandscape,
                        layoutType = layoutType,
                    )
                }
            }
        }
    }
}

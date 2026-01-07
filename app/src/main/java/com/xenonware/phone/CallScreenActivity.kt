package com.xenonware.phone

import android.os.Bundle
import android.telecom.Call
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import com.xenonware.phone.data.SharedPreferenceManager
import com.xenonware.phone.ui.layouts.CallScreenLayout
import com.xenonware.phone.ui.theme.ScreenEnvironment

class CallScreenActivity : ComponentActivity() {

    companion object {
        var currentCall: Call? = null
    }

    private var shouldFinishAfterDelay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContent {
            val sharedPreferenceManager = SharedPreferenceManager(applicationContext)

            val themePreference = sharedPreferenceManager.theme
            val blackedOutModeEnabled = sharedPreferenceManager.blackedOutModeEnabled

            val containerSize = LocalWindowInfo.current.containerSize
            val applyCoverTheme = sharedPreferenceManager.isCoverThemeApplied(containerSize)

            // In CallScreenActivity.kt setContent block
            ScreenEnvironment(
                themePreference = themePreference,
                coverTheme = applyCoverTheme,
                blackedOutModeEnabled = blackedOutModeEnabled
            ) { layoutType, isLandscape ->
                Surface(color = Color.Transparent, modifier = Modifier.fillMaxSize()) {
                    CallScreenLayout(
                        currentCall = currentCall,
                        isLandscape = isLandscape,
                        layoutType = layoutType,
                        appSize = containerSize
                    )
                }
            }

        }

        currentCall?.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                if ((state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) && !shouldFinishAfterDelay) {
                    shouldFinishAfterDelay = true
                    window.decorView.postDelayed({
                        if (!isFinishing && !isDestroyed) finish()
                    }, 2000)
                }
            }
        })
    }
}
package com.xenonware.phone

import android.os.Bundle
import android.telecom.Call
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import com.xenonware.phone.data.SharedPreferenceManager
import com.xenonware.phone.ui.layouts.callscreen.CallScreenContent
import com.xenonware.phone.ui.theme.ScreenEnvironment
import com.xenonware.phone.viewmodel.CallScreenViewModel
import com.xenonware.phone.viewmodel.CallScreenViewModelFactory

class CallScreenActivity : ComponentActivity() {
    private val viewModel: CallScreenViewModel by viewModels {
        CallScreenViewModelFactory(applicationContext, currentCall)
    }
    private lateinit var sharedPreferenceManager: SharedPreferenceManager

    private var lastAppliedTheme: Int = -1
    private var lastAppliedCoverThemeEnabled: Boolean = false
    private var lastAppliedBlackedOutMode: Boolean = false

    companion object {
        var currentCall: Call? = null
    }

    private var shouldFinishAfterDelay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferenceManager = SharedPreferenceManager(applicationContext)

        val initialThemePref = sharedPreferenceManager.theme
        val initialCoverThemeEnabledSetting = sharedPreferenceManager.coverThemeEnabled
        val initialBlackedOutMode = sharedPreferenceManager.blackedOutModeEnabled


        lastAppliedTheme = initialThemePref
        lastAppliedCoverThemeEnabled = initialCoverThemeEnabledSetting
        lastAppliedBlackedOutMode = initialBlackedOutMode

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        setContent {
            val currentContainerSize = LocalWindowInfo.current.containerSize
            val applyCoverTheme = sharedPreferenceManager.isCoverThemeApplied(currentContainerSize)
            ScreenEnvironment(
                themePreference = sharedPreferenceManager.theme,
                coverTheme = applyCoverTheme,
                blackedOutModeEnabled = sharedPreferenceManager.blackedOutModeEnabled
            ) { _, _ ->
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface
                ) {
                    CallScreenContent(
                        call = currentCall, onFinishRequested = {
                            if (!isFinishing && !isDestroyed) finish()
                        }, viewModel = viewModel
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
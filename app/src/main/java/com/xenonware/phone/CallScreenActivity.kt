package com.xenonware.phone

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xenonware.phone.data.SharedPreferenceManager
import com.xenonware.phone.helper.CallNotificationHelper
import com.xenonware.phone.service.MyInCallService
import com.xenonware.phone.ui.layouts.CallScreenLayout
import com.xenonware.phone.ui.theme.ScreenEnvironment
import com.xenonware.phone.viewmodel.CallScreenViewModel

class CallScreenActivity : ComponentActivity() {

    companion object {
        var currentCall: Call? = null
        var isVisible = false
    }
    private fun callFinish() {
        if (isTaskRoot) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    private var shouldFinishAfterDelay = false

    override fun onResume() {
        super.onResume()
        isVisible = true
        MyInCallService.currentCall?.let { call ->
            if (call.state == Call.STATE_RINGING) {
                CallNotificationHelper.showIncomingCallNotification(this, call)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isVisible = false
        MyInCallService.currentCall?.let { call ->
            if (call.state == Call.STATE_RINGING) {
                CallNotificationHelper.showIncomingCallNotification(applicationContext, call)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isVisible = true
    }

    override fun onStop() {
        super.onStop()
        isVisible = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            val sharedPreferenceManager = SharedPreferenceManager(applicationContext)

            val themePreference = sharedPreferenceManager.theme
            val blackedOutModeEnabled = sharedPreferenceManager.blackedOutModeEnabled

            val containerSize = LocalWindowInfo.current.containerSize
            val applyCoverTheme = sharedPreferenceManager.isCoverThemeApplied(containerSize)

            val viewModel: CallScreenViewModel = viewModel()

            val currentAudioRoute by viewModel.currentAudioRoute.collectAsState()

            ProximityHandler(
                isEarpiece = currentAudioRoute == CallAudioState.ROUTE_EARPIECE,
                window = window
            )

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
                        appSize = containerSize,
                    )
                }
            }
        }

        currentCall?.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                if ((state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) && !shouldFinishAfterDelay) {
                    shouldFinishAfterDelay = true
                    window.decorView.postDelayed({
                        callFinish()
                    }, 2000)
                }
            }
        })
    }
}

@SuppressLint("WakelockTimeout", "Wakelock")
@Composable
private fun ProximityHandler(isEarpiece: Boolean, window: Window) {
    val context = LocalContext.current
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val proximitySensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) }

    val proximityWakeLock = remember {
        if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "PhoneApp:ProximityScreenOff")
        } else {
            null
        }
    }

    DisposableEffect(isEarpiece) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (proximityWakeLock == null || proximitySensor == null) {
            onDispose {}
            return@DisposableEffect onDispose {}
        }

        if (isEarpiece) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            if (!proximityWakeLock.isHeld) {
                proximityWakeLock.acquire()
            }
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            if (proximityWakeLock.isHeld) {
                proximityWakeLock.release()
            }
        }

        onDispose {
            if (proximityWakeLock.isHeld) {
                proximityWakeLock.release()
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
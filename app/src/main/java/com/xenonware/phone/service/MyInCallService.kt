package com.xenonware.phone.service

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import com.xenonware.phone.CallScreenActivity
import com.xenonware.phone.data.SharedPreferenceManager
import com.xenonware.phone.helper.CallNotificationHelper
import com.xenonware.phone.helper.CallNotificationHelper.dismissIncomingCallNotification
import com.xenonware.phone.helper.CallNotificationHelper.showIncomingCallNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MyInCallService : InCallService() {

    private val prefManager by lazy { SharedPreferenceManager(this) }

    private val useNewLayout: Boolean
        get() = prefManager.newLayoutEnabled

    companion object {
        @Volatile
        private var INSTANCE: MyInCallService? = null
        fun getInstance(): MyInCallService? = INSTANCE

        var currentAudioState: CallAudioState? = null
            private set

        // Safe reference for receiver and notification updates
        @Volatile
        var currentCall: Call? = null
            private set

        private val _audioStateFlow = MutableStateFlow<CallAudioState?>(null)
        val audioStateFlow: StateFlow<CallAudioState?> = _audioStateFlow.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        CallNotificationHelper.createOngoingNotificationChannel(this)
    }

    override fun onDestroy() {
        INSTANCE = null
        currentCall = null
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d("MyInCallService", "Call added: $call")

        // Store references
        currentCall = call
        CallScreenActivity.currentCall = call

        // Launch activity (helps when app is in foreground)
        val targetActivityClass = CallScreenActivity::class.java
        val intent = Intent(this, targetActivityClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)

        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                when (state) {
                    Call.STATE_RINGING -> {
                        // Only show incoming notification if our CallScreenActivity is NOT visible
                        val activityVisible = CallScreenActivity.isVisible
                        if (!activityVisible) {
                            showIncomingCallNotification(this@MyInCallService, call, useNewLayout)
                        }
                    }
                    Call.STATE_ACTIVE -> {
                        dismissIncomingCallNotification(this@MyInCallService)
                        CallNotificationHelper.showOngoingCallNotification(this@MyInCallService, call, useNewLayout)
                    }
                    Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                        dismissIncomingCallNotification(this@MyInCallService)
                        CallNotificationHelper.dismissOngoingCallNotification(this@MyInCallService)
                        currentCall = null
                        CallScreenActivity.currentCall = null
                    }
                }
            }
        })

        when (call.state) {
            Call.STATE_RINGING -> {
                val activityVisible = CallScreenActivity.isVisible
                if (!activityVisible) {
                    showIncomingCallNotification(this, call, useNewLayout)
                }
            }
            Call.STATE_ACTIVE -> CallNotificationHelper.showOngoingCallNotification(this, call, useNewLayout)
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        currentAudioState = audioState
        _audioStateFlow.value = audioState

        currentCall?.takeIf { it.state == Call.STATE_ACTIVE }?.let { call ->
            CallNotificationHelper.showOngoingCallNotification(this, call, useNewLayout)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        if (currentCall == call) {
            currentCall = null
            CallScreenActivity.currentCall = null
            CallNotificationHelper.dismissOngoingCallNotification(this)
        }
    }
}
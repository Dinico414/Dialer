package com.xenonware.phone.service

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import com.xenonware.phone.data.SharedPreferenceManager
import com.xenonware.phone.CallScreenActivity as NewCallScreen
import com.xenonware.phone.ui.layouts.callscreen.CallScreenActivity as OldCallScreen

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
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }

    override fun onDestroy() {
        INSTANCE = null
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d("MyInCallService", "Call added: $call")

        val targetActivityClass = if (useNewLayout) {
            NewCallScreen.currentCall = call
            NewCallScreen::class.java
        } else {
            OldCallScreen.currentCall = call
            OldCallScreen::class.java
        }

        val intent = Intent(this, targetActivityClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    @Override
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        currentAudioState = audioState
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d("MyInCallService", "Call removed: $call")

        if (useNewLayout) {
            if (NewCallScreen.currentCall == call) {
                NewCallScreen.currentCall = null
            }
        } else {
            if (OldCallScreen.currentCall == call) {
                OldCallScreen.currentCall = null
            }
        }
    }
}
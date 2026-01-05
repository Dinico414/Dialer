package com.xenonware.phone

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import com.xenonware.phone.ui.layouts.callscreen.CallScreenActivity

class MyInCallService : InCallService() {

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

        CallScreenActivity.currentCall = call

        val intent = Intent(this, CallScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d("MyInCallService", "Call removed: $call")

        if (CallScreenActivity.currentCall == call) {
            CallScreenActivity.currentCall = null
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        currentAudioState = audioState
    }
}
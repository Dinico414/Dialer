package com.xenonware.phone

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.xenonware.phone.ui.layouts.callscreen.CallScreenActivity

class MyInCallService : InCallService() {

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
}
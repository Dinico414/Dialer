package com.xenonware.phone.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.phone.MyInCallService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class CallScreenViewModel : ViewModel() {

    var callState by mutableStateOf<Int?>(null)
        private set

    var displayName by mutableStateOf("Unknown")
        private set

    var connectTimeMillis by mutableStateOf(0L)
        private set

    var durationTrigger by mutableStateOf(0)
        private set

    var currentAudioRoute by mutableStateOf(CallAudioState.ROUTE_EARPIECE)
        private set

    var isMuted by mutableStateOf(false)
        private set


    var isOnHold by mutableStateOf(false)
        private set

    var showKeypad by mutableStateOf(false)
        private set

    var callWasRejectedByUser by mutableStateOf(false)
        private set

    var previousActiveState by mutableStateOf<Int?>(null)
        private set

    private var currentCall: Call? = null

    private val inCallService: MyInCallService?
        get() = MyInCallService.getInstance()

    fun initialize(call: Call, context: Context) {
        currentCall = call

        callState = call.state
        connectTimeMillis = call.details.connectTimeMillis
        updateDurationTrigger()  // Initial trigger

        previousActiveState = when (call.state) {
            Call.STATE_RINGING -> Call.STATE_RINGING
            in listOf(Call.STATE_ACTIVE, Call.STATE_HOLDING, Call.STATE_DIALING, Call.STATE_PULLING_CALL) -> Call.STATE_ACTIVE
            else -> null
        }

        val rawNumber = call.details.handle?.schemeSpecificPart ?: "Private"
        displayName = if (rawNumber == "Private") {
            "Private"
        } else {
            lookupContactName(context, rawNumber) ?: rawNumber
        }

        registerCallCallback(call)
        startAudioStatePolling()
    }

    private fun registerCallCallback(call: Call) {
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, newState: Int) {
                callState = newState
                isOnHold = newState == Call.STATE_HOLDING

                if (newState == Call.STATE_RINGING) {
                    previousActiveState = Call.STATE_RINGING
                    callWasRejectedByUser = false
                } else if (newState in listOf(
                        Call.STATE_ACTIVE,
                        Call.STATE_HOLDING,
                        Call.STATE_DIALING,
                        Call.STATE_PULLING_CALL
                    )
                ) {
                    previousActiveState = Call.STATE_ACTIVE
                }

                updateDurationTrigger()  // Critical: force timer restart on state change
                showToastForState(newState)
            }
        }
        call.registerCallback(callback)
    }

    private fun updateDurationTrigger() {
        durationTrigger += 1  // Increment to force produceState recomputation
    }

    private fun startAudioStatePolling() {
        viewModelScope.launch {
            while (true) {
                MyInCallService.currentAudioState?.let { audioState ->
                    isMuted = audioState.isMuted
                    currentAudioRoute = audioState.route
                }
                delay(300)
            }
        }
    }

    private fun showToastForState(state: Int) {
        val context = inCallService?.applicationContext ?: return
        val text = when (state) {
            Call.STATE_NEW -> "New call"
            Call.STATE_DIALING -> "Dialing..."
            Call.STATE_RINGING -> "Incoming call"
            Call.STATE_HOLDING -> "Call on hold"
            Call.STATE_ACTIVE -> "Call connected"
            Call.STATE_DISCONNECTED -> "Call ended"
            Call.STATE_SELECT_PHONE_ACCOUNT -> "Select phone account"
            Call.STATE_CONNECTING -> "Connecting..."
            Call.STATE_DISCONNECTING -> "Disconnecting..."
            Call.STATE_PULLING_CALL -> "Pulling call..."
            Call.STATE_AUDIO_PROCESSING -> "Audio processing"
            Call.STATE_SIMULATED_RINGING -> "Simulated ringing"
            else -> "Unknown state"
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    fun toggleMute() {
        val newMute = !isMuted
        inCallService?.setMuted(newMute)
        isMuted = newMute
    }

    fun cycleAudioRoute(supportedRouteMask: Int) {
        val routes = buildList {
            if (supportedRouteMask and CallAudioState.ROUTE_EARPIECE != 0) add(CallAudioState.ROUTE_EARPIECE)
            if (supportedRouteMask and CallAudioState.ROUTE_SPEAKER != 0) add(CallAudioState.ROUTE_SPEAKER)
            if (supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) add(CallAudioState.ROUTE_WIRED_HEADSET)
            if (supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH != 0) add(CallAudioState.ROUTE_BLUETOOTH)
        }

        if (routes.size > 1) {
            val currentIndex = routes.indexOf(currentAudioRoute)
            val nextIndex = (currentIndex + 1) % routes.size
            val nextRoute = routes[nextIndex]
            inCallService?.setAudioRoute(nextRoute)
            currentAudioRoute = nextRoute
        }
    }

    fun toggleHold() {
        currentCall?.let {
            if (isOnHold) it.unhold() else it.hold()
        }
    }

    fun toggleKeypad() {
        showKeypad = !showKeypad
    }

    fun setUserRejectedCall() {
        callWasRejectedByUser = true
    }

    fun formatDuration(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun lookupContactName(context: Context, phoneNumber: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
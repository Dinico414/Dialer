package com.xenonware.phone.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.phone.MyInCallService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class CallScreenViewModel(private val context: Context, private val call: Call?) : ViewModel() {

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    var displayName by mutableStateOf("Unknown")
        private set

    var callState by mutableStateOf(call?.state ?: Call.STATE_DISCONNECTED)
        private set

    var isMuted by mutableStateOf(false)
        private set

    var currentAudioRoute by mutableStateOf(CallAudioState.ROUTE_EARPIECE)
        private set

    var isOnHold by mutableStateOf(false)
        private set

    var showKeypad by mutableStateOf(false)

    var callWasRejectedByUser by mutableStateOf(false)

    init {
        call?.let { setupCall(it) }
        resolveContactName()
        startDurationTimer()
    }

    private fun setupCall(call: Call) {
        call.registerCallback(callCallback)

        MyInCallService.getInstance()?.let { service ->
            val audioState = MyInCallService.currentAudioState ?: service.callAudioState
            audioState?.let {
                isMuted = it.isMuted
                currentAudioRoute = it.route
            }
        }
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            callState = state
            isOnHold = state == Call.STATE_HOLDING
            showToast(state)
            if (state == Call.STATE_ACTIVE) startDurationTimer()
        }
    }

    private fun startDurationTimer() {
        viewModelScope.launch {
            if (callState == Call.STATE_ACTIVE && call?.details?.connectTimeMillis ?: 0 > 0) {
                while (true) {
                    _duration.value =
                        System.currentTimeMillis() - (call?.details?.connectTimeMillis ?: 0)
                    delay(1000)
                }
            }
        }
    }

    private fun resolveContactName() {
        val rawNumber = call?.details?.handle?.schemeSpecificPart ?: "Private"
        displayName = if (rawNumber == "Private") "Private" else {
            lookupContactName(context, rawNumber) ?: rawNumber
        }
    }

    @SuppressLint("Recycle")
    private fun lookupContactName(context: Context, phoneNumber: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun showToast(state: Int) {
        val text = when (state) {
            Call.STATE_RINGING -> "Incoming call"
            Call.STATE_DIALING -> "Dialing..."
            Call.STATE_ACTIVE -> "Call connected"
            Call.STATE_HOLDING -> "Call on hold"
            Call.STATE_DISCONNECTED -> "Call ended"
            else -> "Call state changed"
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    fun toggleMute() {
        val service = MyInCallService.getInstance() ?: return
        val newMute = !isMuted
        service.setMuted(newMute)
        isMuted = newMute
    }

    fun cycleAudioRoute() {
        val service = MyInCallService.getInstance() ?: return
        val supported = service.callAudioState?.supportedRouteMask ?: return
        val options = listOfNotNull(
            CallAudioState.ROUTE_EARPIECE.takeIf { supported and it != 0 },
            CallAudioState.ROUTE_SPEAKER.takeIf { supported and it != 0 },
            CallAudioState.ROUTE_WIRED_HEADSET.takeIf { supported and it != 0 },
            CallAudioState.ROUTE_BLUETOOTH.takeIf { supported and it != 0 })
        if (options.size > 1) {
            val next = options[(options.indexOf(currentAudioRoute) + 1) % options.size]
            service.setAudioRoute(next)
            currentAudioRoute = next
        }
    }

    fun toggleHold() {
        if (isOnHold) call?.unhold() else call?.hold()
    }

    fun rejectCall() {
        callWasRejectedByUser = true
        call?.reject(false, null)
    }

    fun answerCall() {
        fun answerCall() {
            call?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }
    }

    fun disconnectCall() {
        call?.disconnect()
    }

    fun playDtmfTone(tone: Char) {
        call?.playDtmfTone(tone)
        Handler(Looper.getMainLooper()).postDelayed({
            call?.stopDtmfTone()
        }, 150)
    }

    override fun onCleared() {
        call?.unregisterCallback(callCallback)
        super.onCleared()
    }
}

fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return "%02d:%02d".format(minutes, seconds)
}
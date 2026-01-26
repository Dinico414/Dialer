package com.xenonware.phone.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.xenonware.phone.service.MyInCallService
import com.xenonware.phone.util.PhoneNumberFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class CallScreenViewModel : ViewModel() {

    private var currentCall: Call? = null

    private val inCallService: MyInCallService?
        get() = MyInCallService.getInstance()

    private val _callState = MutableStateFlow<Int?>(null)
    val callState: StateFlow<Int?> = _callState.asStateFlow()

    private val _previousActiveState = MutableStateFlow<Int?>(null)
    val previousActiveState: StateFlow<Int?> = _previousActiveState.asStateFlow()

    private val _callWasRejectedByUser = MutableStateFlow(false)
    val callWasRejectedByUser: StateFlow<Boolean> = _callWasRejectedByUser.asStateFlow()

    private val _displayName = MutableStateFlow("Unknown")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _currentAudioRoute = MutableStateFlow(CallAudioState.ROUTE_EARPIECE)
    val currentAudioRoute: StateFlow<Int> = _currentAudioRoute.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isOnHold = MutableStateFlow(false)
    val isOnHold: StateFlow<Boolean> = _isOnHold.asStateFlow()

    private val _showKeypad = MutableStateFlow(false)
    val showKeypad: StateFlow<Boolean> = _showKeypad.asStateFlow()

    private val _isVoicemailCall = MutableStateFlow(false)
    val isVoicemailCall: StateFlow<Boolean> = _isVoicemailCall.asStateFlow()

    private var voicemailNumber: String? = null

    fun initialize(call: Call, context: Context) {
        currentCall = call

        val state = getCallState(call)
        _callState.value = state

        _previousActiveState.value = when (state) {
            Call.STATE_RINGING -> Call.STATE_RINGING
            in listOf(Call.STATE_ACTIVE, Call.STATE_HOLDING, Call.STATE_DIALING, Call.STATE_PULLING_CALL) -> Call.STATE_ACTIVE
            else -> null
        }

        val rawNumber = call.details.handle?.schemeSpecificPart ?: "Private"

        _displayName.value = if (rawNumber == "Private") {
            "Private"
        } else {
            lookupContactName(context, rawNumber)
                ?: PhoneNumberFormatter.formatForDisplay(rawNumber, context)
        }

        loadVoicemailNumber(context)
        checkIfVoicemailCall()

        registerCallCallback(call)
    }

    fun formatDuration(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    fun setUserRejectedCall() {
        _callWasRejectedByUser.value = true
    }

    fun cycleAudioRoute(supportedRouteMask: Int) {
        val routes = buildList {
            if (supportedRouteMask and CallAudioState.ROUTE_EARPIECE != 0) add(CallAudioState.ROUTE_EARPIECE)
            if (supportedRouteMask and CallAudioState.ROUTE_SPEAKER != 0) add(CallAudioState.ROUTE_SPEAKER)
            if (supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) add(CallAudioState.ROUTE_WIRED_HEADSET)
            if (supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH != 0) add(CallAudioState.ROUTE_BLUETOOTH)
        }

        if (routes.size > 1) {
            val currentIndex = routes.indexOf(_currentAudioRoute.value)
            val nextIndex = (currentIndex + 1) % routes.size
            val nextRoute = routes[nextIndex]

            inCallService?.setAudioRoute(nextRoute)

            _currentAudioRoute.value = nextRoute
        }
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        inCallService?.setMuted(newMute)
        _isMuted.value = newMute
    }

    fun toggleHold() {
        currentCall?.let {
            if (_isOnHold.value) it.unhold() else it.hold()
        }
    }

    fun toggleKeypad() {
        _showKeypad.value = !_showKeypad.value
    }

    private fun loadVoicemailNumber(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_PHONE_STATE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            val tm = context.getSystemService(TelephonyManager::class.java)
            voicemailNumber = tm?.voiceMailNumber?.trim()?.takeIf { it.isNotBlank() }
        }

        if (voicemailNumber.isNullOrBlank()) {
            voicemailNumber = "*86"
        }
    }

    private fun checkIfVoicemailCall() {
        val dialed = currentCall?.details?.handle?.schemeSpecificPart?.trim() ?: ""
        val vm = voicemailNumber?.trim() ?: ""
        _isVoicemailCall.value = dialed.isNotBlank() && vm.isNotBlank() &&
                (dialed == vm || dialed.endsWith(vm) || dialed == "*86")
    }


    private fun getCallState(call: Call): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details.state
        } else {
            call.state
        }
    }

    private fun registerCallCallback(call: Call) {
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, newState: Int) {
                _callState.value = newState
                _isOnHold.value = newState == Call.STATE_HOLDING

                if (newState == Call.STATE_RINGING) {
                    _previousActiveState.value = Call.STATE_RINGING
                    _callWasRejectedByUser.value = false
                } else if (newState in listOf(
                        Call.STATE_ACTIVE,
                        Call.STATE_HOLDING,
                        Call.STATE_DIALING,
                        Call.STATE_PULLING_CALL
                    )
                ) {
                    _previousActiveState.value = Call.STATE_ACTIVE
                }
                checkIfVoicemailCall()
                showToastForState(newState)
            }

            override fun onDetailsChanged(call: Call, details: Call.Details) {
                val newState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    details.state
                } else {
                    call.state
                }
                _callState.value = newState
                _isOnHold.value = newState == Call.STATE_HOLDING

                if (newState == Call.STATE_RINGING) {
                    _previousActiveState.value = Call.STATE_RINGING
                    _callWasRejectedByUser.value = false
                } else if (newState in listOf(
                        Call.STATE_ACTIVE,
                        Call.STATE_HOLDING,
                        Call.STATE_DIALING,
                        Call.STATE_PULLING_CALL
                    )
                ) {
                    _previousActiveState.value = Call.STATE_ACTIVE
                }
                checkIfVoicemailCall()
                showToastForState(newState)
            }
        }
        call.registerCallback(callback)
    }

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

    private fun showToastForState(state: Int) {
        val showToast = false
        if (showToast) {
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
    }
}
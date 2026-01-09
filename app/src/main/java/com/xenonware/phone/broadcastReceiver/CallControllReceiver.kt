package com.xenonware.phone.broadcastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import com.xenonware.phone.CallScreenActivity
import com.xenonware.phone.helper.CallNotificationHelper
import com.xenonware.phone.service.MyInCallService

class CallControlReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ANSWER_CALL = "com.xenonware.phone.ACTION_ANSWER_CALL"
        const val ACTION_REJECT_CALL = "com.xenonware.phone.ACTION_REJECT_CALL"
        const val ACTION_TOGGLE_MUTE = "com.xenonware.phone.ACTION_TOGGLE_MUTE"
        const val ACTION_CYCLE_AUDIO_ROUTE = "com.xenonware.phone.ACTION_CYCLE_AUDIO_ROUTE"
        const val ACTION_HANG_UP = "com.xenonware.phone.ACTION_HANG_UP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val service = MyInCallService.getInstance() ?: return

        when (intent.action) {
            ACTION_ANSWER_CALL -> {
                MyInCallService.currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)

                val launchIntent = Intent(context, CallScreenActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(launchIntent)
            }

            ACTION_REJECT_CALL -> {
                MyInCallService.currentCall?.reject(false, null)
                CallScreenActivity.currentCall = null
            }

            ACTION_TOGGLE_MUTE -> {
                val muted = MyInCallService.currentAudioState?.isMuted ?: false
                service.setMuted(!muted)

                MyInCallService.currentCall?.let { call ->
                    CallNotificationHelper.showOngoingCallNotification(context, call)
                }
            }

            ACTION_CYCLE_AUDIO_ROUTE -> {
                val audioState = MyInCallService.currentAudioState ?: return
                val mask = audioState.supportedRouteMask
                val routes = mutableListOf<Int>().apply {
                    if (mask and CallAudioState.ROUTE_EARPIECE != 0) add(CallAudioState.ROUTE_EARPIECE)
                    if (mask and CallAudioState.ROUTE_BLUETOOTH != 0) add(CallAudioState.ROUTE_BLUETOOTH)
                    if (mask and CallAudioState.ROUTE_WIRED_HEADSET != 0) add(CallAudioState.ROUTE_WIRED_HEADSET)
                    if (mask and CallAudioState.ROUTE_SPEAKER != 0) add(CallAudioState.ROUTE_SPEAKER)
                }
                if (routes.size > 1) {
                    val currentIndex = routes.indexOf(audioState.route).coerceAtLeast(0)
                    val next = routes[(currentIndex + 1) % routes.size]
                    service.setAudioRoute(next)
                }

                MyInCallService.currentCall?.let { call ->
                    CallNotificationHelper.showOngoingCallNotification(context, call)
                }
            }

            ACTION_HANG_UP -> {
                val callHandle = intent.getStringExtra("call_handle") ?: return

                val calls = service.calls
                val targetCall = calls.find {
                    it.details.handle?.schemeSpecificPart == callHandle
                }

                if (targetCall != null) {
                    targetCall.disconnect()

                    val activityIntent = Intent(context, CallScreenActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    context.startActivity(activityIntent)
                }
            }
        }
    }
}
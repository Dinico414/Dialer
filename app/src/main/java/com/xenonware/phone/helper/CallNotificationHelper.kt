package com.xenonware.phone.helper

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service.STOP_FOREGROUND_REMOVE
import android.content.Context
import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.xenonware.phone.CallScreenActivity
import com.xenonware.phone.R
import com.xenonware.phone.broadcastReceiver.CallControlReceiver
import com.xenonware.phone.service.MyInCallService

object CallNotificationHelper {

    private const val ONGOING_CALL_CHANNEL_ID = "ongoing_call_channel"
    private const val ONGOING_CALL_NOTIFICATION_ID = 100

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            ONGOING_CALL_CHANNEL_ID,
            "Ongoing Calls",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setBypassDnd(true)
            description = "Active phone call"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    @SuppressLint("FullScreenIntentPolicy")
    fun showOngoingCallNotification(context: Context, call: Call, useNewLayout: Boolean) {
        createNotificationChannel(context)

        val handle = call.details.handle?.schemeSpecificPart ?: "Unknown"
        val person = Person.Builder().setName(handle).build()

        val targetClass = if (useNewLayout) CallScreenActivity::class.java
        else com.xenonware.phone.ui.layouts.callscreen.CallScreenActivity::class.java

        val contentIntent = Intent(context, targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 1, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mute
        val isMuted = MyInCallService.currentAudioState?.isMuted ?: false
        val muteIcon = if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic_on
        val muteLabel = if (isMuted) "Unmute" else "Mute"

        val muteIntent = Intent(context, CallControlReceiver::class.java).apply {
            action = CallControlReceiver.ACTION_TOGGLE_MUTE
        }
        val mutePI = PendingIntent.getBroadcast(context, 2, muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Speaker
        val currentRoute = MyInCallService.currentAudioState?.route ?: CallAudioState.ROUTE_EARPIECE
        val isSpeakerOn = currentRoute == CallAudioState.ROUTE_SPEAKER
        val speakerIcon = if (isSpeakerOn) R.drawable.ic_speaker_on else R.drawable.ic_speaker_off
        val speakerLabel = if (isSpeakerOn) "Speaker Off" else "Speaker"

        val speakerIntent = Intent(context, CallControlReceiver::class.java).apply {
            action = CallControlReceiver.ACTION_CYCLE_AUDIO_ROUTE
        }
        val speakerPI = PendingIntent.getBroadcast(context, 3, speakerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val callHandle = call.details.handle?.schemeSpecificPart ?: "Hang Up"

        val hangupIntent = Intent(context, CallControlReceiver::class.java).apply {
            action = CallControlReceiver.ACTION_HANG_UP
            putExtra("call_handle", callHandle)
            putExtra("use_new_layout", useNewLayout)
        }
        val hangupPI = PendingIntent.getBroadcast(
            context, 4, hangupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ONGOING_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_ongoing)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(contentPendingIntent, true)
            .addAction(muteIcon, muteLabel, mutePI)
            .addAction(speakerIcon, speakerLabel, speakerPI)
            .setStyle(NotificationCompat.CallStyle.forOngoingCall(person, hangupPI))
            .build()

        if (context is MyInCallService) {
            context.startForeground(ONGOING_CALL_NOTIFICATION_ID, notification)
        } else {
            context.getSystemService(NotificationManager::class.java)
                .notify(ONGOING_CALL_NOTIFICATION_ID, notification)
        }
    }

    fun dismissOngoingCallNotification(context: Context) {
        if (context is MyInCallService) {
            context.stopForeground(STOP_FOREGROUND_REMOVE)
        }
        context.getSystemService(NotificationManager::class.java)
            .cancel(ONGOING_CALL_NOTIFICATION_ID)
    }
}
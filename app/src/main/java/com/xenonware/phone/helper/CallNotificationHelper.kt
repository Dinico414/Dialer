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

    fun createOngoingNotificationChannel(context: Context) {
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
        createOngoingNotificationChannel(context)

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

        // Mute action (always shown)
        val isMuted = MyInCallService.currentAudioState?.isMuted ?: false
        val muteIcon = if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic_on
        val muteLabel = if (isMuted) "Unmute" else "Mute"

        val muteIntent = Intent(context, CallControlReceiver::class.java).apply {
            action = CallControlReceiver.ACTION_TOGGLE_MUTE
        }
        val mutePI = PendingIntent.getBroadcast(
            context, 2, muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Hang up action
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

        val notificationBuilder = NotificationCompat.Builder(context, ONGOING_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(contentPendingIntent, true)
            .addAction(muteIcon, muteLabel, mutePI)
            .setStyle(NotificationCompat.CallStyle.forOngoingCall(person, hangupPI))

        // Audio Route (Speaker/Headset/Bluetooth/Earpiece) action
        MyInCallService.currentAudioState?.let { audioState ->
            if (Integer.bitCount(audioState.supportedRouteMask) > 1) {
                val currentRoute = audioState.route

                val (speakerIcon, speakerLabel) = when (currentRoute) {
                    CallAudioState.ROUTE_SPEAKER -> {
                        R.drawable.ic_speaker_on to "Speaker Off"
                    }
                    CallAudioState.ROUTE_WIRED_HEADSET -> {
                        R.drawable.ic_headset to "Wired Headset"
                    }
                    CallAudioState.ROUTE_BLUETOOTH -> {
                        R.drawable.ic_bluetooth to "Bluetooth"
                    }
                    else -> {
                        R.drawable.ic_earpiece to "Earpiece"
                    }
                }

                val speakerIntent = Intent(context, CallControlReceiver::class.java).apply {
                    action = CallControlReceiver.ACTION_CYCLE_AUDIO_ROUTE
                }
                val speakerPI = PendingIntent.getBroadcast(
                    context, 3, speakerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                notificationBuilder.addAction(speakerIcon, speakerLabel, speakerPI)
            }
        }

        val notification = notificationBuilder.build()

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

    private const val INCOMING_CALL_CHANNEL_ID = "incoming_call_channel"
    private const val INCOMING_CALL_NOTIFICATION_ID = 101

    fun createIncomingCallChannel(context: Context) {
        val channel = NotificationChannel(
            INCOMING_CALL_CHANNEL_ID,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setBypassDnd(true)
            description = "Incoming phone calls"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    @SuppressLint("FullScreenIntentPolicy")
    fun showIncomingCallNotification(context: Context, call: Call, useNewLayout: Boolean) {
        createIncomingCallChannel(context)

        val handle = call.details.handle?.schemeSpecificPart ?: "Unknown"
        val person = Person.Builder().setName(handle).build()

        val targetClass = if (useNewLayout) CallScreenActivity::class.java
        else com.xenonware.phone.ui.layouts.callscreen.CallScreenActivity::class.java

        val contentIntent = Intent(context, targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 100, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Answer action
        val answerIntent = Intent(context, CallControlReceiver::class.java).apply {
            action = CallControlReceiver.ACTION_ANSWER_CALL
            putExtra("use_new_layout", useNewLayout)
        }
        val answerPI = PendingIntent.getBroadcast(
            context, 101, answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reject action
        val rejectIntent = Intent(context, CallControlReceiver::class.java).apply {
            action = CallControlReceiver.ACTION_REJECT_CALL
            putExtra("use_new_layout", useNewLayout)
        }
        val rejectPI = PendingIntent.getBroadcast(
            context, 102, rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, INCOMING_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_incoming) // Use your own rounded icon here!
            .setContentTitle("Incoming call")
            .setContentText(handle)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Better for heads-up
            .setFullScreenIntent(contentPendingIntent, true)
            .setOngoing(true)
            .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, rejectPI, answerPI))
        val notification = notificationBuilder.build()

        if (context is MyInCallService) {
            context.startForeground(INCOMING_CALL_NOTIFICATION_ID, notification)
        } else {
            context.getSystemService(NotificationManager::class.java)
                .notify(INCOMING_CALL_NOTIFICATION_ID, notification)
        }
    }

    fun dismissIncomingCallNotification(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(INCOMING_CALL_NOTIFICATION_ID)
    }
}
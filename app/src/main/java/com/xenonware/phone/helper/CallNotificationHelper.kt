package com.xenonware.phone.helper

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service.STOP_FOREGROUND_REMOVE
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
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
    fun showOngoingCallNotification(context: Context, call: Call) {
        createOngoingNotificationChannel(context)

        val rawHandle = call.details.handle?.schemeSpecificPart ?: "Unknown"

        val displayName = when (rawHandle) {
            "Private" -> "Private"
            "Unknown" -> "Unknown"
            else -> lookupContactName(context, rawHandle) ?: rawHandle
        }

        val person = Person.Builder()
            .setName(displayName)
            .build()

        val contentIntent = Intent(context, CallScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 1, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mute action
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
        val hangupIntent = Intent(context, CallControlReceiver::class.java).apply {
            action = CallControlReceiver.ACTION_HANG_UP
            putExtra("call_handle", rawHandle)
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
            .setWhen(call.details.connectTimeMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(false)
            .setShowWhen(true)
            .setStyle(NotificationCompat.CallStyle.forOngoingCall(person, hangupPI))

        // Audio route action (speaker/headset/etc.)
        MyInCallService.currentAudioState?.let { audioState ->
            if (Integer.bitCount(audioState.supportedRouteMask) > 1) {
                val currentRoute = audioState.route

                val (speakerIcon, speakerLabel) = when (currentRoute) {
                    CallAudioState.ROUTE_SPEAKER -> R.drawable.ic_speaker_on to "Speaker Off"
                    CallAudioState.ROUTE_WIRED_HEADSET -> R.drawable.ic_headset to "Wired Headset"
                    CallAudioState.ROUTE_BLUETOOTH -> R.drawable.ic_bluetooth to "Bluetooth"
                    else -> R.drawable.ic_earpiece to "Earpiece"  // Usually means switch to speaker next
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
    fun showIncomingCallNotification(context: Context, call: Call) {
        createIncomingCallChannel(context)

        val rawHandle = call.details.handle?.schemeSpecificPart ?: "Unknown"

        val callerName = lookupContactName(context, rawHandle) ?: rawHandle

        val person = Person.Builder()
            .setName(callerName)
            .build()

        val subtitleText = if (callerName != rawHandle && rawHandle != "Unknown") rawHandle else null

        val contentIntent = Intent(context, CallScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 100, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerIntent = Intent(context, CallControlReceiver::class.java).apply {
            action = CallControlReceiver.ACTION_ANSWER_CALL
        }
        val answerPI = PendingIntent.getBroadcast(
            context, 101, answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rejectIntent = Intent(context, CallControlReceiver::class.java).apply {
            action = CallControlReceiver.ACTION_REJECT_CALL
        }
        val rejectPI = PendingIntent.getBroadcast(
            context, 102, rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, INCOMING_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_incoming)
            .setContentTitle("Incoming call")
            .setContentText(subtitleText)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(contentPendingIntent, true)
            .setOngoing(true)
            .setWhen(0)
            .setShowWhen(false)
            .setUsesChronometer(false)
            .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, rejectPI, answerPI))

        val notification = notificationBuilder.build()

        if (context is MyInCallService) {
            context.startForeground(INCOMING_CALL_NOTIFICATION_ID, notification)
        } else {
            context.getSystemService(NotificationManager::class.java)
                .notify(INCOMING_CALL_NOTIFICATION_ID, notification)
        }
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

    fun dismissIncomingCallNotification(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(INCOMING_CALL_NOTIFICATION_ID)
    }
}
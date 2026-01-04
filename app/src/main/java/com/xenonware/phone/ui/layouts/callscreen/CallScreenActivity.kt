package com.xenonware.phone.ui.layouts.callscreen

import android.os.Bundle
import android.telecom.Call
import android.telecom.VideoProfile
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenonware.phone.data.SharedPreferenceManager
import com.xenonware.phone.ui.theme.ScreenEnvironment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

class CallScreenActivity : ComponentActivity() {

    companion object {
        var currentCall: Call? = null
    }

    private var shouldFinishAfterDelay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContent {
            val sharedPreferenceManager = SharedPreferenceManager(applicationContext)

            val themePreference = sharedPreferenceManager.theme
            val blackedOutModeEnabled = sharedPreferenceManager.blackedOutModeEnabled

            val containerSize = LocalWindowInfo.current.containerSize
            val applyCoverTheme = sharedPreferenceManager.isCoverThemeApplied(containerSize)

            ScreenEnvironment(
                themePreference = themePreference,
                coverTheme = applyCoverTheme,
                blackedOutModeEnabled = blackedOutModeEnabled
            ) { layoutType, isLandscape ->

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorScheme.surfaceContainer
                ) {
                    CallScreen(call = currentCall)
                }
            }
        }

        currentCall?.let { call ->
            call.registerCallback(object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    if ((state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING)
                        && !shouldFinishAfterDelay
                    ) {
                        shouldFinishAfterDelay = true
                        window.decorView.postDelayed({
                            if (!isFinishing && !isDestroyed) {
                                finish()
                            }
                        }, 2000)
                    }
                }
            })
        }
    }
}

@Composable
fun CallScreen(call: Call?) {
    if (call == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active call", fontSize = 32.sp, color = colorScheme.onSurface)
        }
        return
    }

    var state by remember { mutableStateOf(call.state) }

    DisposableEffect(call) {
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, newState: Int) {
                state = newState
            }
        }
        call.registerCallback(callback)
        onDispose { call.unregisterCallback(callback) }
    }

    val handle = call.details.handle?.schemeSpecificPart ?: "Unknown number"

    // Duration timer – only when truly ACTIVE
    val duration by produceState(0L) {
        while (state == Call.STATE_ACTIVE) {
            value = System.currentTimeMillis() - (call.details.connectTimeMillis
                ?: System.currentTimeMillis())
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(100.dp))

        // Caller info + status/timer
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = handle, fontSize = 48.sp, color = colorScheme.onSurface)
            Spacer(Modifier.height(32.dp))

            val statusText = when (state) {
                Call.STATE_RINGING -> "Incoming call..."
                Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_PULLING_CALL -> "Calling..."
                Call.STATE_ACTIVE -> formatDuration(duration)
                Call.STATE_DISCONNECTED -> if (duration > 0) "Call ended" else "Call declined/failed"
                else -> "Unknown state"
            }

            Text(text = statusText, fontSize = 32.sp, color = colorScheme.onSurface)
        }

        // Bottom controls – different layouts per state
        CallControls(state = state, call = call)

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun CallControls(state: Int, call: Call) {
    when (state) {

// Inside CallControls, for Call.STATE_RINGING:
        Call.STATE_RINGING -> {
            val trackWidthDp = 360.dp
            val iconSizeDp = 52.dp

            var offsetX by remember { mutableStateOf(0f) }

            val density = LocalDensity.current
            val trackWidthPx = with(density) { trackWidthDp.toPx() }
            val iconSizePx = with(density) { iconSizeDp.toPx() }

            // Allow full free movement
            (trackWidthPx - iconSizePx) / 2 + 40f

            val draggableState = rememberDraggableState { delta ->
                offsetX += delta
            }

            // Strong rotational shake when near center
            val infiniteTransition = rememberInfiniteTransition()
            val shakeRotation by infiniteTransition.animateFloat(
                initialValue = -28f, targetValue = 28f, animationSpec = infiniteRepeatable(
                    animation = tween(200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            // Rotation logic: hanging down by default, rotates based on drag direction
            val targetRotation = when {
                offsetX > 80f -> -120f     // clearly swiped right → upright
                offsetX < -80f -> 0f  // clearly swiped left → facing down
                else -> 0f            // hanging down
            }

            val rotation by animateFloatAsState(
                targetValue = if (abs(offsetX) < 120f) targetRotation + shakeRotation else targetRotation,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow
                )
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(136.dp)
                    .background(colorScheme.surfaceDim, CircleShape)
                    .padding(horizontal = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                // Left: Decline hint
                Icon(
                    Icons.Rounded.CallEnd,
                    contentDescription = null,
                    tint = Color(0xFFFB4F43),
                    modifier = Modifier
                        .size(iconSizeDp)
                        .align(Alignment.CenterStart)
                )

                // Right: Accept hint
                Icon(
                    Icons.Rounded.Phone,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier
                        .size(iconSizeDp)
                        .align(Alignment.CenterEnd)
                )

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .offset { IntOffset(offsetX.roundToInt(), 0) }
                        .background(colorScheme.surfaceBright, CircleShape)
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Horizontal,
                            onDragStopped = { velocity ->
                                val swipedRight = offsetX > 80f || velocity > 500f
                                val swipedLeft = offsetX < -80f || velocity < -500f

                                when {
                                    swipedRight -> {
                                        call.answer(VideoProfile.STATE_AUDIO_ONLY)
                                        offsetX = trackWidthPx / 2 + 100f
                                    }

                                    swipedLeft -> {
                                        call.reject(false, null)
                                        offsetX = -trackWidthPx / 2 - 100f
                                    }

                                    else -> {
                                        offsetX = 0f
                                    }
                                }

                                if (swipedRight || swipedLeft) {
                                    kotlinx.coroutines.MainScope().launch {
                                        delay(600)
                                        offsetX = 0f
                                    }
                                }
                            })

                ) {
                    // Draggable phone icon
                    Icon(
                        Icons.Rounded.CallEnd,
                        contentDescription = "Swipe left to decline, right to accept",
                        tint = colorScheme.onSurface,
                        modifier = Modifier
                            .size(iconSizeDp)
                            .align(Alignment.Center)
                            .rotate(rotation)
                    )
                }
            }
        }

        Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_PULLING_CALL -> {
            // Outgoing call in progress: only Cancel
            Button(
                onClick = { call.disconnect() },
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                modifier = Modifier.size(width = 200.dp, height = 80.dp)
            ) {
                Text("Cancel", fontSize = 32.sp, color = colorScheme.onError)
            }
        }

        Call.STATE_ACTIVE -> {
            // Active call: big red Hang Up
            Button(
                onClick = { call.disconnect() },
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                modifier = Modifier.size(width = 200.dp, height = 80.dp)
            ) {
                Text("Hang Up", fontSize = 32.sp, color = colorScheme.onError)
            }

            // Optional: Add more in-call controls here later (speaker, mute, hold, etc.)
        }

        Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
            Text("Call ended", fontSize = 32.sp, color = colorScheme.onSurface)
        }

        else -> {
            // Fallback
            Button(
                onClick = { call.disconnect() },
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                modifier = Modifier.size(width = 200.dp, height = 80.dp)
            ) {
                Text("End", fontSize = 32.sp, color = colorScheme.onError)
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
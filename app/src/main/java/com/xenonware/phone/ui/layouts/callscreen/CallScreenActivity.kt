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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenonware.phone.ui.theme.XenonTheme
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
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContent {
            // Use the new XenonTheme instead of the old DialerTheme
            // We force a blacked-out dark theme for the call screen (pure black background, typical for in-call UI)
            XenonTheme(
                darkTheme = true,
                useBlackedOutDarkTheme = true,
                isCoverMode = false,
                dynamicColor = true // You can set to false if you prefer static colors
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    // Override background to ensure pure black (in case blacked-out mode doesn't fully cover it)
                    color = Color.Black
                ) {
                    CallScreen(call = currentCall)
                }
            }
        }

        // Auto-close activity 2 seconds after call fully ends
        currentCall?.let { call ->
            call.registerCallback(object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    if ((state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) && !shouldFinishAfterDelay) {
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
            Text("No active call", fontSize = 32.sp, color = Color.White)
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
            Text(text = handle, fontSize = 48.sp, color = Color.White)
            Spacer(Modifier.height(32.dp))

            val statusText = when (state) {
                Call.STATE_RINGING -> "Incoming call..."
                Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_PULLING_CALL -> "Calling..."
                Call.STATE_ACTIVE -> formatDuration(duration)
                Call.STATE_DISCONNECTED -> if (duration > 0) "Call ended" else "Call declined/failed"
                else -> "Unknown state"
            }

            Text(text = statusText, fontSize = 32.sp, color = Color.White)
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
            val iconSizeDp = 100.dp

            var offsetX by remember { mutableStateOf(0f) }

            val density = LocalDensity.current
            val trackWidthPx = with(density) { trackWidthDp.toPx() }
            val iconSizePx = with(density) { iconSizeDp.toPx() }

            // Allow full free movement
            val maxOffset = (trackWidthPx - iconSizePx) / 2 + 40f  // slight overshoot allowed

            val draggableState = rememberDraggableState { delta ->
                offsetX += delta
            }

            // Strong rotational shake when near center
            val infiniteTransition = rememberInfiniteTransition()
            val shakeRotation by infiniteTransition.animateFloat(
                initialValue = -28f,
                targetValue = 28f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            // Rotation logic: hanging down by default, rotates based on drag direction
            val targetRotation = when {
                offsetX > 80f -> 0f     // clearly swiped right → upright
                offsetX < -80f -> 180f  // clearly swiped left → facing down
                else -> 270f            // hanging down
            }

            val rotation by animateFloatAsState(
                targetValue = if (abs(offsetX) < 120f) targetRotation + shakeRotation else targetRotation,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
            )

            Box(
                modifier = Modifier
                    .width(trackWidthDp)
                    .height(180.dp)
                    .background(Color(0xFF111111), CircleShape)
                    .padding(horizontal = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                // Left: Decline hint
                Icon(
                    Icons.Rounded.CallEnd,
                    contentDescription = null,
                    tint = Color.Red.copy(alpha = 0.4f + (abs(offsetX.coerceAtMost(0f)) / trackWidthPx) * 0.6f),
                    modifier = Modifier.size(iconSizeDp).align(Alignment.CenterStart)
                )

                // Right: Accept hint
                Icon(
                    Icons.Rounded.Phone,
                    contentDescription = null,
                    tint = Color(0xFF00C853).copy(alpha = 0.4f + (offsetX.coerceAtLeast(0f) / trackWidthPx) * 0.6f),
                    modifier = Modifier.size(iconSizeDp).align(Alignment.CenterEnd)
                )

                // Draggable phone icon
                Icon(
                    Icons.Rounded.Phone,
                    contentDescription = "Swipe left to decline, right to accept",
                    tint = Color.White,
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), 0) }
                        .size(iconSizeDp)
                        .background(Color(0xFF333333), CircleShape)
                        .padding(24.dp)
                        .rotate(rotation)
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Horizontal,
                            onDragStopped = { velocity ->
                                // Decide based on final position (no velocity needed, but we still support fling feel)
                                val swipedRight = offsetX > 80f || velocity > 500f
                                val swipedLeft = offsetX < -80f || velocity < -500f

                                when {
                                    swipedRight -> {
                                        call.answer(VideoProfile.STATE_AUDIO_ONLY)
                                        offsetX = trackWidthPx / 2 + 100f  // fly off right
                                    }
                                    swipedLeft -> {
                                        call.reject(false, null)
                                        offsetX = -trackWidthPx / 2 - 100f  // fly off left
                                    }
                                    else -> {
                                        offsetX = 0f  // snap back
                                    }
                                }

                                // Reset to center after action (so shake returns)
                                if (swipedRight || swipedLeft) {
                                    kotlinx.coroutines.MainScope().launch {
                                        delay(600)
                                        offsetX = 0f
                                    }
                                }
                            }
                        )
                )
            }
        }Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_PULLING_CALL -> {
            // Outgoing call in progress: only Cancel
            Button(
                onClick = { call.disconnect() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.size(width = 200.dp, height = 80.dp)
            ) {
                Text("Cancel", fontSize = 32.sp, color = MaterialTheme.colorScheme.onError)
            }
        }

        Call.STATE_ACTIVE -> {
            // Active call: big red Hang Up
            Button(
                onClick = { call.disconnect() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.size(width = 200.dp, height = 80.dp)
            ) {
                Text("Hang Up", fontSize = 32.sp, color = MaterialTheme.colorScheme.onError)
            }

            // Optional: Add more in-call controls here later (speaker, mute, hold, etc.)
        }

        Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
            Text("Call ended", fontSize = 32.sp, color = Color.White)
        }

        else -> {
            // Fallback
            Button(
                onClick = { call.disconnect() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.size(width = 200.dp, height = 80.dp)
            ) {
                Text("End", fontSize = 32.sp, color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
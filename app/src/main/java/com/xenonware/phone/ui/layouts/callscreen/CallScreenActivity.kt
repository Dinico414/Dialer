package com.xenonware.phone.ui.layouts.callscreen

import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.VideoProfile
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.LargePadding
import com.xenon.mylibrary.values.LargestPadding
import com.xenonware.phone.data.SharedPreferenceManager
import com.xenonware.phone.ui.layouts.main.contacts.Contact
import com.xenonware.phone.ui.layouts.main.contacts.RingingContactAvatar
import com.xenonware.phone.ui.theme.ScreenEnvironment
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
            ) { _, _ ->

                Surface(
                    modifier = Modifier.fillMaxSize(), color = colorScheme.surfaceContainer
                ) {
                    CallScreen(call = currentCall)
                }
            }
        }

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

    val context = LocalContext.current
    val rawNumber = call.details.handle?.schemeSpecificPart ?: "Private"

    // Look up contact name efficiently
    val displayName = remember(rawNumber) {
        if (rawNumber == "Private") "Private" else lookupContactName(context, rawNumber) ?: rawNumber
    }

    val duration by produceState(0L) {
        while (state == Call.STATE_ACTIVE) {
            value = System.currentTimeMillis() - (call.details.connectTimeMillis
                ?: System.currentTimeMillis())
            kotlinx.coroutines.delay(1000)
        }
    }

    val safeTopPadding =
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()
    val safeBottomPadding =
        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues()
            .calculateBottomPadding()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(
            Modifier
                .padding(top = safeTopPadding)
                .weight(0.25f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val statusText = when (state) {
                Call.STATE_RINGING -> "Incoming call..."
                Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_PULLING_CALL -> "Calling..."
                Call.STATE_HOLDING -> "Call is on hold"
                Call.STATE_ACTIVE -> formatDuration(duration)
                Call.STATE_DISCONNECTED -> if (duration > 0) "Call ended" else "Call failed"
                else -> "Unknown state"
            }

            Text(
                text = statusText,
                fontSize = 24.sp,
                color = colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Light
            )

            Spacer(Modifier.height(LargestPadding))

            // Now shows real contact name or number
            Text(
                text = displayName,
                fontSize = 48.sp,
                fontFamily = QuicksandTitleVariable,
                color = colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // Pass correct name to avatar (so letter/initial is correct)
            RingingContactAvatar(
                contact = Contact(name = displayName),
                state = state,
                size = 180.dp
            )
        }

        CallControls(state = state, call = call)

        Spacer(Modifier.height(LargePadding))

        if (state == Call.STATE_RINGING) {
            Box(
                Modifier
                    .padding(bottom = safeBottomPadding)
                    .weight(0.25f),
            ) {
                TextButton(onClick = {}) {
                    Text(
                        text = "SMS",
                        color = colorScheme.onSurface,
                        fontFamily = QuicksandTitleVariable,
                        fontWeight = FontWeight.Light,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        } else {
            Spacer(
                Modifier
                    .padding(bottom = safeBottomPadding)
                    .weight(0.25f)
            )
        }
    }
}

private fun lookupContactName(context: android.content.Context, phoneNumber: String): String? {
    return try {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        }
        null
    } catch (e: Exception) {
        null // In case of any permission or security issue
    }
}

@Composable
private fun CallControls(state: Int, call: Call) {
    when (state) {
        Call.STATE_RINGING -> {
            // ... (unchanged ringing swipe-to-answer UI)
            val iconSizeDp = 52.dp
            val draggableSizeDp = 96.dp
            val maxTrackWidthDp = 480.dp
            val horizontalPadding = 16.dp

            val density = LocalDensity.current
            val scope = rememberCoroutineScope()

            val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
            val availableWidthDp = screenWidthDp - (horizontalPadding * 2)
            val trackWidthDp = minOf(maxTrackWidthDp, availableWidthDp)

            val trackWidthPx = with(density) { trackWidthDp.toPx() }

            val maxOffsetPx =
                (trackWidthPx / 2) - with(density) { 16.dp.toPx() } - with(density) { draggableSizeDp.toPx() / 2 }

            val offsetX = remember { Animatable(0f) }

            val upDownAmplitudePx = with(density) { 4.dp.toPx() }

            val draggableState = rememberDraggableState { delta ->
                scope.launch {
                    val newValue = (offsetX.value + delta).coerceIn(-maxOffsetPx, maxOffsetPx)
                    offsetX.snapTo(newValue)
                }
            }

            val infiniteTransition = rememberInfiniteTransition(label = "shake transition")

            val shakeRotation by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 0f, animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1000 + 500

                        -20f at 100 with FastOutSlowInEasing
                        35f at 200 with FastOutSlowInEasing
                        -35f at 300 with FastOutSlowInEasing
                        35f at 400 with FastOutSlowInEasing
                        -35f at 500 with FastOutSlowInEasing
                        35f at 600 with FastOutSlowInEasing
                        -35f at 700 with FastOutSlowInEasing
                        35f at 800 with FastOutSlowInEasing
                        -15f at 900 with FastOutSlowInEasing
                        0f at 1000 with FastOutSlowInEasing
                        0f at durationMillis
                    }, repeatMode = RepeatMode.Restart
                ), label = "shake rotation"
            )

            val shakeVerticalOffset by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 0f, animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1000 + 500

                        -upDownAmplitudePx at 1000 with FastOutSlowInEasing
                        upDownAmplitudePx at durationMillis with FastOutSlowInEasing
                    }, repeatMode = RepeatMode.Restart
                ), label = "shake vertical offset"
            )

            val targetRotation = when {
                offsetX.value > 100f -> -135f
                offsetX.value < -100f -> 0f
                else -> 0f
            }

            val rotation by animateFloatAsState(
                targetValue = if (abs(offsetX.value) < 120f) targetRotation + shakeRotation else targetRotation,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow
                ),
                label = "final rotation"
            )

            val currentVerticalOffset by animateFloatAsState(
                targetValue = if (abs(offsetX.value) < 120f) shakeVerticalOffset else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow
                ),
                label = "final vertical offset"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = horizontalPadding)
                    .fillMaxWidth()
                    .height(136.dp)
                    .background(colorScheme.surfaceDim, CircleShape)
                    .padding(horizontal = 40.dp), contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CallEnd,
                    contentDescription = null,
                    tint = Color(0xFFFB4F43),
                    modifier = Modifier
                        .size(iconSizeDp)
                        .align(Alignment.CenterStart)
                )

                Icon(
                    imageVector = Icons.Rounded.Phone,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier
                        .size(iconSizeDp)
                        .align(Alignment.CenterEnd)
                )

                Box(
                    modifier = Modifier
                        .size(draggableSizeDp)
                        .offset {
                            IntOffset(
                                offsetX.value.roundToInt(), currentVerticalOffset.roundToInt()
                            )
                        }
                        .background(colorScheme.onSurface, CircleShape)
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Horizontal,
                            onDragStopped = { velocity ->
                                val positionThreshold = maxOffsetPx * 0.6f
                                val velocityThreshold = 1000f

                                val swipedRight =
                                    offsetX.value > positionThreshold || velocity > velocityThreshold
                                val swipedLeft =
                                    offsetX.value < -positionThreshold || velocity < -velocityThreshold

                                scope.launch {
                                    when {
                                        swipedRight -> {
                                            call.answer(VideoProfile.STATE_AUDIO_ONLY)
                                            offsetX.animateTo(
                                                maxOffsetPx, animationSpec = tween(
                                                    200, easing = FastOutSlowInEasing
                                                )
                                            )
                                        }

                                        swipedLeft -> {
                                            call.reject(false, null)
                                            offsetX.animateTo(
                                                -maxOffsetPx, animationSpec = tween(
                                                    200, easing = FastOutSlowInEasing
                                                )
                                            )
                                        }

                                        else -> {
                                            offsetX.animateTo(
                                                0f,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                            )
                                        }
                                    }
                                }
                            })
                ) {
                    val progress = (offsetX.value / maxOffsetPx).coerceIn(-0.85f, 0.85f)
                    val blendAmount = abs(progress)
                    val targetColor = if (progress > 0) Color(0xFF4CAF50) else Color(0xFFFB4F43)

                    val blendedColor = lerp(
                        colorScheme.surfaceBright, targetColor, blendAmount.coerceIn(0f, 1f)
                    )

                    val animatedColor by animateColorAsState(
                        targetValue = blendedColor, animationSpec = tween(100)
                    )

                    Icon(
                        imageVector = Icons.Rounded.CallEnd,
                        contentDescription = "Swipe right to accept, left to decline",
                        tint = animatedColor,
                        modifier = Modifier
                            .size(iconSizeDp)
                            .align(Alignment.Center)
                            .rotate(rotation)
                    )
                }
            }
        }

        Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
            Box(
                modifier = Modifier
                    .height(136.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { call.disconnect() },
                    enabled = false,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFFB4F43),
                        disabledContainerColor = Color(0xFFFB4F43).copy(alpha = 0.5f),
                        contentColor = colorScheme.onSurface,
                        disabledContentColor = colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(width = 200.dp, height = 96.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CallEnd,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        else -> {
            Box(
                modifier = Modifier
                    .height(136.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { call.disconnect() },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFFB4F43)),
                    modifier = Modifier.size(width = 200.dp, height = 96.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CallEnd,
                        tint = colorScheme.onSurface,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
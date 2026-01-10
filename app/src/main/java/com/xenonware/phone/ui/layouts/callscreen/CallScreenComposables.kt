@file:Suppress("UnusedUnaryOperator")

package com.xenonware.phone.ui.layouts.callscreen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.LargePadding
import com.xenon.mylibrary.values.LargerCornerRadius
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.NoCornerRadius
import com.xenon.mylibrary.values.SmallElevation
import com.xenonware.phone.service.MyInCallService
import com.xenonware.phone.ui.layouts.main.contacts.Contact
import com.xenonware.phone.ui.layouts.main.contacts.RingingContactAvatar
import com.xenonware.phone.viewmodel.CallScreenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CallScreenUi(
    call: Call?, isLandscape: Boolean, forceCompactMode: Boolean = false,
) {
    val context = LocalContext.current
    val viewModel: CallScreenViewModel = viewModel()
    val activity = context as? Activity

    DisposableEffect(isLandscape) {
        activity?.requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }

        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(call) {
        call?.let { viewModel.initialize(it, context) }
    }

    val stateNullable by viewModel.callState.collectAsStateWithLifecycle()
    val state = stateNullable ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active call", fontSize = 32.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    if (call == null) return

    val displayName by viewModel.displayName.collectAsStateWithLifecycle()
    val previousActiveState by viewModel.previousActiveState.collectAsStateWithLifecycle()
    val callWasRejectedByUser by viewModel.callWasRejectedByUser.collectAsStateWithLifecycle()
    val cameFromRinging = previousActiveState == Call.STATE_RINGING

    val duration by produceState(0L, state, call.details.connectTimeMillis) {
        if (state == Call.STATE_ACTIVE && call.details.connectTimeMillis > 0) {
            while (true) {
                value = System.currentTimeMillis() - call.details.connectTimeMillis
                delay(1000)
            }
        } else {
            value = 0L
        }
    }

    val statusText = when (state) {
        Call.STATE_RINGING -> "Incoming call..."
        Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_PULLING_CALL -> "Calling..."
        Call.STATE_HOLDING -> "Call is on hold"
        Call.STATE_ACTIVE -> viewModel.formatDuration(duration)
        Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> when {
            callWasRejectedByUser -> "Call rejected"
            cameFromRinging && !callWasRejectedByUser -> "Call missed"
            else -> "Call ended"
        }

        else -> "Unknown state"
    }

    val isCompact = forceCompactMode || isLandscape

    val avatarSize = if (isCompact) 140.dp else 180.dp
    val nameFontSize = if (isCompact) 36.sp else 48.sp
    val statusFontSize = if (isCompact) 20.sp else 24.sp
    val controlButtonSize = if (isCompact) 64.dp else 72.dp
    val endCallButtonWidth = if (isCompact) 160.dp else 200.dp
    val verticalSpacerWeight = if (isCompact) 0.15f else 0.25f

    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val primaryBreath by infiniteTransition.animateFloat(
        initialValue = 1.3f, targetValue = 1.7f, animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse
        ), label = "primaryBreath"
    )
    val secondaryBreath by infiniteTransition.animateFloat(
        initialValue = 1.7f, targetValue = 1.3f, animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse
        ), label = "secondaryBreath"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(color = surfaceContainer)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(secondaryContainer, Color.Transparent),
                        center = Offset(size.width, size.height),
                        radius = size.width * secondaryBreath
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryContainer, Color.Transparent),
                        center = Offset(0f, size.height),
                        radius = size.width * primaryBreath
                    )
                )
            }) {
        Column(
            modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val safeTopPadding =
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues()
                    .calculateTopPadding()
            val safeBottomPadding =
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues()
                    .calculateBottomPadding()

            Spacer(
                Modifier
                    .padding(top = safeTopPadding)
                    .weight(verticalSpacerWeight)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = statusText,
                    fontSize = statusFontSize,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Light
                )
                Spacer(Modifier.height(if (isCompact) LargePadding else LargestPadding))
                Text(
                    text = displayName,
                    fontSize = nameFontSize,
                    fontFamily = QuicksandTitleVariable,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), contentAlignment = Alignment.Center
            ) {
                RingingContactAvatar(
                    contact = Contact(name = displayName), state = state, size = avatarSize
                )
            }

            CallControls(
                state = state,
                call = call,
                viewModel = viewModel,
                isCompact = isCompact,
                controlButtonSize = controlButtonSize,
                endCallButtonWidth = endCallButtonWidth
            )

            Spacer(Modifier.height(LargePadding))

            if (state == Call.STATE_RINGING) {
                Box(
                    Modifier
                        .padding(bottom = safeBottomPadding)
                        .weight(verticalSpacerWeight)
                ) {
                    CompositionLocalProvider(
                        LocalRippleConfiguration provides RippleConfiguration(
                            color = Color(
                                0xFFFFB300
                            )
                        )
                    ) {
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        val targetTextColor = if (isPressed) lerp(
                            start = MaterialTheme.colorScheme.onSurface,
                            stop = Color(0xFFFFB300),
                            fraction = 0.25f
                        ) else MaterialTheme.colorScheme.onSurface
                        val animatedTextColor by animateColorAsState(
                            targetValue = targetTextColor, animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessVeryLow
                            ), label = "TextColorAnimation"
                        )

                        TextButton(
                            onClick = { call.reject(true, null) },
                            interactionSource = interactionSource
                        ) {
                            Text(
                                text = "SMS",
                                color = animatedTextColor,
                                fontFamily = QuicksandTitleVariable,
                                fontWeight = FontWeight.Light,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(
                    Modifier
                        .padding(bottom = safeBottomPadding)
                        .weight(verticalSpacerWeight)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallControls(
    state: Int,
    call: Call,
    viewModel: CallScreenViewModel,
    isCompact: Boolean,
    controlButtonSize: androidx.compose.ui.unit.Dp,
    endCallButtonWidth: androidx.compose.ui.unit.Dp,
) {
    val shadowTint = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)

    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isOnHold by viewModel.isOnHold.collectAsStateWithLifecycle()
    val showKeypad by viewModel.showKeypad.collectAsStateWithLifecycle()
    val previousActiveState by viewModel.previousActiveState.collectAsStateWithLifecycle()

    val audioState by MyInCallService.audioStateFlow.collectAsStateWithLifecycle()

    when (state) {
        Call.STATE_RINGING -> {
            RingingSwipeControl(
                call = call, onUserReject = { viewModel.setUserRejectedCall() })
        }

        Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> {
            val cameFromRinging = previousActiveState == Call.STATE_RINGING
            Box(
                modifier = Modifier
                    .height(if (isCompact) 112.dp else 136.dp)
                    .fillMaxWidth()
                    .alpha(0.6f), contentAlignment = Alignment.Center
            ) {
                if (cameFromRinging) {
                    val iconSizeDp = 52.dp
                    val draggableSizeDp = 96.dp
                    val maxTrackWidthDp = 480.dp
                    val horizontalPadding = 16.dp

                    Box(
                        modifier = Modifier
                            .padding(horizontal = horizontalPadding)
                            .widthIn(max = maxTrackWidthDp)
                            .fillMaxWidth()
                            .height(if (isCompact) 112.dp else 136.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape)
                            .padding(horizontal = 16.dp), contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Phone,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier
                                .padding(24.dp)
                                .size(iconSizeDp)
                                .align(Alignment.CenterEnd)
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(draggableSizeDp)
                                .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CallEnd,
                                contentDescription = "Decline",
                                tint = Color(0xFFFB4F43),
                                modifier = Modifier
                                    .size(iconSizeDp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = { call.disconnect() },
                        enabled = state == Call.STATE_DISCONNECTING,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFFFB4F43),
                            disabledContainerColor = Color(0xFFFB4F43),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .size(width = endCallButtonWidth, height = 96.dp)
                            .shadow(
                                10.dp,
                                CircleShape,
                                ambientColor = shadowTint,
                                spotColor = shadowTint
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = "Ending call...",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        else -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(LargePadding)) {
                    // Mute
                    IconButton(
                        onClick = { viewModel.toggleMute() },
                        modifier = Modifier
                            .size(controlButtonSize)
                            .shadow(
                                8.dp, CircleShape, ambientColor = shadowTint, spotColor = shadowTint
                            )
                            .background(
                                if (isMuted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerLowest,
                                CircleShape
                            )
                    ) {
                        Crossfade(targetState = isMuted) { muted ->
                            Icon(
                                imageVector = if (muted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                contentDescription = if (muted) "Unmute" else "Mute",
                                tint = if (muted) Color(0xFFFB4F43) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Audio Route
                    audioState?.let { audio ->
                        if (Integer.bitCount(audio.supportedRouteMask) > 1) {
                            IconButton(
                                onClick = { viewModel.cycleAudioRoute(audio.supportedRouteMask) },
                                modifier = Modifier
                                    .size(controlButtonSize)
                                    .shadow(
                                        8.dp,
                                        CircleShape,
                                        ambientColor = shadowTint,
                                        spotColor = shadowTint
                                    )
                                    .background(
                                        if (audio.route == CallAudioState.ROUTE_EARPIECE) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.onSurface,
                                        CircleShape
                                    )
                            ) {
                                AnimatedContent(
                                    targetState = audio.route, transitionSpec = {
                                        fadeIn(
                                            animationSpec = tween(
                                                220, delayMillis = 90
                                            )
                                        ) togetherWith fadeOut(animationSpec = tween(90))
                                    }, label = "AudioRouteTransition"
                                ) { targetRoute ->
                                    val icon = when (targetRoute) {
                                        CallAudioState.ROUTE_SPEAKER -> Icons.AutoMirrored.Rounded.VolumeUp
                                        CallAudioState.ROUTE_WIRED_HEADSET -> Icons.Rounded.Headset
                                        CallAudioState.ROUTE_BLUETOOTH -> Icons.Rounded.Bluetooth
                                        else -> Icons.Rounded.Phone
                                    }

                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "Audio output",
                                        tint = if (targetRoute == CallAudioState.ROUTE_EARPIECE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inversePrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                    // Hold
                    IconButton(
                        onClick = { viewModel.toggleHold() },
                        modifier = Modifier
                            .size(controlButtonSize)
                            .shadow(
                                8.dp, CircleShape, ambientColor = shadowTint, spotColor = shadowTint
                            )
                            .background(
                                if (isOnHold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerLowest,
                                CircleShape
                            )
                    ) {
                        Crossfade(targetState = isOnHold) { held ->
                            Icon(
                                imageVector = if (held) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = if (held) "Resume" else "Hold",
                                tint = if (held) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                    }

                    // Keypad
                    IconButton(
                        onClick = { viewModel.toggleKeypad() },
                        modifier = Modifier
                            .size(controlButtonSize)
                            .shadow(
                                8.dp, CircleShape, ambientColor = shadowTint, spotColor = shadowTint
                            )
                            .background(
                                if (showKeypad) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerLowest,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Dialpad,
                            contentDescription = "Show keypad",
                            tint = if (showKeypad) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                val sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true,
                )

                if (showKeypad) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.toggleKeypad() },
                        sheetState = sheetState,
                        dragHandle = { BottomSheetDefaults.DragHandle() },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        tonalElevation = SmallElevation,
                        shape = RoundedCornerShape(
                            LargerCornerRadius, LargerCornerRadius, NoCornerRadius, NoCornerRadius
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val keys =
                                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
                            val letters = listOf(
                                "", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "+", ""
                            )

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 32.dp)
                            ) {
                                items(12) { index ->
                                    val key = keys[index]
                                    val letter = letters[index]

                                    KeyButton(
                                        key = key,
                                        subtitle = letter,
                                        isCompact = isCompact,
                                        onClick = {
                                            call.playDtmfTone(key[0])
                                            android.os.Handler(android.os.Looper.getMainLooper())
                                                .postDelayed({ call.stopDtmfTone() }, 140)
                                        })
                                }
                            }
                        }
                    }
                }

                val canMerge =
                    call.details.callCapabilities and Call.Details.CAPABILITY_MERGE_CONFERENCE != 0
                val hasConference =
                    call.children.isNotEmpty() || call.conferenceableCalls.isNotEmpty()

                if (canMerge && hasConference) {
                    IconButton(
                        onClick = { call.conference(call.conferenceableCalls.first()) },
                        modifier = Modifier
                            .padding(bottom = LargePadding)
                            .size(controlButtonSize)
                            .shadow(
                                8.dp, CircleShape, ambientColor = shadowTint, spotColor = shadowTint
                            )
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Merge,
                            contentDescription = "Merge calls",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.height(LargestPadding))

                IconButton(
                    onClick = { call.disconnect() },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFFB4F43)),
                    modifier = Modifier
                        .size(width = endCallButtonWidth, height = 96.dp)
                        .shadow(
                            10.dp, CircleShape, ambientColor = shadowTint, spotColor = shadowTint
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CallEnd,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = "End call",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun KeyButton(
    key: String,
    subtitle: String,
    isCompact: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(if (isCompact) 76.dp else 88.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Text(
            text = key,
            fontSize = if (isCompact) 30.sp else 36.sp,
            fontFamily = QuicksandTitleVariable,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            style = LocalTextStyle.current.copy(lineHeight = 12.sp),
            modifier = Modifier.offset(y = (-2).dp),
            fontFamily = QuicksandTitleVariable,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun RingingSwipeControl(call: Call, onUserReject: () -> Unit) {
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

    @Suppress("UnusedUnaryOperator") val shakeRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 0f, animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1500
                -20f at 100 using FastOutSlowInEasing
                35f at 200 using FastOutSlowInEasing
                -35f at 300 using FastOutSlowInEasing
                35f at 400 using FastOutSlowInEasing
                -35f at 500 using FastOutSlowInEasing
                35f at 600 using FastOutSlowInEasing
                -35f at 700 using FastOutSlowInEasing
                35f at 800 using FastOutSlowInEasing
                -15f at 900 using FastOutSlowInEasing
                0f at 1000 using FastOutSlowInEasing
                0f at durationMillis
            }, repeatMode = RepeatMode.Restart
        ), label = "shake rotation"
    )

    val shakeVerticalOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 0f, animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1500
                -upDownAmplitudePx at 1000 using FastOutSlowInEasing
                upDownAmplitudePx at durationMillis using FastOutSlowInEasing
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
            .widthIn(max = maxTrackWidthDp)
            .fillMaxWidth()
            .height(136.dp)
            .shadow(10.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.scrim.copy(0.6f))
            .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape)
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
                .background(MaterialTheme.colorScheme.onSurface, CircleShape)
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
                                        maxOffsetPx, tween(200, easing = FastOutSlowInEasing)
                                    )
                                }

                                swipedLeft -> {
                                    onUserReject()
                                    call.reject(false, null)
                                    offsetX.animateTo(
                                        -maxOffsetPx, tween(200, easing = FastOutSlowInEasing)
                                    )
                                }

                                else -> {
                                    offsetX.animateTo(
                                        0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)
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
                MaterialTheme.colorScheme.surfaceBright, targetColor, blendAmount.coerceIn(0f, 1f)
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
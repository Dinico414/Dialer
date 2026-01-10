package com.xenonware.phone.ui.layouts.main.dialer_screen

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.LargePadding
import com.xenon.mylibrary.values.LargestPadding

@Composable
fun DialerScreen(
    modifier: Modifier = Modifier,
    onShowCallLog: () -> Unit,
) {
    var phoneNumber by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = phoneNumber.ifEmpty { "Enter number" },
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = if (phoneNumber.isEmpty()) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onBackground,
            fontFamily = QuicksandTitleVariable
        )
        Dialpad(
            onNumberClick = { digit -> phoneNumber += digit }, onDeleteClick = {
            if (phoneNumber.isNotEmpty()) {
                phoneNumber = phoneNumber.dropLast(1)
            }
        }, onClearAll = { phoneNumber = "" }, onCallClick = {
            if (phoneNumber.isNotEmpty()) {
                safePlaceCall(context, phoneNumber)
            }
        }, onShowCallLog = onShowCallLog
        )
    }
}

@Composable
fun Dialpad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearAll: () -> Unit,
    onCallClick: () -> Unit,
    onShowCallLog: () -> Unit,
) {
    val extraBottomPadding = 64.dp + LargePadding * 2

    val safeBottomPadding =
        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues()
            .calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = safeBottomPadding + extraBottomPadding)
            .padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
        val letters = listOf(
            "", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "+", ""
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(12) { index ->
                val key = keys[index]
                val letter = letters[index]

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .height(70.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .combinedClickable(onClick = { onNumberClick(key) }, onLongClick = {
                            if (key == "0") {
                                onNumberClick("+")
                            } else {
                                onNumberClick(key)
                            }
                        })) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = QuicksandTitleVariable,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = letter,
                        fontSize = 12.sp,
                        style = LocalTextStyle.current.copy(lineHeight = 12.sp),
                        modifier = Modifier.offset(y = (-2).dp),
                        fontFamily = QuicksandTitleVariable,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(LargestPadding))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledTonalIconButton(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape), onClick = onShowCallLog
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = "Call log",
                    modifier = Modifier.size(28.sp.value.dp)
                )
            }

            FilledTonalIconButton(
                onClick = onCallClick, colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFF4CAF50), contentColor = Color.White
                ), modifier = Modifier
                    .weight(1f)
                    .height(82.dp)
                    .clip(RoundedCornerShape(50.dp))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = "Call",
                    modifier = Modifier.size(36.sp.value.dp)
                )
            }

            val interactionSource = remember { MutableInteractionSource() }

            FilledTonalIconButton(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                onClick = onDeleteClick,
                interactionSource = interactionSource
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "Delete",
                    modifier = Modifier.size(28.sp.value.dp)
                )
            }

            LaunchedEffect(interactionSource) {
                var pressStartTime = 0L

                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> {
                            pressStartTime = System.currentTimeMillis()
                        }
                        is PressInteraction.Release,
                        is PressInteraction.Cancel -> {
                            val pressDuration = System.currentTimeMillis() - pressStartTime
                            if (pressDuration >= 420L) {
                                onClearAll()
                            }
                            pressStartTime = 0L
                        }
                    }
                }
            }
        }
    }
}

private fun safePlaceCall(context: Context, phoneNumber: String) {
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    val uri = Uri.parse("tel:$phoneNumber")

    val isDefaultDialer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    } else {
        telecomManager.defaultDialerPackage == context.packageName
    }

    if (isDefaultDialer) {
        val extras = Bundle().apply {
            putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
        }
        try {
            telecomManager.placeCall(uri, extras)
        } catch (_: SecurityException) {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            fallbackCallIntent(context, uri)
        }
    } else {
        fallbackCallIntent(context, uri)
    }
}

private fun fallbackCallIntent(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_CALL, uri)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to place call", Toast.LENGTH_SHORT).show()
    }
}
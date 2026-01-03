package com.xenonware.phone.ui.layouts.main.dialer_screen

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DialerScreen(
    modifier: Modifier = Modifier,
    onShowCallLog: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    val context = LocalContext.current

    val roleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Dialer app set as default", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to set as default phone", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (phoneNumber.isEmpty()) "Enter number" else phoneNumber,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Dialpad(
            onNumberClick = { digit -> phoneNumber += digit },
            onDeleteClick = {
                if (phoneNumber.isNotEmpty()) phoneNumber = phoneNumber.dropLast(1)
            },
            onCallClick = {
                if (phoneNumber.isNotEmpty()) {
                    safePlaceCall(context, phoneNumber)
                }
            },
            onShowCallLog = onShowCallLog,
            onSetDefault = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                    if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                        roleRequestLauncher.launch(intent)
                    } else {
                        Toast.makeText(context, "Already default or role not available", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun Dialpad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onCallClick: () -> Unit,
    onShowCallLog: () -> Unit,
    onSetDefault: () -> Unit
) {
    val buttons = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(buttons) { button ->
                Button(
                    onClick = { onNumberClick(button) },
                    modifier = Modifier.size(80.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = button,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledTonalIconButton(
                modifier = Modifier.size(64.dp).clip(CircleShape),
                onClick = onShowCallLog
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = "Call log",
                    modifier = Modifier.size(28.sp.value.dp)
                )
            }

            FilledTonalIconButton(
                onClick = onCallClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ),
                modifier = Modifier
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

            FilledTonalIconButton(
                modifier = Modifier.size(64.dp).clip(CircleShape),
                onClick = onDeleteClick
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "Delete",
                    modifier = Modifier.size(28.sp.value.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(onClick = onSetDefault) {
            Text("Set as Default Dialer")
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
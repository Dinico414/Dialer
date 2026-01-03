package com.xenonware.phone.ui.layouts.main.call_history

import android.Manifest
import android.content.Context
import android.provider.CallLog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.values.LargestPadding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallHistoryScreen(
    modifier: Modifier = Modifier, onBack: (() -> Unit)? = null
) {
    var callLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            callLogs = loadCallLogEntries(context)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header with circular back button (only if onBack is provided)
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular back button
                IconButton(
                    onClick = onBack, modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Call History", style = MaterialTheme.typography.headlineSmall
                )
            }
        } else {
            // No back button needed (e.g., if this is the root screen)
            Text(
                text = "Call History",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Content
        if (callLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No call history or permission denied",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                bottom = with(LocalDensity.current) {
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues()
                        .calculateBottomPadding() + 64.dp + LargestPadding * 2
                })) {
                items(callLogs) { log ->
                    Text(
                        text = log,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Divider()
                }
            }
        }
    }
}

private fun loadCallLogEntries(context: Context): List<String> {
    val logs = mutableListOf<String>()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy – HH:mm", Locale.getDefault())

    val cursor = context.contentResolver.query(
        CallLog.Calls.CONTENT_URI, null, null, null, "${CallLog.Calls.DATE} DESC"
    )

    cursor?.use {
        val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
        val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
        val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
        val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)

        while (it.moveToNext()) {
            val number = it.getString(numberIndex) ?: "Unknown"
            val name = it.getString(nameIndex)
            val type = it.getInt(typeIndex)
            val date = it.getLong(dateIndex)

            val displayName = name ?: number
            val typeString = when (type) {
                CallLog.Calls.INCOMING_TYPE -> "Incoming"
                CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                CallLog.Calls.MISSED_TYPE -> "Missed"
                else -> "Unknown"
            }

            logs.add("$displayName\n$typeString • ${dateFormat.format(Date(date))}")
        }
    }
    return logs
}
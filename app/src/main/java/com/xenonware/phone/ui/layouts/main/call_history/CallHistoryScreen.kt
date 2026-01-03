package com.xenonware.phone.ui.layouts.main.call_history

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.LargePadding
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenon.mylibrary.values.SmallSpacing
import com.xenon.mylibrary.values.SmallestCornerRadius
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CallLogEntry(
    val nameOrNumber: String,
    val phoneNumber: String,
    val type: Int,
    val date: Long
)

data class CallGroup(
    val title: String,
    val entries: List<CallLogEntry>
)

@Composable
fun CallHistoryScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    var callLogs by remember { mutableStateOf<List<CallLogEntry>>(emptyList()) }
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
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back to Dialer",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Call History",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = QuicksandTitleVariable
            )
        }

        // Scrollable content starts here
        if (callLogs.isEmpty()) {
            // Show empty state (no divider needed)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No call history\nor permission denied",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            val groupedCalls = remember(callLogs) { groupCallLogsByDate(callLogs) }

            // Create LazyListState to track scroll
            val listState = rememberLazyListState()

            // Determine if we can scroll up (i.e., not at the top)
            val showDivider = remember { derivedStateOf {
                listState.firstVisibleItemIndex > 0 ||
                        (listState.firstVisibleItemScrollOffset > 0)
            } }.value

            // Conditional divider
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            LazyColumn(
                state = listState, // <-- Important: pass the state
                verticalArrangement = Arrangement.spacedBy(SmallSpacing),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = with(LocalDensity.current) {
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                64.dp + LargePadding * 2
                    }
                )
            ) {
                groupedCalls.forEach { group ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = group.title,
                                fontSize = 20.sp,
                                fontFamily = QuicksandTitleVariable,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(group.entries.indices.toList()) { index ->
                        val entry = group.entries[index]
                        val isFirst = index == 0
                        val isLast = index == group.entries.lastIndex
                        val isSingle = group.entries.size == 1

                        CallHistoryItemCard(
                            entry = entry,
                            isFirstInGroup = isFirst,
                            isLastInGroup = isLast,
                            isSingle = isSingle
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallHistoryItemCard(
    entry: CallLogEntry,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    isSingle: Boolean
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val (icon, backgroundColor) = when (entry.type) {
        CallLog.Calls.INCOMING_TYPE -> Icons.Rounded.KeyboardArrowDown to Color(0xFF2196F3)
        CallLog.Calls.OUTGOING_TYPE -> Icons.Rounded.KeyboardArrowUp to Color(0xFF4CAF50)
        CallLog.Calls.MISSED_TYPE -> Icons.Rounded.Close to Color(0xFFF44336)
        else -> Icons.Rounded.Remove to Color(0xFF9E9E9E)
    }

    val shape = when {
        isSingle -> RoundedCornerShape(MediumCornerRadius)
        isFirstInGroup -> RoundedCornerShape(
            topStart = MediumCornerRadius,
            topEnd = MediumCornerRadius,
            bottomStart = SmallestCornerRadius,
            bottomEnd = SmallestCornerRadius
        )
        isLastInGroup -> RoundedCornerShape(
            topStart = SmallestCornerRadius,
            topEnd = SmallestCornerRadius,
            bottomStart = MediumCornerRadius,
            bottomEnd = MediumCornerRadius
        )
        else -> RoundedCornerShape(SmallestCornerRadius)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surfaceBright,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.nameOrNumber,
                    fontFamily = QuicksandTitleVariable,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (entry.type) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        else -> "Unknown"
                    } + " • ${dateFormat.format(Date(entry.date))}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${entry.phoneNumber}"))
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${entry.phoneNumber}")))
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF4CAF50))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = "Call",
                    tint = MaterialTheme.colorScheme.surfaceBright,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun groupCallLogsByDate(entries: List<CallLogEntry>): List<CallGroup> {
    if (entries.isEmpty()) return emptyList()

    val now = Calendar.getInstance()
    val today = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    val yesterday = today.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_MONTH, -1)

    val groups = mutableListOf<CallGroup>()
    val currentGroup = mutableListOf<CallLogEntry>()
    var currentTitle: String? = null

    for (entry in entries) {
        val cal = Calendar.getInstance().apply { timeInMillis = entry.date }

        val title = when {
            cal.after(today) || cal == today -> "Today"
            cal.after(yesterday) || cal == yesterday -> "Yesterday"
            cal.after(Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -7) }) -> "Last 7 Days"
            cal.after(Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -30) }) -> "Last 30 Days"
            cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) -> "This Month"
            else -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        }

        if (title != currentTitle && currentGroup.isNotEmpty()) {
            groups.add(CallGroup(currentTitle!!, currentGroup.toList()))
            currentGroup.clear()
        }

        currentTitle = title
        currentGroup.add(entry)
    }

    if (currentGroup.isNotEmpty()) {
        groups.add(CallGroup(currentTitle!!, currentGroup.toList()))
    }

    return groups
}

private fun loadCallLogEntries(context: Context): List<CallLogEntry> {
    val logs = mutableListOf<CallLogEntry>()

    val cursor = context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        null,
        null,
        null,
        "${CallLog.Calls.DATE} DESC"
    )

    cursor?.use {
        val numberIndex = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
        val typeIndex = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
        val dateIndex = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
        val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)

        while (it.moveToNext()) {
            val number = it.getString(numberIndex) ?: "Unknown"
            val name = if (nameIndex != -1) it.getString(nameIndex) else null
            val type = it.getInt(typeIndex)
            val date = it.getLong(dateIndex)

            val displayName = if (!name.isNullOrBlank()) name else number

            logs.add(
                CallLogEntry(
                    nameOrNumber = displayName,
                    phoneNumber = number,
                    type = type,
                    date = date
                )
            )
        }
    }
    return logs
}
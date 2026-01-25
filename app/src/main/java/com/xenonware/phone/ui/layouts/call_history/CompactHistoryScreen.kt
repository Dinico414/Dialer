package com.xenonware.phone.ui.layouts.call_history

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Voicemail
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.LargePadding
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.NoSpacing
import com.xenon.mylibrary.values.SmallSpacing
import com.xenon.mylibrary.values.SmallestCornerRadius
import com.xenonware.phone.R
import com.xenonware.phone.ui.layouts.main.dialer_screen.safePlaceCall
import com.xenonware.phone.util.PhoneNumberFormatter
import com.xenonware.phone.viewmodel.CallHistoryViewModel
import com.xenonware.phone.viewmodel.LayoutType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


data class CallLogEntry(
    val nameOrNumber: String, val phoneNumber: String, val type: Int, val date: Long,
)

data class CallGroup(
    val title: String, val entries: List<CallLogEntry>,
)


@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun CompactHistoryScreen(
    onNavigateBack: () -> Unit,
    layoutType: LayoutType,
    isLandscape: Boolean,
    viewModel: CallHistoryViewModel,
) {
    val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadCallLogs(context)
        }
    }

    LaunchedEffect(Unit) {
        if (hasPermission) {
            viewModel.loadCallLogs(context)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
        }
    }

    val configuration = LocalConfiguration.current
    val appHeight = configuration.screenHeightDp.dp

    val isAppBarExpandable = when (layoutType) {
        LayoutType.COVER -> false
        LayoutType.SMALL -> false
        LayoutType.COMPACT -> !isLandscape && appHeight >= 460.dp
        LayoutType.MEDIUM -> true
        LayoutType.EXPANDED -> true
    }

    ActivityScreen(
        titleText = stringResource(R.string.call_history),
        expandable = isAppBarExpandable,
        navigationIconStartPadding = MediumPadding,
        navigationIconPadding = MediumPadding,
        navigationIconSpacing = NoSpacing,
        navigationIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.navigate_back_description),
                modifier = Modifier.size(24.dp)
            )
        },
        onNavigationIconClick = onNavigateBack,
        hasNavigationIconExtraContent = false,
        actions = {},
        content = { _ ->
            Column(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    callLogs.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (hasPermission) stringResource(R.string.no_calls_yet) else stringResource(R.string.permission_required),
                                fontSize = 18.sp,
                                lineHeight = 28.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        val groupedCalls = groupCallLogsByDate(
                            entries = callLogs,
                            todayStr = stringResource(R.string.today),
                            yesterdayStr = stringResource(R.string.yesterday),
                        )
                        val listState = rememberLazyListState()

                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(SmallSpacing),
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxSize(),
                            contentPadding = PaddingValues(
                                bottom = with(LocalDensity.current) {
                                    WindowInsets.navigationBars.asPaddingValues()
                                        .calculateBottomPadding() + 64.dp + LargePadding * 2
                                })
                        ) {
                            groupedCalls.forEach { group ->
                                item(key = "header_${group.title}") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceContainer)
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
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

                                items(
                                    items = group.entries, key = { it.date }) { entry ->
                                    val index = group.entries.indexOf(entry)
                                    CallHistoryItemCard(
                                        entry = entry,
                                        isFirstInGroup = index == 0,
                                        isLastInGroup = index == group.entries.lastIndex,
                                        isSingle = group.entries.size == 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        })
}

@Composable
fun CallHistoryItemCard(
    entry: CallLogEntry, isFirstInGroup: Boolean, isLastInGroup: Boolean, isSingle: Boolean,
) {
    val context = LocalContext.current

    val callDate = Date(entry.date)

    val (icon, backgroundColor) = when (entry.type) {
        CallLog.Calls.INCOMING_TYPE -> Icons.Rounded.KeyboardArrowDown to Color(0xFF2196F3)
        CallLog.Calls.OUTGOING_TYPE -> Icons.Rounded.KeyboardArrowUp to Color(0xFF4CAF50)
        CallLog.Calls.MISSED_TYPE -> Icons.Rounded.Close to Color(0xFFF44336)
        CallLog.Calls.VOICEMAIL_TYPE -> Icons.Rounded.Voicemail to Color(0xFFFFC107)
        CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> Icons.Rounded.KeyboardArrowDown to Color(0xFF9E9E9E)
        CallLog.Calls.REJECTED_TYPE -> Icons.Rounded.Close to Color(0xFF9E9E9E)
        CallLog.Calls.BLOCKED_TYPE -> Icons.Rounded.Block to Color(0xFF9E9E9E)
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

                val displayText = if (entry.nameOrNumber == entry.phoneNumber || entry.nameOrNumber.isEmpty()) {
                    PhoneNumberFormatter.formatForDisplay(entry.nameOrNumber, context)
                } else {
                    entry.nameOrNumber
                }
                Text(
                    text = displayText,
                    fontFamily = QuicksandTitleVariable,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val hasContact = entry.nameOrNumber != entry.phoneNumber && entry.nameOrNumber.isNotBlank()

                val callTypeText = when (entry.type) {
                    CallLog.Calls.INCOMING_TYPE -> stringResource(R.string.incoming)
                    CallLog.Calls.OUTGOING_TYPE -> stringResource(R.string.outgoing)
                    CallLog.Calls.MISSED_TYPE -> stringResource(R.string.missed)
                    CallLog.Calls.VOICEMAIL_TYPE -> stringResource(R.string.voicemail)
                    CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> stringResource(R.string.answered_externally)
                    CallLog.Calls.REJECTED_TYPE -> stringResource(R.string.rejected)
                    CallLog.Calls.BLOCKED_TYPE -> stringResource(R.string.blocked)
                    else -> stringResource(R.string.unknown)
                }

                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(callDate)

                val extraLabel = getNumberTypeOrOrigin(
                    context = context,
                    phoneNumber = entry.phoneNumber,
                    hasContactName = hasContact
                )

                Text(
                    text = "$callTypeText • $time$extraLabel",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    if (entry.phoneNumber.isNotEmpty()) {
                        safePlaceCall(context, entry.phoneNumber)
                    }
                },
                enabled = entry.phoneNumber.isNotEmpty(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = "Call",
                    tint = if (entry.phoneNumber.isNotEmpty())
                        Color(0xFF4CAF50)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private val phoneUtil = PhoneNumberUtil.getInstance()
private val geocoder = PhoneNumberOfflineGeocoder.getInstance()

@Composable
private fun getNumberOriginLabel(
    context: Context,
    rawNumber: String
): String {
    if (rawNumber.isBlank()) return ""

    val userCountry = context.resources.configuration.locales.get(0).country ?: "DE"

    return try {
        val number = phoneUtil.parse(rawNumber, userCountry)
        if (!phoneUtil.isValidNumber(number)) return ""

        val regionCode = phoneUtil.getRegionCodeForNumber(number) ?: return ""

        val description = geocoder.getDescriptionForNumber(
            number,
            Locale.getDefault()  // uses device language → "Berlin", "Rom", "Paris"...
        )?.trim() ?: ""

        if (description.isBlank()) return ""

        if (regionCode == userCountry) {
            " • $description"  // e.g. " • Berlin", " • München", " • Rom"
        } else {
            val countryName = Locale("", regionCode).displayCountry
            " • $countryName"  // e.g. " • Italien", " • Frankreich"
        }
    } catch (_: Exception) {
        ""
    }
}

@Composable
private fun getNumberTypeOrOrigin(
    context: Context,
    phoneNumber: String,
    hasContactName: Boolean
): String {
    if (phoneNumber.isBlank()) return ""

    if (hasContactName) {
        val labelFromContact = getContactPhoneLabel(context, phoneNumber)
        if (labelFromContact.isNotBlank()) {
            return " • $labelFromContact"
        }
    }

    return getNumberOriginLabel(context, phoneNumber)
}

private fun getContactPhoneLabel(context: Context, rawNumber: String): String {
    if (rawNumber.isBlank()) return ""

    try {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?"
        val selectionArgs = arrayOf(rawNumber)

        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE))
                val customLabel = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL))

                return when (type) {
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobil"
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Geschäftlich"
                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Privat"
                    ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME,
                    ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> "Fax"
                    ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "Sonstige"
                    else -> customLabel?.takeIf { it.isNotBlank() } ?: ""
                }
            }
        }
    } catch (_: SecurityException) {
    } catch (_: Exception) {
    }

    return ""
}

fun groupCallLogsByDate(
    entries: List<CallLogEntry>,
    todayStr: String,
    yesterdayStr: String,
): List<CallGroup> {
    if (entries.isEmpty()) return emptyList()

    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val yesterdayStart = todayStart.clone() as Calendar
    yesterdayStart.add(Calendar.DAY_OF_MONTH, -1)

    val dateFormatWithDay = SimpleDateFormat("EE dd.MM.yyyy", Locale.getDefault())


    val groups = mutableListOf<CallGroup>()
    val currentGroupEntries = mutableListOf<CallLogEntry>()
    var currentTitle: String? = null

    for (entry in entries.sortedByDescending { it.date }) {
        val cal = Calendar.getInstance().apply { timeInMillis = entry.date }

        val title = when {
            cal.timeInMillis >= todayStart.timeInMillis -> todayStr
            cal.timeInMillis >= yesterdayStart.timeInMillis -> yesterdayStr
            else -> dateFormatWithDay.format(cal.time)
        }

        if (title != currentTitle && currentGroupEntries.isNotEmpty()) {
            groups.add(CallGroup(currentTitle!!, currentGroupEntries.toList()))
            currentGroupEntries.clear()
        }

        currentTitle = title
        currentGroupEntries.add(entry)
    }

    if (currentGroupEntries.isNotEmpty()) {
        groups.add(CallGroup(currentTitle!!, currentGroupEntries.toList()))
    }

    return groups
}

fun loadCallLogEntries(context: Context): List<CallLogEntry> {
    val logs = mutableListOf<CallLogEntry>()

    val cursor = context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        null,
        null,
        null,
        "${CallLog.Calls.DATE} DESC"
    )

    cursor?.use {
        val numberIndex     = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
        val typeIndex       = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
        val dateIndex       = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
        val nameIndex       = it.getColumnIndex(CallLog.Calls.CACHED_NAME)

        val presentationIndex = it.getColumnIndex(CallLog.Calls.NUMBER_PRESENTATION)

        while (it.moveToNext()) {
            val rawNumber = it.getString(numberIndex) ?: ""
            val name      = if (nameIndex >= 0) it.getString(nameIndex) else null
            val type      = it.getInt(typeIndex)
            val date      = it.getLong(dateIndex)

            val privateStr    = context.getString(R.string.private_label)
            val unknownStr    = context.getString(R.string.unknown)
            val restrictedStr = context.getString(R.string.restricted)
            val payphoneStr   = context.getString(R.string.payphone)

            val displayName = when {
                !name.isNullOrBlank() -> name.trim()

                presentationIndex >= 0 -> {
                    when (it.getInt(presentationIndex)) {
                        CallLog.Calls.PRESENTATION_ALLOWED     -> rawNumber.trim().ifBlank { unknownStr }
                        CallLog.Calls.PRESENTATION_RESTRICTED  -> privateStr
                        CallLog.Calls.PRESENTATION_UNKNOWN     -> unknownStr
                        CallLog.Calls.PRESENTATION_PAYPHONE    -> payphoneStr
                        else -> privateStr
                    }
                }

                else -> when {
                    rawNumber.isBlank()
                            || rawNumber == "-1"
                            || rawNumber == "-2"
                            || rawNumber == "-3"
                            || rawNumber.equals(unknownStr, ignoreCase = true)
                            || rawNumber.equals("P", ignoreCase = true)
                            || rawNumber.equals("Restricted", ignoreCase = true)
                            || rawNumber.equals("Withheld", ignoreCase = true)
                        -> unknownStr

                    rawNumber.equals(privateStr, ignoreCase = true)
                            || rawNumber.equals(restrictedStr, ignoreCase = true)
                        -> privateStr

                    rawNumber.equals(payphoneStr, ignoreCase = true) -> payphoneStr

                    else -> PhoneNumberFormatter.formatForDisplay(rawNumber, context)
                }
            }

            val canDial = when {
                displayName == privateStr -> false
                displayName == unknownStr -> false
                displayName == payphoneStr -> false
                rawNumber.isBlank() -> false
                rawNumber.startsWith("-") -> false
                else -> true
            }

            val phoneNumberToDial = if (canDial) rawNumber.trim() else ""

            logs.add(
                CallLogEntry(
                    nameOrNumber = displayName,
                    phoneNumber = phoneNumberToDial,
                    type = type,
                    date = date
                )
            )
        }
    }

    return logs
}
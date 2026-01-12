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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.LargePadding
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenon.mylibrary.values.SmallSpacing
import com.xenon.mylibrary.values.SmallestCornerRadius
import com.xenonware.phone.viewmodel.CallLogEntry
import com.xenonware.phone.viewmodel.Contact
import com.xenonware.phone.viewmodel.PhoneViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DialerScreen(
    modifier: Modifier = Modifier,
    onOpenHistory: () -> Unit,
    viewModel: PhoneViewModel = viewModel()
) {
    val recentCalls by viewModel.recentCalls.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    var phoneNumber by remember { mutableStateOf("") }
    val context = LocalContext.current

    val suggestions by remember(phoneNumber, recentCalls, favorites) {
        derivedStateOf { buildSuggestions(phoneNumber, recentCalls, favorites) }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(MediumCornerRadius))
                .background(colorScheme.surfaceContainerHigh)
        ) {
            when {
                suggestions.isEmpty() && phoneNumber.isEmpty() -> {
                    Text(
                        text = "Recent calls & favorites will appear here",
                        modifier = Modifier.align(Alignment.Center),
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                suggestions.isEmpty() -> {
                    Text(
                        text = "No matching results",
                        modifier = Modifier.align(Alignment.Center),
                        color = colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(SmallSpacing),
                    ) {
                        items(
                            count = suggestions.size,
                            key = { index -> suggestions[index].number + suggestions[index].title }
                        ) { index ->
                            val isFirst = index == 0
                            val isLast = index == suggestions.lastIndex
                            val isSingle = suggestions.size == 1

                            SuggestionRow(
                                item = suggestions[index],
                                onClick = { phoneNumber = suggestions[index].number },
                                isFirstInGroup = isFirst,
                                isLastInGroup = isLast,
                                isSingle = isSingle
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = phoneNumber.ifEmpty { "Enter number" },
            modifier = Modifier
                .padding(16.dp)
                .height(50.dp)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge,
            color = if (phoneNumber.isEmpty())
                colorScheme.onBackground.copy(alpha = 0.6f)
            else
                colorScheme.onBackground,
            fontFamily = QuicksandTitleVariable,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )

        Dialpad(
            onNumberClick = { digit -> phoneNumber += digit },
            onDeleteClick = {
                if (phoneNumber.isNotEmpty()) {
                    phoneNumber = phoneNumber.dropLast(1)
                }
            },
            onClearAll = { phoneNumber = "" },
            onCallClick = {
                if (phoneNumber.isNotEmpty()) {
                    safePlaceCall(context, phoneNumber)
                }
            },
            onOpenHistory = onOpenHistory
        )
    }
}

@Composable
private fun SuggestionRow(
    item: SuggestionItem,
    onClick: () -> Unit,
    isFirstInGroup: Boolean, isLastInGroup: Boolean, isSingle: Boolean
) {
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

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(shape).background(colorScheme.surfaceBright)
            .padding(16.dp)
        ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

data class SuggestionItem(
    val title: String,
    val subtitle: String,
    val number: String,
    val type: SuggestionType = SuggestionType.RECENT
)

enum class SuggestionType { RECENT, FAVORITE }

private fun buildSuggestions(
    query: String,
    recent: List<CallLogEntry>,
    favorites: List<Contact>
): List<SuggestionItem> = buildList {
    val isDigitsOnly = query.all { it.isDigit() }

    if (query.isNotEmpty()) {
        val numberMatches = (favorites + recent.map { Contact("", "", it.number) })
            .filter { contact ->
                val num = contact.phone
                num.startsWith(query) ||
                        num.removePrefix("+").startsWith(query) ||
                        num.removePrefix("00").startsWith(query)
            }
            .distinctBy { it.phone }
            .take(5)
            .map { contact ->
                if (contact.name.isNotBlank()) SuggestionItem(
                    title = contact.name,
                    subtitle = contact.phone,
                    number = contact.phone,
                    type = SuggestionType.FAVORITE
                ) else SuggestionItem(
                    title = contact.phone,
                    subtitle = "Recent",
                    number = contact.phone
                )
            }
        addAll(numberMatches)
    }

    if (isDigitsOnly && query.isNotEmpty() && query.none { it in "01*#" }) {
        val nameMatches = favorites
            .filter { contact ->
                val normalized = normalizeName(contact.name)
                matchesT9(normalized, query)
            }
            .take(8)
            .map {
                SuggestionItem(
                    title = it.name,
                    subtitle = it.phone,
                    number = it.phone,
                    type = SuggestionType.FAVORITE
                )
            }
            .sortedBy { it.title }

        addAll(nameMatches)
    }
    if (query.isEmpty()) {
        favorites.take(6).forEach {
            add(SuggestionItem(it.name, it.phone, it.phone, SuggestionType.FAVORITE))
        }
        recent.take(8).forEach {
            add(SuggestionItem(it.number, formatRelativeTime(it.timestamp), it.number))
        }
    }
}
private fun normalizeName(name: String): String {
    return name.lowercase()
        .replace(Regex("[^a-z]"), "")
}

private fun matchesT9(nameNormalized: String, digits: String): Boolean {
    if (digits.isEmpty()) return true

    var nameIndex = 0
    for (digit in digits) {
        val possibleLetters = t9Map[digit] ?: continue
        var found = false

        while (nameIndex < nameNormalized.length) {
            if (possibleLetters.contains(nameNormalized[nameIndex])) {
                found = true
                nameIndex++
                break
            }
            nameIndex++
        }

        if (!found) return false
    }
    return true
}
private val t9Map = mapOf(
    '2' to "abc",
    '3' to "def",
    '4' to "ghi",
    '5' to "jkl",
    '6' to "mno",
    '7' to "pqrs",
    '8' to "tuv",
    '9' to "wxyz"
)

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 90_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000} min ago"
        diff < 86_400_000L -> "${diff / 3_600_000} h ago"
        diff < 604_800_000L -> "${diff / 86_400_000} days ago"
        else -> SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun Dialpad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearAll: () -> Unit,
    onCallClick: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenHeightDp = configuration.screenHeightDp.dp

    val extraBottomPadding = 64.dp + LargePadding * 2
    val safeBottomPadding =
        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues()
            .calculateBottomPadding()

    val safeTopPadding =
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()

    val callButtonHeight = 82.dp + LargestPadding

    val targetTotalHeight =
        screenHeightDp * 0.70f - extraBottomPadding - safeTopPadding - safeBottomPadding - callButtonHeight - 82.dp

    val spacing = 8.dp
    val totalSpacing = spacing * 3

    val buttonHeight = (targetTotalHeight - totalSpacing) / 4

    val digitTextSize = with(density) { (buttonHeight.toPx() * 0.48f).toSp() }
    val letterTextSize = with(density) { (buttonHeight.toPx() * 0.185f).toSp() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp
            )
            .padding(bottom = extraBottomPadding + safeBottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
        val letters =
            listOf("", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "+", "")

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
                        .height(buttonHeight)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceBright)
                        .combinedClickable(onClick = { onNumberClick(key) }, onLongClick = {
                            if (key == "0") onNumberClick("+") else onNumberClick(key)
                        })
                ) {
                    Text(
                        text = key,
                        fontSize = digitTextSize,
                        fontFamily = QuicksandTitleVariable,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = letter,
                        fontSize = letterTextSize,
                        style = LocalTextStyle.current.copy(lineHeight = letterTextSize),
                        fontFamily = QuicksandTitleVariable,
                        fontWeight = FontWeight.ExtraLight,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.offset(y = (-2).dp)
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
                    .clip(CircleShape), onClick = onOpenHistory
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = "Call log",
                    modifier = Modifier.size(28.sp.value.dp)
                )
            }

            FilledTonalIconButton(
                onClick = onCallClick, colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFF4CAF50), contentColor = colorScheme.onSurface
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
                            val duration = System.currentTimeMillis() - pressStartTime
                            if (duration >= 420L) {
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
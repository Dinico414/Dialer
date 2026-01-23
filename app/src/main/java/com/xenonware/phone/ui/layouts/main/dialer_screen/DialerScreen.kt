@file:Suppress("DEPRECATION")

package com.xenonware.phone.ui.layouts.main.dialer_screen

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Voicemail
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenon.mylibrary.values.SmallSpacing
import com.xenon.mylibrary.values.SmallestCornerRadius
import com.xenonware.phone.R
import com.xenonware.phone.data.Contact
import com.xenonware.phone.ui.layouts.main.contacts.ContactAvatar
import com.xenonware.phone.viewmodel.CallLogEntry
import com.xenonware.phone.viewmodel.PhoneViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DialerScreen(
    modifier: Modifier = Modifier,
    onOpenHistory: () -> Unit,
    viewModel: PhoneViewModel = viewModel(),
    contentPadding: PaddingValues
) {
    val incomingNumber by viewModel.incomingPhoneNumber.collectAsState()
    val recentCalls by viewModel.recentCalls.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val allContacts by viewModel.contacts.collectAsState()
    val indexedContacts by viewModel._indexedContacts.collectAsState()

    var phoneNumber by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    val context = LocalContext.current

    LaunchedEffect(phoneNumber) {
        delay(280)
        debouncedQuery = phoneNumber.trim()
    }

    val currentSuggestions by remember(debouncedQuery, recentCalls, favorites, indexedContacts) {
        derivedStateOf {
            buildSuggestions(
                query = debouncedQuery,
                recent = recentCalls,
                favorites = favorites,
                indexedContacts = indexedContacts
            )
        }
    }

    var cachedSuggestions by remember { mutableStateOf<List<SuggestionItem>>(emptyList()) }

    LaunchedEffect(currentSuggestions) {
        if (currentSuggestions.isNotEmpty()) {
            cachedSuggestions = currentSuggestions
        }
    }

    val suggestionsToShow = when {
        currentSuggestions.isNotEmpty() -> currentSuggestions
        debouncedQuery.isEmpty() -> emptyList()
        else -> cachedSuggestions
    }

    LaunchedEffect(incomingNumber) {
        if (incomingNumber != null && phoneNumber.isEmpty()) {
            phoneNumber = incomingNumber ?: ""
        }
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
        ) {
            when {
                suggestionsToShow.isEmpty() && debouncedQuery.isEmpty() -> {
                    Text(
                        text = stringResource(id = R.string.no_suggestions),
                        modifier = Modifier.align(Alignment.Center),
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                suggestionsToShow.isEmpty() -> {
                    Text(
                        text = stringResource(id = R.string.no_match),
                        modifier = Modifier.align(Alignment.Center),
                        color = colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(SmallSpacing),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = suggestionsToShow,
                            key = { _, item -> item.id }
                        ) { index, item ->
                            val isFirst = index == 0
                            val isLast = index == suggestionsToShow.lastIndex
                            val isSingle = suggestionsToShow.size == 1

                            val matchingContact = remember(item.number) {
                                allContacts.find {
                                    PhoneViewModel.normalizePhone(it.phone) == PhoneViewModel.normalizePhone(item.number)
                                }
                            }

                            SuggestionRow(
                                item = item,
                                onClick = { phoneNumber = item.number },
                                isFirstInGroup = isFirst,
                                isLastInGroup = isLast,
                                isSingle = isSingle,
                                matchingContact = matchingContact
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = phoneNumber.ifEmpty { stringResource(R.string.enter_phone_number) },
            modifier = Modifier
                .padding(16.dp)
                .height(50.dp)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            vibrateFeedback(context)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            if (phoneNumber.isEmpty()) {
                                val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
                                if (clipText.isNotBlank() && clipText.all { it.isDigit() || it in "+*#-" }) {
                                    phoneNumber = clipText
                                }
                            } else {
                                val clip = ClipData.newPlainText("Phone number", phoneNumber)
                                clipboard.setPrimaryClip(clip)
                            }
                        }
                    )
                },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge,
            color = if (phoneNumber.isEmpty()) colorScheme.onBackground.copy(alpha = 0.6f)
            else colorScheme.onBackground,
            fontFamily = QuicksandTitleVariable,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )

        Dialpad(
            onNumberClick = { digit -> phoneNumber += digit },
            onDeleteClick = {
                if (phoneNumber.isNotEmpty()) phoneNumber = phoneNumber.dropLast(1)
            },
            onClearAll = { phoneNumber = "" },
            onCallClick = {
                if (phoneNumber.isNotEmpty()) {
                    safePlaceCall(context, phoneNumber)
                }
            },
            onOpenHistory = onOpenHistory,
            contentPadding = contentPadding
        )
    }
}

@Composable
private fun SuggestionRow(
    item: SuggestionItem,
    onClick: () -> Unit,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    isSingle: Boolean,
    matchingContact: Contact?
) {
    val shape = when {
        isSingle -> RoundedCornerShape(
            topStart = SmallestCornerRadius,
            topEnd = SmallestCornerRadius,
            bottomStart = MediumCornerRadius,
            bottomEnd = MediumCornerRadius
        )

        isFirstInGroup -> RoundedCornerShape(
            topStart = SmallestCornerRadius,
            topEnd = SmallestCornerRadius,
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        color = colorScheme.surfaceBright,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (matchingContact != null) {
                ContactAvatar(
                    contact = matchingContact, modifier = Modifier.size(48.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#",
                        fontSize = 22.sp,
                        fontFamily = QuicksandTitleVariable,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = QuicksandTitleVariable,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

data class SuggestionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val number: String,
    val type: SuggestionType = SuggestionType.RECENT_CONTACT,
)

enum class SuggestionType { RECENT_CONTACT, FAVORITE }

private fun buildSuggestions(
    query: String,
    recent: List<CallLogEntry>,
    favorites: List<Contact>,
    indexedContacts: List<PhoneViewModel.IndexedContact>
): List<SuggestionItem> = buildList {

    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
        val frequencyMap = mutableMapOf<String, Int>()

        recent.forEach { call ->
            val matchingContact = indexedContacts.find {
                it.normalizedPhone == PhoneViewModel.normalizePhone(call.number)
            }
            if (matchingContact != null) {
                frequencyMap[matchingContact.contact.id] = (frequencyMap[matchingContact.contact.id] ?: 0) + 1
            }
        }

        val recentCalledContacts = indexedContacts
            .filter { it.contact.id in frequencyMap }
            .sortedWith(
                compareByDescending { indexed ->
                    val callCount = frequencyMap[indexed.contact.id] ?: 0
                    val isFavorite = favorites.any { it.id == indexed.contact.id }
                    callCount + if (isFavorite) 2.5 else 0.0
                }
            )
            .take(12)

        recentCalledContacts.forEach { indexed ->
            val contact = indexed.contact
            val isFav = favorites.any { it.id == contact.id }
            add(
                SuggestionItem(
                    id = if (isFav) "fav_${contact.id}" else "recent_contact_${contact.id}",
                    title = contact.name.ifBlank { contact.phone },
                    subtitle = contact.phone,
                    number = contact.phone,
                    type = if (isFav) SuggestionType.FAVORITE else SuggestionType.RECENT_CONTACT
                )
            )
        }
        return@buildList
    }

    val cleanQuery = trimmed.replace(Regex("[^+0-9*#-]"), "")
    val isDigitInput = trimmed.all { it.isDigit() || it in "+*#-" }

    val matching = indexedContacts.asSequence()
        .filter { indexed ->
            indexed.normalizedPhone.startsWith(cleanQuery) ||
                    indexed.normalizedPhone.contains(cleanQuery) ||
                    (isDigitInput && matchesT9Fast(indexed.t9Keys, cleanQuery))
        }
        .map { it.contact }
        .take(20)
        .toList()

    val sorted = matching.sortedWith(compareByDescending<Contact> { c ->
        val score = when {
            c.name.startsWith(trimmed, ignoreCase = true) -> 5
            matchesT9Fast(
                indexedContacts.first { it.contact.id == c.id }.t9Keys,
                cleanQuery
            ) -> 4
            c.phone.startsWith(trimmed) -> 3
            c.phone.contains(trimmed) -> 2
            else -> 1
        }
        score
    }.thenBy { it.name.lowercase() })
        .take(12)

    sorted.forEach { contact ->
        val isFav = favorites.any { it.id == contact.id }
        add(
            SuggestionItem(
                id = if (isFav) "fav_${contact.id}" else "contact_${contact.id}",
                title = contact.name.ifBlank { contact.phone },
                subtitle = contact.phone,
                number = contact.phone,
                type = if (isFav) SuggestionType.FAVORITE else SuggestionType.RECENT_CONTACT
            )
        )
    }
}

private fun matchesT9Fast(t9Keys: String, query: String): Boolean {
    if (query.isEmpty()) return true
    if (t9Keys.length < query.length) return false

    var i = 0
    for (digit in query) {
        var found = false
        while (i < t9Keys.length) {
            if (t9Keys[i] == digit) {
                found = true
                i++
                break
            }
            i++
        }
        if (!found) return false
    }
    return true
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun Dialpad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearAll: () -> Unit,
    onCallClick: () -> Unit,
    onOpenHistory: () -> Unit,
    contentPadding: PaddingValues
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val context = LocalContext.current

    val screenHeightDp = configuration.screenHeightDp.dp

    val bottomPadding = contentPadding.calculateBottomPadding()

    val safeTopPadding =
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()

    val callButtonHeight = 82.dp + LargestPadding
    val textFieldHeight = 50.dp + LargestPadding * 2

    val targetTotalHeight =
        screenHeightDp * 0.70f - safeTopPadding - bottomPadding - callButtonHeight - textFieldHeight

    val spacing = 8.dp
    val totalSpacing = spacing * 3

    val buttonHeight = (targetTotalHeight - totalSpacing) / 4

    val digitTextSize = with(density) { (buttonHeight.toPx() * 0.48f).toSp() }
    val letterTextSize = with(density) {
        (buttonHeight.toPx() * 0.185f).coerceAtLeast(0f).toSp()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomPadding), horizontalAlignment = Alignment.CenterHorizontally
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

                Box(
                    modifier = Modifier
                        .height(buttonHeight)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceBright)
                        .combinedClickable(
                            onClick = { onNumberClick(key) },
                            onLongClick = {
                                if (key == "0") onNumberClick("+") else onNumberClick(key)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
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

                    if (key == "1") {
                        Icon(
                            imageVector = Icons.Rounded.Voicemail,
                            contentDescription = "Voicemail",
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 5.dp)
                        )
                    }
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
                    .clip(CircleShape), onClick = onOpenHistory,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary)
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
                    .clip(CircleShape), onClick = {
                    onDeleteClick()
                    vibrateFeedback(context)
                }, interactionSource = interactionSource
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "Delete",
                    modifier = Modifier.size(28.sp.value.dp)
                )
            }

            LaunchedEffect(interactionSource) {
                var hasVibratedForThisPress = false
                var pressStart: Long? = null

                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> {
                            pressStart = System.currentTimeMillis()
                            hasVibratedForThisPress = false

                            launch {
                                delay(420L)

                                if (pressStart != null && !hasVibratedForThisPress) {
                                    vibrateFeedback(context, 45L, 110)
                                    onClearAll()
                                    hasVibratedForThisPress = true
                                }
                            }
                        }

                        is PressInteraction.Release -> {
                            if (pressStart != null && !hasVibratedForThisPress) {
                                val held = System.currentTimeMillis() - pressStart!!
                                if (held < 420L) {
                                    vibrateFeedback(context, 5L, 5)
                                }
                            }
                            pressStart = null
                            hasVibratedForThisPress = false
                        }

                        is PressInteraction.Cancel -> {
                            pressStart = null
                            hasVibratedForThisPress = false
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("ObsoleteSdkInt")
fun safePlaceCall(context: Context, phoneNumber: String) {
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    val uri = "tel:$phoneNumber".toUri()
    val permissionDeniedString = context.getString(R.string.permission_denied)

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
            Toast.makeText(context, permissionDeniedString, Toast.LENGTH_SHORT).show()
            fallbackCallIntent(context, uri)
        }
    } else {
        fallbackCallIntent(context, uri)
    }
}

private fun fallbackCallIntent(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_CALL, uri)
    val callFailedString = context.getString(R.string.call_failed)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, callFailedString, Toast.LENGTH_SHORT).show()
    }
}

private fun vibrateFeedback(context: Context, durationMs: Long = 35L, amplitude: Int = 90) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator ?: return
    vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, amplitude))
}
@file:Suppress("DEPRECATION")

package com.xenonware.phone.ui.layouts.main.dialer_screen

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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
import com.xenonware.phone.data.SharedPreferenceManager
import com.xenonware.phone.ui.layouts.main.contacts.ContactAvatar
import com.xenonware.phone.util.PhoneNumberFormatter
import com.xenonware.phone.viewmodel.CallLogEntry
import com.xenonware.phone.viewmodel.IndexedContact
import com.xenonware.phone.viewmodel.PhoneViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DialerScreen(
    modifier: Modifier = Modifier,
    viewModel: PhoneViewModel = viewModel(),
    contentPadding: PaddingValues,
    isCoverMode: Boolean = false,
    isAppBarExpandable: Boolean = true
) {
    val incomingNumber by viewModel.incomingPhoneNumber.collectAsState()
    val recentCalls by viewModel.recentCalls.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val allContacts by viewModel.contacts.collectAsState()
    val indexedContacts by viewModel.indexedContacts.collectAsState()

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
            phoneNumber = (incomingNumber ?: "").replace(" ", "")
            viewModel.setIncomingPhoneNumber(null)
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isAppBarExpandable && !isLandscape) {
        // --- 1. DEFAULT VERTICAL LAYOUT ---
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = if (isCoverMode) 0.dp else 16.dp)
                    .padding(top = if (isCoverMode) 0.dp else 16.dp)
                    .clip(RoundedCornerShape(MediumCornerRadius))
            ) {
                when {
                    suggestionsToShow.isEmpty() && debouncedQuery.isEmpty() -> {
                        Text(
                            text = stringResource(id = R.string.no_suggestions),
                            modifier = Modifier.align(Alignment.Center),
                            color = colorScheme.onSurfaceVariant,
                            fontFamily = QuicksandTitleVariable,
                            style = typography.titleLarge
                        )
                    }
                    suggestionsToShow.isEmpty() -> {
                        Text(
                            text = stringResource(id = R.string.no_match),
                            modifier = Modifier.align(Alignment.Center),
                            color = colorScheme.onSurfaceVariant,
                            fontFamily = QuicksandTitleVariable,
                            style = typography.titleLarge
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
                                val matchingContact = remember(item.number) {
                                    allContacts.find {
                                        PhoneViewModel.normalizePhone(it.phone) == PhoneViewModel.normalizePhone(item.number)
                                    }
                                }
                                SuggestionRow(
                                    item = item,
                                    onClick = { phoneNumber = item.number.replace(" ", "") },
                                    isFirstInGroup = index == 0,
                                    isLastInGroup = index == suggestionsToShow.lastIndex,
                                    isSingle = suggestionsToShow.size == 1,
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
                                    val cleanClipText = clipText.replace(" ", "")
                                    if (cleanClipText.isNotBlank() && cleanClipText.all { it.isDigit() || it in "+*#-" }) {
                                        phoneNumber = cleanClipText
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

            DialpadVertical(
                phoneNumber = phoneNumber,
                onNumberClick = { digit -> phoneNumber += digit },
                onDeleteClick = { if (phoneNumber.isNotEmpty()) phoneNumber = phoneNumber.dropLast(1) },
                onClearAll = { phoneNumber = "" },
                onCallClick = { if (phoneNumber.isNotEmpty()) safePlaceCall(context, phoneNumber) },
                contentPadding = contentPadding,
                isCoverMode = isCoverMode
            )
        }
    } else {
        // --- 2. COMPACT & WIDE COMPACT LAYOUTS (BoxWithConstraints Root) ---
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isWide = maxWidth > maxHeight * 1.5f
            val constraintsMaxHeight = maxHeight
            val density = LocalDensity.current
            val phoneFieldHeight = (constraintsMaxHeight * (if (isWide) 0.12f else 0.09f)).coerceAtMost(50.dp)
            val phoneFieldPadding = (phoneFieldHeight * 0.32f).coerceAtLeast(4.dp)
            val phoneFieldTextSize = with(density) {
                (phoneFieldHeight.toPx() * 0.64f).coerceAtLeast(12f).toSp()
            }


            if (isWide) {
                // Wide Landscape Split
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = if (isCoverMode) 0.dp else 16.dp)
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(topStart = MediumCornerRadius, topEnd = MediumCornerRadius, bottomStart = 0.dp, bottomEnd = 0.dp))
                    ) {
                        if (suggestionsToShow.isEmpty() && debouncedQuery.isEmpty()) {
                            Text(text = stringResource(id = R.string.no_suggestions), modifier = Modifier.align(Alignment.Center), color = colorScheme.onSurfaceVariant, fontFamily = QuicksandTitleVariable, style = typography.titleLarge)
                        } else if (suggestionsToShow.isEmpty()) {
                            Text(text = stringResource(id = R.string.no_match), modifier = Modifier.align(Alignment.Center), color = colorScheme.onSurfaceVariant, fontFamily = QuicksandTitleVariable, style = typography.titleLarge)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(SmallSpacing), modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(items = suggestionsToShow, key = { _, item -> item.id }) { index, item ->
                                    val matchingContact = remember(item.number) { allContacts.find { PhoneViewModel.normalizePhone(it.phone) == PhoneViewModel.normalizePhone(item.number) } }
                                    SuggestionRow(item = item, onClick = { phoneNumber = item.number.replace(" ", "") }, isFirstInGroup = index == 0, isLastInGroup = index == suggestionsToShow.lastIndex, isSingle = suggestionsToShow.size == 1, matchingContact = matchingContact)
                                }
                                item { Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding())) }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = phoneNumber.ifEmpty { stringResource(R.string.enter_phone_number) },
                            modifier = Modifier.padding(phoneFieldPadding).height(phoneFieldHeight).fillMaxWidth().horizontalScroll(rememberScrollState()).pointerInput(Unit) {
                                detectTapGestures(onLongPress = {
                                    vibrateFeedback(context)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    if (phoneNumber.isEmpty()) {
                                        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
                                        val cleanClipText = clipText.replace(" ", "")
                                        if (cleanClipText.isNotBlank() && cleanClipText.all { it.isDigit() || it in "+*#-" }) phoneNumber = cleanClipText
                                    } else clipboard.setPrimaryClip(ClipData.newPlainText("Phone number", phoneNumber))
                                })
                            },
                            textAlign = TextAlign.Center,
                            style = typography.headlineLarge,
                            fontSize = phoneFieldTextSize,
                            color = if (phoneNumber.isEmpty()) colorScheme.onBackground.copy(alpha = 0.6f) else colorScheme.onBackground,
                            fontFamily = QuicksandTitleVariable,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        DialpadCompact(phoneNumber = phoneNumber, onNumberClick = { phoneNumber += it }, onDeleteClick = { if (phoneNumber.isNotEmpty()) phoneNumber = phoneNumber.dropLast(1) }, onClearAll = { phoneNumber = "" }, onCallClick = { if (phoneNumber.isNotEmpty()) safePlaceCall(context, phoneNumber) }, contentPadding = contentPadding, isCoverMode = isCoverMode, maxAvailableHeight = constraintsMaxHeight, phoneFieldHeight = phoneFieldHeight, phoneFieldPadding = phoneFieldPadding)
                    }
                }
            } else {
                // Portrait Compact
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = if (isCoverMode) 0.dp else 16.dp).padding(top = if (isCoverMode) 0.dp else 16.dp).clip(RoundedCornerShape(MediumCornerRadius))) {
                        if (suggestionsToShow.isEmpty() && debouncedQuery.isEmpty()) {
                            Text(text = stringResource(id = R.string.no_suggestions), modifier = Modifier.align(Alignment.Center), color = colorScheme.onSurfaceVariant, fontFamily = QuicksandTitleVariable, style = typography.titleLarge)
                        } else if (suggestionsToShow.isEmpty()) {
                            Text(text = stringResource(id = R.string.no_match), modifier = Modifier.align(Alignment.Center), color = colorScheme.onSurfaceVariant, fontFamily = QuicksandTitleVariable, style = typography.titleLarge)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(SmallSpacing), modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(items = suggestionsToShow, key = { _, item -> item.id }) { index, item ->
                                    val matchingContact = remember(item.number) { allContacts.find { PhoneViewModel.normalizePhone(it.phone) == PhoneViewModel.normalizePhone(item.number) } }
                                    SuggestionRow(item = item, onClick = { phoneNumber = item.number.replace(" ", "") }, isFirstInGroup = index == 0, isLastInGroup = index == suggestionsToShow.lastIndex, isSingle = suggestionsToShow.size == 1, matchingContact = matchingContact)
                                }
                            }
                        }
                    }
                    Text(
                        text = phoneNumber.ifEmpty { stringResource(R.string.enter_phone_number) },
                        modifier = Modifier.padding(phoneFieldPadding).height(phoneFieldHeight).fillMaxWidth().horizontalScroll(rememberScrollState()).pointerInput(Unit) {
                            detectTapGestures(onLongPress = {
                                vibrateFeedback(context)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                if (phoneNumber.isEmpty()) {
                                    val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
                                    val cleanClipText = clipText.replace(" ", "")
                                    if (cleanClipText.isNotBlank() && cleanClipText.all { it.isDigit() || it in "+*#-" }) phoneNumber = cleanClipText
                                } else clipboard.setPrimaryClip(ClipData.newPlainText("Phone number", phoneNumber))
                            })
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = phoneFieldTextSize,
                        color = if (phoneNumber.isEmpty()) colorScheme.onBackground.copy(alpha = 0.6f) else colorScheme.onBackground,
                        fontFamily = QuicksandTitleVariable,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    DialpadCompactPortrait(
                        phoneNumber = phoneNumber,
                        onNumberClick = { phoneNumber += it },
                        onDeleteClick = { if (phoneNumber.isNotEmpty()) phoneNumber = phoneNumber.dropLast(1) },
                        onClearAll = { phoneNumber = "" },
                        onCallClick = { if (phoneNumber.isNotEmpty()) safePlaceCall(context, phoneNumber) },
                        contentPadding = contentPadding,
                        phoneFieldHeight = phoneFieldHeight,
                        phoneFieldPadding = phoneFieldPadding
                    )
                }
            }
        }
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
        isSingle -> RoundedCornerShape(topStart = SmallestCornerRadius, topEnd = SmallestCornerRadius, bottomStart = MediumCornerRadius, bottomEnd = MediumCornerRadius)
        isFirstInGroup -> RoundedCornerShape(topStart = SmallestCornerRadius, topEnd = SmallestCornerRadius, bottomStart = SmallestCornerRadius, bottomEnd = SmallestCornerRadius)
        isLastInGroup -> RoundedCornerShape(topStart = SmallestCornerRadius, topEnd = SmallestCornerRadius, bottomStart = MediumCornerRadius, bottomEnd = MediumCornerRadius)
        else -> RoundedCornerShape(SmallestCornerRadius)
    }
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxWidth().clip(shape), shape = shape, color = colorScheme.surfaceBright, onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (matchingContact != null) ContactAvatar(contact = matchingContact, modifier = Modifier.size(48.dp))
            else Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) { Text(text = "#", fontSize = 22.sp, fontFamily = QuicksandTitleVariable, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant) }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontFamily = QuicksandTitleVariable, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = PhoneNumberFormatter.formatForDisplay(item.subtitle, context), style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

data class SuggestionItem(val id: String, val title: String, val subtitle: String, val number: String, val type: SuggestionType = SuggestionType.RECENT_CONTACT)
enum class SuggestionType { RECENT_CONTACT, FAVORITE }

private fun buildSuggestions(query: String, recent: List<CallLogEntry>, favorites: List<Contact>, indexedContacts: List<IndexedContact>): List<SuggestionItem> = buildList {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
        val frequencyMap = mutableMapOf<String, Int>()
        recent.forEach { call ->
            val matchingContact = indexedContacts.find { it.normalizedPhone == PhoneViewModel.normalizePhone(call.number) }
            if (matchingContact != null) frequencyMap[matchingContact.contact.id] = (frequencyMap[matchingContact.contact.id] ?: 0) + 1
        }
        val sorted = indexedContacts.filter { it.contact.id in frequencyMap }.sortedWith(compareByDescending { indexed -> (frequencyMap[indexed.contact.id] ?: 0) + if (favorites.any { it.id == indexed.contact.id }) 2.5 else 0.0 }).take(12)
        sorted.forEach { indexed -> add(SuggestionItem(id = if (favorites.any { it.id == indexed.contact.id }) "fav_${indexed.contact.id}" else "recent_${indexed.contact.id}", title = indexed.contact.name.ifBlank { indexed.contact.phone }, subtitle = indexed.contact.phone, number = indexed.contact.phone)) }
        return@buildList
    }
    val normalizedQuery = PhoneViewModel.normalizePhone(trimmed.replace(Regex("[^+0-9*#-]"), ""))
    val isDigit = trimmed.all { it.isDigit() || it in "+*#-" }
    val multiTap = if (isDigit) PhoneViewModel.multiTapToT9(trimmed) else ""
    val matching = indexedContacts.asSequence().filter { indexed -> indexed.normalizedPhone.contains(normalizedQuery) || (isDigit && (PhoneViewModel.matchesT9(indexed.t9Keys, trimmed) || (multiTap.isNotEmpty() && PhoneViewModel.matchesT9(indexed.t9Keys, multiTap)))) }.map { it.contact }.take(20).toList()
    matching.sortedWith(compareByDescending<Contact> { c -> if (c.name.startsWith(trimmed, ignoreCase = true)) 5 else 1 }.thenBy { it.name.lowercase() }).take(12).forEach { contact -> add(SuggestionItem(id = "contact_${contact.id}", title = contact.name.ifBlank { contact.phone }, subtitle = contact.phone, number = contact.phone)) }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun DialpadVertical(
    phoneNumber: String,
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearAll: () -> Unit,
    onCallClick: () -> Unit,
    contentPadding: PaddingValues,
    isCoverMode: Boolean = false
) {
    val viewModel: PhoneViewModel = viewModel()
    val prefs = SharedPreferenceManager(LocalContext.current)
    val configuration = LocalConfiguration.current; val density = LocalDensity.current; val context = LocalContext.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val bottomPadding = contentPadding.calculateBottomPadding()
    val safeTopPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()
    val callButtonHeight = 82.dp + LargestPadding; val textFieldHeight = 50.dp + LargestPadding * 2
    val targetTotalHeight = screenHeightDp * 0.70f - safeTopPadding - bottomPadding - callButtonHeight - textFieldHeight
    val spacing = 8.dp; val totalSpacing = spacing * 3
    var buttonHeight = (targetTotalHeight - totalSpacing) / 4
    if (buttonHeight < 48.dp) buttonHeight = 48.dp
    val digitTextSize = with(density) { (buttonHeight.toPx() * 0.48f).coerceAtLeast(16f).toSp() }
    val letterTextSize = with(density) { (buttonHeight.toPx() * 0.185f).coerceAtLeast(8f).toSp() }
    val iconSize = with(density) { (buttonHeight.toPx() * 0.20f).coerceAtLeast(12f).toSp() }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = if (isCoverMode) 0.dp else 16.dp).padding(bottom = bottomPadding), horizontalAlignment = Alignment.CenterHorizontally) {
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
        val letters = listOf("ↈ", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "+", "")
        LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            items(12) { index ->
                val key = keys[index]; val letter = letters[index]; val interactionSource = remember { MutableInteractionSource() }; val isPressed by interactionSource.collectIsPressedAsState(); var longPressTriggered by remember { mutableStateOf(false) }
                LaunchedEffect(isPressed) {
                    if (isPressed) { delay(480L); if (isPressed) { longPressTriggered = true; when (key) { "1" -> viewModel.startVoicemailCall(); in "2".."9" -> { val entered = phoneNumber.trim(); if (entered.isNotBlank()) { prefs.saveQuickDial(key.toInt(), entered); Toast.makeText(context, "Saved to Quick Dial ${key}", Toast.LENGTH_SHORT).show() } else { if (prefs.hasQuickDial(key.toInt())) viewModel.startCall(prefs.getQuickDialNumber(key.toInt())!!) else Toast.makeText(context, "No Quickdial set yet", Toast.LENGTH_LONG).show() } }; "0" -> onNumberClick("+") } } } else longPressTriggered = false
                }
                Box(modifier = Modifier.height(buttonHeight).clip(CircleShape).background(colorScheme.surfaceBright).clickable(interactionSource = interactionSource, onClick = { if (!longPressTriggered) onNumberClick(key) }), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(text = key, fontSize = digitTextSize, fontFamily = QuicksandTitleVariable, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                        Text(text = letter, fontSize = if (key == "1" || key == "0") iconSize else letterTextSize, style = LocalTextStyle.current.copy(lineHeight = if (key == "1" || key == "0") iconSize else letterTextSize), fontFamily = if (key == "1") FontFamily(Font(R.font.voicemailfont)) else QuicksandTitleVariable, fontWeight = if (key == "1") FontWeight.Bold else FontWeight.ExtraLight, color = colorScheme.onSurfaceVariant, modifier = Modifier.offset(y = (-2).dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(LargestPadding))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
            FilledTonalIconButton(onClick = onCallClick, colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF4CAF50), contentColor = colorScheme.onSurface), modifier = Modifier.weight(2f).height(82.dp).clip(RoundedCornerShape(50.dp))) { Icon(imageVector = Icons.Rounded.Call, contentDescription = "Call", modifier = Modifier.size(36.sp.value.dp)) }
            val interactionSource = remember { MutableInteractionSource() }
            FilledTonalIconButton(modifier = Modifier.weight(1f).height(82.dp).clip(RoundedCornerShape(50.dp)), onClick = { onDeleteClick(); vibrateFeedback(context) }, interactionSource = interactionSource) { Icon(imageVector = Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Delete", modifier = Modifier.size(28.sp.value.dp)) }
            LaunchedEffect(interactionSource) { var pressStart: Long? = null; interactionSource.interactions.collect { interaction -> when (interaction) { is PressInteraction.Press -> { pressStart = System.currentTimeMillis(); launch { delay(420L); if (pressStart != null) { vibrateFeedback(context, 45L, 110); onClearAll(); pressStart = null } } }; is PressInteraction.Release -> { if (pressStart != null) vibrateFeedback(context, 5L, 5); pressStart = null }; is PressInteraction.Cancel -> pressStart = null } } }
        }
    }
}

@Composable
fun DialpadCompact(
    phoneNumber: String,
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearAll: () -> Unit,
    onCallClick: () -> Unit,
    contentPadding: PaddingValues,
    isCoverMode: Boolean = false,
    maxAvailableHeight: androidx.compose.ui.unit.Dp,
    phoneFieldHeight: androidx.compose.ui.unit.Dp = 50.dp,
    phoneFieldPadding: androidx.compose.ui.unit.Dp = 16.dp
) {
    val density = LocalDensity.current; val context = LocalContext.current
    val bottomPadding = contentPadding.calculateBottomPadding(); val spacing = 4.dp
    val textFieldTotalHeight = phoneFieldHeight + phoneFieldPadding * 2
    val baseAvailableHeight = (maxAvailableHeight - bottomPadding - textFieldTotalHeight - 2.dp).coerceAtLeast(0.dp)
    val buttonHeight = (baseAvailableHeight - (spacing * 3)).coerceAtLeast(0.dp) / 4
    val digitTextSize = with(density) { (buttonHeight.toPx() * 0.48f).coerceAtLeast(16f).toSp() }
    val letterTextSize = with(density) { (buttonHeight.toPx() * 0.185f).coerceAtLeast(8f).toSp() }
    val iconSize = with(density) { (buttonHeight.toPx() * 0.20f).coerceAtLeast(12f).toSp() }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = bottomPadding), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
        val letters = listOf("ↈ", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "+", "")
        LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f).height(buttonHeight * 4 + spacing * 3), userScrollEnabled = false) {
            items(12) { index ->
                val key = keys[index]; val letter = letters[index]; val interactionSource = remember { MutableInteractionSource() }
                Box(modifier = Modifier.height(buttonHeight).clip(CircleShape).background(colorScheme.surfaceBright).clickable(interactionSource = interactionSource, onClick = { onNumberClick(key) }), contentAlignment = Alignment.Center) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(text = key, fontSize = digitTextSize, fontFamily = QuicksandTitleVariable, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface) }
                        Box(modifier = Modifier.weight(3f), contentAlignment = Alignment.Center) {
                            if (letter.isNotBlank()) {
                                val currentSize = if (key == "1" || key == "0") iconSize else letterTextSize
                                Text(text = letter, fontSize = (currentSize.value + 2).sp, style = LocalTextStyle.current.copy(lineHeight = (currentSize.value + 2).sp), fontFamily = if (key == "1") FontFamily(Font(R.font.voicemailfont)) else QuicksandTitleVariable, fontWeight = if (key == "1") FontWeight.Bold else FontWeight.ExtraLight, color = colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        Column(modifier = Modifier.width(48.dp).height(buttonHeight * 4 + spacing * 3), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(onClick = onCallClick, colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF4CAF50), contentColor = colorScheme.onSurface), modifier = Modifier.weight(2f).fillMaxWidth().clip(RoundedCornerShape(50.dp))) { Icon(imageVector = Icons.Rounded.Call, contentDescription = "Call", modifier = Modifier.size(27.dp)) }
            val backInteraction = remember { MutableInteractionSource() }
            FilledTonalIconButton(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(50.dp)), onClick = { onDeleteClick(); vibrateFeedback(context) }, interactionSource = backInteraction) { Icon(imageVector = Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Delete", modifier = Modifier.size(21.dp)) }
            LaunchedEffect(backInteraction) { var pressStart: Long? = null; backInteraction.interactions.collect { interaction -> when (interaction) { is PressInteraction.Press -> { pressStart = System.currentTimeMillis(); launch { delay(420L); if (pressStart != null) { vibrateFeedback(context, 45L, 110); onClearAll(); pressStart = null } } }; is PressInteraction.Release -> { if (pressStart != null) vibrateFeedback(context, 5L, 5); pressStart = null }; is PressInteraction.Cancel -> pressStart = null } } }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun DialpadCompactPortrait(
    phoneNumber: String,
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearAll: () -> Unit,
    onCallClick: () -> Unit,
    contentPadding: PaddingValues,
    phoneFieldHeight: androidx.compose.ui.unit.Dp = 50.dp,
    phoneFieldPadding: androidx.compose.ui.unit.Dp = 16.dp
) {
    val viewModel: PhoneViewModel = viewModel()
    val prefs = SharedPreferenceManager(LocalContext.current)
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val context = LocalContext.current

    val screenHeightDp = configuration.screenHeightDp.dp
    val bottomPadding = contentPadding.calculateBottomPadding()
    val safeTopPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
        .asPaddingValues().calculateTopPadding()

    val textFieldHeight = phoneFieldHeight + phoneFieldPadding * 2
    val spacing = 8.dp
    val baseAvailableHeight =
        screenHeightDp * 0.70f - safeTopPadding - bottomPadding - textFieldHeight
    val buttonHeight = (baseAvailableHeight - (spacing * 3)).coerceAtLeast(0.dp) / 4

    val digitTextSize = with(density) { (buttonHeight.toPx() * 0.48f).coerceAtLeast(16f).toSp() }
    val letterTextSize = with(density) { (buttonHeight.toPx() * 0.185f).coerceAtLeast(8f).toSp() }
    val iconSize = with(density) { (buttonHeight.toPx() * 0.20f).coerceAtLeast(12f).toSp() }

    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
    val letters = listOf("ↈ", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "+", "")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(12) { index ->
                val key = keys[index]
                val letter = letters[index]
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                var longPressTriggered by remember { mutableStateOf(false) }

                LaunchedEffect(isPressed) {
                    if (isPressed) {
                        delay(480L)
                        if (isPressed) {
                            longPressTriggered = true
                            when (key) {
                                "1" -> viewModel.startVoicemailCall()
                                in "2".."9" -> {
                                    val entered = phoneNumber.trim()
                                    if (entered.isNotBlank()) {
                                        prefs.saveQuickDial(key.toInt(), entered)
                                        Toast.makeText(
                                            context,
                                            "Saved to Quick Dial $key",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        if (prefs.hasQuickDial(key.toInt())) {
                                            viewModel.startCall(prefs.getQuickDialNumber(key.toInt())!!)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "No Quickdial set yet",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                                "0" -> onNumberClick("+")
                            }
                        }
                    } else longPressTriggered = false
                }

                Box(
                    modifier = Modifier
                        .height(buttonHeight)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceBright)
                        .clickable(
                            interactionSource = interactionSource,
                            onClick = { if (!longPressTriggered) onNumberClick(key) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = key,
                                fontSize = digitTextSize,
                                fontFamily = QuicksandTitleVariable,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                        }
                        Box(modifier = Modifier.weight(3f), contentAlignment = Alignment.Center) {
                            if (letter.isNotBlank()) {
                                val currentSize =
                                    if (key == "1" || key == "0") iconSize else letterTextSize
                                val enlargedSize = (currentSize.value + 2).sp
                                Text(
                                    text = letter,
                                    fontSize = enlargedSize,
                                    style = LocalTextStyle.current.copy(lineHeight = enlargedSize),
                                    fontFamily = if (key == "1") FontFamily(Font(R.font.voicemailfont)) else QuicksandTitleVariable,
                                    fontWeight = if (key == "1") FontWeight.Bold else FontWeight.ExtraLight,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .width(48.dp)
                .height(buttonHeight * 4 + spacing * 3)
                .heightIn(max = 520.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilledTonalIconButton(
                onClick = onCallClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50.dp))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = "Call",
                    modifier = Modifier.size(27.dp)
                )
            }

            val backInteraction = remember { MutableInteractionSource() }
            FilledTonalIconButton(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50.dp)),
                onClick = { onDeleteClick(); vibrateFeedback(context) },
                interactionSource = backInteraction
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "Delete",
                    modifier = Modifier.size(21.dp)
                )
            }
            LaunchedEffect(backInteraction) {
                var pressStart: Long? = null
                backInteraction.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> {
                            pressStart = System.currentTimeMillis()
                            launch {
                                delay(420L)
                                if (pressStart != null) {
                                    vibrateFeedback(context, 45L, 110)
                                    onClearAll()
                                    pressStart = null
                                }
                            }
                        }
                        is PressInteraction.Release -> {
                            if (pressStart != null) vibrateFeedback(context, 5L, 5)
                            pressStart = null
                        }
                        is PressInteraction.Cancel -> pressStart = null
                    }
                }
            }
        }
    }
}

@SuppressLint("ObsoleteSdkInt")
fun safePlaceCall(context: Context, phoneNumber: String) {
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    val uri = "tel:${phoneNumber.replace(" ", "")}".toUri()
    val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) (context.getSystemService(Context.ROLE_SERVICE) as RoleManager).isRoleHeld(RoleManager.ROLE_DIALER) else telecomManager.defaultDialerPackage == context.packageName
    if (isDefault) { try { telecomManager.placeCall(uri, Bundle().apply { putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false) }) } catch (_: SecurityException) { Toast.makeText(context, context.getString(R.string.permission_denied), Toast.LENGTH_SHORT).show(); fallbackCallIntent(context, uri) } }
    else fallbackCallIntent(context, uri)
}

private fun fallbackCallIntent(context: Context, uri: Uri) { try { context.startActivity(Intent(Intent.ACTION_CALL, uri)) } catch (_: Exception) { Toast.makeText(context, context.getString(R.string.call_failed), Toast.LENGTH_SHORT).show() } }

private fun vibrateFeedback(context: Context, durationMs: Long = 35L, amplitude: Int = 90, pattern: LongArray? = null) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator ?: return
    if (pattern != null) vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1)) else vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, amplitude))
}
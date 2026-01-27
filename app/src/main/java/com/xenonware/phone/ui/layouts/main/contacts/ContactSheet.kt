@file:Suppress("DEPRECATION")

package com.xenonware.phone.ui.layouts.main.contacts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.phone.R
import com.xenonware.phone.data.Contact
import com.xenonware.phone.ui.res.MenuItem
import com.xenonware.phone.ui.res.XenonDropDown
import com.xenonware.phone.ui.theme.LocalIsDarkTheme
import com.xenonware.phone.util.PhoneNumberFormatter
import com.xenonware.phone.viewmodel.PhoneViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

@OptIn(ExperimentalFoundationApi::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun ContactSheet(
    initialContent: String = "",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    toolbarHeight: Dp,
    saveTrigger: Boolean,
    onSaveTriggerConsumed: () -> Unit,
    isBlackThemeActive: Boolean = false,
    isCoverModeActive: Boolean = false,
    viewModel: PhoneViewModel,
    modifier: Modifier,
    isContactSheetOpen: Boolean,
    // ── New parameters added below (no existing ones removed) ───────────────────────
    contact: Contact? = null,
    isViewMode: Boolean = true,
    onCallClick: (String) -> Unit = {},
    onMessageClick: (String) -> Unit = {},
) {
    val hazeState = remember { HazeState() }
    val isDarkTheme = LocalIsDarkTheme.current

    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }

    val systemUiController = rememberSystemUiController()
    val originalStatusBarColor = Color.Transparent

//    LaunchedEffect(saveTrigger) {
//        if (saveTrigger) {
//            onSave ( )
//            onSaveTriggerConsumed()
//        }
//    }

    DisposableEffect(systemUiController, isDarkTheme) {
        systemUiController.setSystemBarsColor(color = Color.Transparent, darkIcons = !isDarkTheme)
        onDispose {
            systemUiController.setStatusBarColor(color = originalStatusBarColor)
        }
    }
    val hazeThinColor = colorScheme.surfaceDim

    val safeDrawingPadding = if (WindowInsets.ime.asPaddingValues()
            .calculateBottomPadding() > WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
            .asPaddingValues().calculateBottomPadding()
    ) {
        WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    } else {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues()
            .calculateBottomPadding()
    }

    val bottomPadding = safeDrawingPadding + toolbarHeight + 16.dp
    val backgroundColor =
        if (isCoverModeActive || isBlackThemeActive) Color.Black else colorScheme.surfaceContainer

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        val topPadding = 68.dp
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = 4.dp)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .verticalScroll(scrollState)
                .hazeSource(state = hazeState)
        ) {
            Spacer(modifier = Modifier.height(topPadding))

            // ── Replaced only the TODO line with contact display logic ───────────────────────
            if (contact != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    ContactAvatar(
                        contact = contact,
                        modifier = Modifier.size(96.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Name
                    Text(
                        text = contact.name,
                        fontSize = 26.sp,
                        fontFamily = QuicksandTitleVariable,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Phone number
                    if (contact.phone.isNotBlank()) {
                        Text(
                            text = PhoneNumberFormatter.formatForDisplay(contact.phone, LocalContext.current),
                            fontSize = 18.sp,
                            color = colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Action buttons (only in view mode)
                    if (isViewMode && contact.phone.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(0.8f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(
                                onClick = { onMessageClick(contact.phone) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Rounded.Message, null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Message")
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            Button(
                                onClick = { onCallClick(contact.phone) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Call, null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Call")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(bottomPadding))
        }

        //Toolbar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                )
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(100f))
                .background(colorScheme.surfaceDim)
                .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin(hazeThinColor)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.padding(4.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }

            val titleTextStyle = MaterialTheme.typography.titleLarge.merge(
                TextStyle(
                    fontFamily = QuicksandTitleVariable,
                    textAlign = TextAlign.Center,
                    color = colorScheme.onSurface
                )
            )

            Text(
                text = stringResource(id = R.string.contact_card),
                modifier = Modifier.weight(1f),
                style = titleTextStyle
            )

            Box {
                IconButton(onClick = { showMenu = !showMenu }, modifier = Modifier.padding(4.dp)) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More Options")
                }
                XenonDropDown(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    items = listOfNotNull(
                        MenuItem(
                            text = "Edit",
                            onClick = { /*TODO*/ },
                            dismissOnClick = true,
                            icon = {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = "Edit",
                                    tint = colorScheme.onSurfaceVariant
                                )
                            },
                        )
                    ),
                    hazeState = hazeState
                )
            }
        }
    }
}
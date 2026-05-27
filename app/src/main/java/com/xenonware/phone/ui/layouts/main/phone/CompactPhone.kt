@file:Suppress("AssignedValueIsNeverRead")

package com.xenonware.phone.ui.layouts.main.phone

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.identity.Identity
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.res.FloatingToolbarContent
import com.xenon.mylibrary.res.GoogleProfilBorder
import com.xenon.mylibrary.res.GoogleProfilePicture
import com.xenon.mylibrary.res.SpannedModeFAB
import com.xenon.mylibrary.theme.DeviceConfigProvider
import com.xenon.mylibrary.theme.LocalDeviceConfig
import com.xenon.mylibrary.values.LargePadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.NoSpacing
import com.xenon.mylibrary.values.SmallPadding
import com.xenonware.phone.R
import com.xenonware.phone.data.SharedPreferenceManager
import com.xenonware.phone.presentation.sign_in.GoogleAuthUiClient
import com.xenonware.phone.presentation.sign_in.SignInViewModel
import com.xenonware.phone.ui.layouts.call_history.CallHistoryScreen
import com.xenonware.phone.ui.layouts.main.contacts.ContactsScreen
import com.xenonware.phone.ui.layouts.main.dialer_screen.DialerScreen
import com.xenonware.phone.ui.layouts.main.dialer_screen.safePlaceCall
import com.xenonware.phone.ui.res.ContactSheet
import com.xenonware.phone.ui.theme.LocalIsDarkTheme
import com.xenonware.phone.viewmodel.CallHistoryViewModel
import com.xenonware.phone.viewmodel.LayoutType
import com.xenonware.phone.viewmodel.PhoneViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalHazeMaterialsApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun CompactPhone(
    viewModel: PhoneViewModel,
    isLandscape: Boolean,
    layoutType: LayoutType,
    onOpenSettings: () -> Unit,
    appSize: IntSize,
) {
    DeviceConfigProvider(appSize = appSize) {
        val deviceConfig = LocalDeviceConfig.current
        var backProgress by remember { mutableFloatStateOf(0f) }
        val context = LocalContext.current
        val sharedPreferenceManager = remember { SharedPreferenceManager(context) }

        val configuration = LocalConfiguration.current
        val isCompact = LocalDeviceConfig.current.isCommunicator || LocalDeviceConfig.current.isMindOne
        val appHeight = configuration.screenHeightDp.dp

        val isAppBarExpandable = when (layoutType) {
            LayoutType.COVER -> false
            LayoutType.SMALL -> false
            LayoutType.COMPACT -> !isLandscape && !isCompact && appHeight >= 460.dp
            LayoutType.MEDIUM -> true
            LayoutType.EXPANDED -> true
        }

        val hazeState = rememberHazeState()
        val lazyListState = rememberLazyListState()

        val showContactCard by viewModel.showContactCard.collectAsStateWithLifecycle()
        val selectedContact by viewModel.selectedContact.collectAsStateWithLifecycle()
        var isSearchActive by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var showResizeValue by remember { mutableStateOf(false) }
        var resizeTimerKey by remember { mutableIntStateOf(0) }

        val isDarkTheme = LocalIsDarkTheme.current

        val isBlackedOut by produceState(
            initialValue = sharedPreferenceManager.blackedOutModeEnabled && isDarkTheme
        ) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "blacked_out_mode_enabled") {
                    value = sharedPreferenceManager.blackedOutModeEnabled
                }
            }
            sharedPreferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(
                listener
            )
            awaitDispose {
                sharedPreferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(
                    listener
                )
            }
        }

        LaunchedEffect(resizeTimerKey) {
            if (showResizeValue) {
                delay(2000)
                showResizeValue = false
            }
        }

        val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
        val coroutineScope = rememberCoroutineScope()

        val currentScreen by remember {
            derivedStateOf {
                when (pagerState.currentPage) {
                    0 -> PhoneScreen.History
                    1 -> PhoneScreen.Dialer
                    2 -> PhoneScreen.Contacts
                    else -> PhoneScreen.Dialer
                }
            }
        }

        LaunchedEffect(isSearchActive) {
            if (isSearchActive && pagerState.currentPage != 2) {
                pagerState.animateScrollToPage(
                    page = 2,
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                )
            }
        }

        val areNavButtonsEnabled = true

        Scaffold(
            bottomBar = {
                val bottomPaddingNavigationBar =
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val imePaddingValues = WindowInsets.ime.asPaddingValues()
                val imeHeight = imePaddingValues.calculateBottomPadding()
                val targetBottomPadding =
                    remember(imeHeight, bottomPaddingNavigationBar, imePaddingValues) {
                        val calculatedPadding = if (imeHeight > bottomPaddingNavigationBar) {
                            imeHeight + LargePadding
                        } else {
                            max(
                                bottomPaddingNavigationBar, imePaddingValues.calculateTopPadding()
                            ) + LargePadding
                        }
                        max(calculatedPadding, 0.dp)
                    }
                val animatedBottomPadding by animateDpAsState(
                    targetValue = targetBottomPadding, animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow
                    ), label = "bottomPaddingAnimation"
                )
                FloatingToolbarContent(
                    hazeState = hazeState,
                    currentSearchQuery = searchQuery,
                    onSearchQueryChanged = { newValue ->
                        searchQuery = newValue
                        viewModel.setSearchQuery(newValue)
                    },
                    lazyListState = lazyListState,
                    allowToolbarScrollBehavior = !isAppBarExpandable && !showContactCard,
                    selectedNoteIds = emptyList(),
                    onClearSelection = {},
                    isAddModeActive = false,
                    isSearchActive = isSearchActive,
                    onIsSearchActiveChange = { newActive ->
                        isSearchActive = newActive
                        if (!newActive) {
                            searchQuery = ""
                            viewModel.setSearchQuery("")
                        }
                    },
                    defaultContent = { iconsAlphaDuration, showActionIconsExceptSearch ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val iconAlphaTarget = if (isSearchActive) 0f else 1f

                            val navIconAlpha by animateFloatAsState(
                                targetValue = iconAlphaTarget, animationSpec = tween(
                                    durationMillis = iconsAlphaDuration,
                                    delayMillis = if (isSearchActive) 0 else 0
                                ), label = "NavIconAlpha"
                            )
                            val navBoxAlpha by animateFloatAsState(
                                targetValue = iconAlphaTarget, animationSpec = tween(
                                    durationMillis = iconsAlphaDuration,
                                    delayMillis = if (isSearchActive) 100 else 0
                                ), label = "NavBoxAlpha"
                            )

                            Box(
                                modifier = Modifier
                                    .alpha(navBoxAlpha)
                                    .clip(CircleShape)
                                    .background(colorScheme.surfaceBright)
                            ) {
                                val itemWidth = 48.dp
                                val navIcons = listOf(
                                    Icons.Rounded.History to "History",
                                    Icons.Rounded.Dialpad to "Dialer",
                                    Icons.Rounded.Person to "Contacts"
                                )

                                val indicatorPosition by remember {
                                    derivedStateOf {
                                        pagerState.currentPage + pagerState.currentPageOffsetFraction
                                    }
                                }

                                // Indicator
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            androidx.compose.ui.unit.IntOffset(
                                                x = (indicatorPosition * itemWidth.toPx()).toInt(),
                                                y = 0
                                            )
                                        }
                                        .size(itemWidth)
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(colorScheme.tertiary)
                                )

                                // Icons Row
                                Row(modifier = Modifier.alpha(navIconAlpha)) {
                                    navIcons.forEachIndexed { index, (icon, desc) ->
                                        Box(
                                            modifier = Modifier
                                                .size(itemWidth)
                                                .padding(4.dp)
                                                .clip(CircleShape)
                                                .clickable(
                                                    enabled = areNavButtonsEnabled,
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            pagerState.animateScrollToPage(index)
                                                        }
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Calculate color based on indicator proximity for a smooth "reveal" effect
                                            val distance = abs(indicatorPosition - index)
                                            val colorFraction = (1f - (distance * 2f)).coerceIn(0f, 1f)
                                            val iconColor = androidx.compose.ui.graphics.lerp(
                                                colorScheme.onSurface,
                                                colorScheme.onTertiary,
                                                colorFraction
                                            )

                                            Icon(
                                                imageVector = icon,
                                                contentDescription = desc,
                                                tint = iconColor
                                            )
                                        }
                                    }
                                }
                            }

                            val settingsIconAlpha by animateFloatAsState(
                                targetValue = iconAlphaTarget, animationSpec = tween(
                                    durationMillis = iconsAlphaDuration,
                                    delayMillis = if (isSearchActive) 200 else 0
                                ), label = "SettingsIconAlpha"
                            )

                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.alpha(settingsIconAlpha),
                                enabled = !isSearchActive && showActionIconsExceptSearch
                            ) {
                                Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                            }
                        }
                    },
                    onAddModeToggle = { },
                    isSelectedColor = colorScheme.primary,
                    selectionContentOverride = { },
                    addModeContentOverride = { },
                    contentOverride = null,
                    fabOverride = null,
                    isFabEnabled = false,
                    isSpannedMode = deviceConfig.isSpannedMode,
                    fabOnLeftInSpannedMode = deviceConfig.fabOnLeft,
                    spannedModeHingeGap = deviceConfig.hingeGapDp,
                    spannedModeFab = {
                        SpannedModeFAB(
                            hazeState = hazeState,
                            onClick = deviceConfig.toggleFabSide,
                            modifier = Modifier.padding(bottom = animatedBottomPadding),
                            isSheetOpen = showContactCard
                        )
                    })
            }) { scaffoldPadding ->
            if (showContactCard) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {})
            }
            val context = LocalContext.current
            val googleAuthUiClient = remember {
                GoogleAuthUiClient(
                    context = context.applicationContext,
                    oneTapClient = Identity.getSignInClient(context.applicationContext)
                )
            }
        val signInViewModel: SignInViewModel = viewModel()
        val callHistoryViewModel: CallHistoryViewModel = viewModel()
        val state by signInViewModel.state.collectAsStateWithLifecycle()
            val userData = googleAuthUiClient.getSignedInUser()

            ActivityScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState),
                titleText = when (currentScreen) {
                    PhoneScreen.History -> stringResource(R.string.call_history)
                    PhoneScreen.Dialer -> stringResource(R.string.phone)
                    PhoneScreen.Contacts -> stringResource(R.string.contacts)
                },
                expandable = isAppBarExpandable,
                navigationIconStartPadding = if (state.isSignInSuccessful) SmallPadding else 0.dp,
                navigationIconPadding = if (state.isSignInSuccessful) SmallPadding else 0.dp,
                navigationIconSpacing = if (state.isSignInSuccessful) NoSpacing else 0.dp,
                hasNavigationIconExtraContent = state.isSignInSuccessful,
                navigationIconExtraContent = {
                    if (state.isSignInSuccessful) {
                        Box(contentAlignment = Alignment.Center) {
                            GoogleProfilBorder(
                                isSignedIn = true,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.5.dp
                            )
                            GoogleProfilePicture(
                                noAccIcon = painterResource(id = R.drawable.default_icon),
                                profilePictureUrl = userData?.profilePictureUrl,
                                contentDescription = stringResource(R.string.profile_picture),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                },
                navigationIcon = {},
                actions = {},
                content = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val filteredContacts by viewModel.filteredContacts.collectAsStateWithLifecycle()

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                        ) { page ->
                            when (page) {
                                0 -> CallHistoryScreen(
                                    viewModel = callHistoryViewModel,
                                    searchQuery = searchQuery,
                                    contentPadding = PaddingValues(scaffoldPadding.calculateBottomPadding() + MediumPadding)
                                )

                                1 -> DialerScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(scaffoldPadding.calculateBottomPadding() + MediumPadding),
                                    isAppBarExpandable = isAppBarExpandable
                                )

                                2 -> ContactsScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    contactsToShow = filteredContacts.toList(),
                                    searchQuery = searchQuery,
                                    contentPadding = PaddingValues(scaffoldPadding.calculateBottomPadding() + MediumPadding),
                                    onOpenDetail = { contact ->
                                        viewModel.showContactCard(contact)
                                    })
                            }
                        }

                        LaunchedEffect(currentScreen, pagerState.isScrollInProgress) {
                            if (currentScreen != PhoneScreen.Contacts && !pagerState.isScrollInProgress) {
                                isSearchActive = false
                                searchQuery = ""
                                viewModel.setSearchQuery("")
                            }
                        }
                    }
                })

            PredictiveBackHandler(enabled = showContactCard) { progressFlow ->
                try {
                    progressFlow.collect { event ->
                        backProgress = event.progress
                    }
                    viewModel.hideContactCard()
                    isSearchActive = false
                    viewModel.setSearchQuery("")
                } catch (_: CancellationException) {
                    backProgress = 0f
                }
            }

            LaunchedEffect(showContactCard) {
                if (!showContactCard) {
                    delay(200)
                    backProgress = 0f
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val scrimAlpha = 0.6f * (1f - backProgress / 2)
                AnimatedVisibility(
                    visible = showContactCard,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colorScheme.scrim.copy(alpha = scrimAlpha))
                            .combinedClickable(
                                onClick = { viewModel.hideContactCard() },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() })
                    )
                }

                AnimatedVisibility(
                    visible = showContactCard,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationY = backProgress * (size.height * 0.2f)
                                val exponentialProgress =
                                    kotlin.math.sqrt(backProgress.toDouble()).toFloat()
                                val radius = (exponentialProgress * 40).dp
                                shape = RoundedCornerShape((radius))
                                clip = true
                            },
                        color = if (isBlackedOut) Color.Black else colorScheme.surfaceContainer,
                    ) {

                        selectedContact?.let { contact ->
                            ContactSheet(
                                onDismiss = {
                                    viewModel.hideContactCard()
                                    isSearchActive = false
                                    viewModel.setSearchQuery("")
                                },
                                toolbarHeight = 72.dp,
                                isBlackThemeActive = false,
                                isCoverModeActive = false,
                                contact = contact,
                                isViewMode = true,
                                onCallClick = { number -> safePlaceCall(context, number) },
                                onMessageClick = { number ->
                                    val cleanNumber = number.replace(" ", "")
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = "smsto:$cleanNumber".toUri()
                                    }
                                    context.startActivity(intent)
                                },
                                backProgress = backProgress
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class PhoneScreen {
    object History : PhoneScreen()
    object Dialer : PhoneScreen()
    object Contacts : PhoneScreen()
}

@file:Suppress("AssignedValueIsNeverRead")

package com.xenonware.phone.ui.layouts.main.phone

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
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
import com.xenonware.phone.presentation.sign_in.GoogleAuthUiClient
import com.xenonware.phone.presentation.sign_in.SignInViewModel
import com.xenonware.phone.ui.layouts.main.call_history.CallHistoryScreen
import com.xenonware.phone.ui.layouts.main.contacts.ContactsScreen
import com.xenonware.phone.ui.layouts.main.dialer_screen.DialerScreen
import com.xenonware.phone.viewmodel.LayoutType
import com.xenonware.phone.viewmodel.PhoneViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalHazeMaterialsApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun CompactPhone(
    viewModel: PhoneViewModel,
    signInViewModel: SignInViewModel,
    isLandscape: Boolean,
    layoutType: LayoutType,
    onOpenSettings: () -> Unit,
    appSize: IntSize,
) {
    val density = LocalDensity.current

    val configuration = LocalConfiguration.current
    val appHeight = configuration.screenHeightDp.dp
    val isAppBarExpandable = when (layoutType) {
        LayoutType.COVER -> false
        LayoutType.SMALL -> false
        LayoutType.COMPACT -> !isLandscape && appHeight >= 460.dp
        LayoutType.MEDIUM -> true
        LayoutType.EXPANDED -> true
    }

    DeviceConfigProvider(appSize = appSize) {
        val deviceConfig = LocalDeviceConfig.current
        val hazeState = rememberHazeState()
        val snackbarHostState = remember { SnackbarHostState() }
        val lazyListState = rememberLazyListState()

        var currentScreen by remember { mutableStateOf<PhoneScreen>(PhoneScreen.Dialer) }
        var isSearchActive by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var showResizeValue by remember { mutableStateOf(false) }
        var resizeTimerKey by remember { mutableIntStateOf(0) }

        val context = LocalContext.current
        val googleAuthUiClient = remember {
            GoogleAuthUiClient(
                context = context.applicationContext,
                oneTapClient = Identity.getSignInClient(context.applicationContext)
            )
        }
        val userData = googleAuthUiClient.getSignedInUser()

        LaunchedEffect(resizeTimerKey) {
            if (showResizeValue) {
                delay(2000)
                showResizeValue = false
            }
        }

        val onResizeClick: () -> Unit = {
            showResizeValue = true
            resizeTimerKey++
        }

        // Pager: 0 = Dialer, 1 = Contacts
        val pagerState = rememberPagerState(pageCount = { 2 })

        // Sync pager with currentScreen (only when not in CallHistory)
        LaunchedEffect(pagerState.currentPage) {
            if (currentScreen !is PhoneScreen.CallHistory) {
                currentScreen = when (pagerState.currentPage) {
                    0 -> PhoneScreen.Dialer
                    1 -> PhoneScreen.Contacts
                    else -> PhoneScreen.Dialer
                }
            }
        }

        // Sync pager when switching via buttons (only Dialer/Contacts)
        LaunchedEffect(currentScreen) {
            when (currentScreen) {
                PhoneScreen.Dialer -> {
                    if (pagerState.currentPage != 0) pagerState.animateScrollToPage(0)
                }

                PhoneScreen.Contacts -> {
                    if (pagerState.currentPage != 1) pagerState.animateScrollToPage(1)
                }

                else -> Unit
            }
        }

        // Handle back gesture when in Call History
        BackHandler(enabled = currentScreen is PhoneScreen.CallHistory) {
            currentScreen = PhoneScreen.Dialer
        }

        val isCallHistoryOpen = currentScreen is PhoneScreen.CallHistory
        val areNavButtonsEnabled = !isSearchActive && !isCallHistoryOpen

        Scaffold(snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        }, bottomBar = {
            val imePadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
            val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val targetPadding = max(imePadding, navPadding) + LargePadding

            val animatedPadding by animateDpAsState(
                targetValue = targetPadding, animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow
                )
            )

            FloatingToolbarContent(
                hazeState = hazeState,
                currentSearchQuery = searchQuery,
                onSearchQueryChanged = { searchQuery = it },
                lazyListState = lazyListState,
                allowToolbarScrollBehavior = true,
                selectedNoteIds = emptyList(),
                onClearSelection = {},
                isAddModeActive = false,
                isSearchActive = isSearchActive,
                onIsSearchActiveChange = { isSearchActive = it },
                defaultContent = { iconsAlphaDuration, showActionIconsExceptSearch ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val iconAlphaTarget = if (isSearchActive || isCallHistoryOpen) 0.38f else 1f

                        val navIconAlpha by animateFloatAsState(
                            targetValue = iconAlphaTarget,
                            animationSpec = tween(durationMillis = iconsAlphaDuration),
                            label = "NavIconAlpha"
                        )

                        Box(
                            modifier = Modifier
                                .graphicsLayer(alpha = navIconAlpha)
                                .clip(CircleShape)
                                .background(colorScheme.surfaceBright)
                        ) {
                            Row {
                                FilledTonalIconButton(
                                    onClick = { currentScreen = PhoneScreen.Dialer },
                                    enabled = areNavButtonsEnabled,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = if (currentScreen == PhoneScreen.Dialer && !isCallHistoryOpen) colorScheme.tertiary
                                        else colorScheme.surfaceBright,
                                        contentColor = if (currentScreen == PhoneScreen.Dialer && !isCallHistoryOpen) colorScheme.onTertiary
                                        else colorScheme.onSurface,
                                        disabledContainerColor = colorScheme.onSurface.copy(
                                            alpha = 0.6f
                                        ),
                                        disabledContentColor = colorScheme.surfaceBright.copy(
                                            alpha = 0.38f
                                        )
                                    )
                                ) {
                                    Icon(Icons.Rounded.Dialpad, contentDescription = "Dialer")
                                }

                                FilledTonalIconButton(
                                    onClick = { currentScreen = PhoneScreen.Contacts },
                                    enabled = areNavButtonsEnabled,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = if (currentScreen == PhoneScreen.Contacts && !isCallHistoryOpen) colorScheme.tertiary
                                        else colorScheme.surfaceBright,
                                        contentColor = if (currentScreen == PhoneScreen.Contacts && !isCallHistoryOpen) colorScheme.onTertiary
                                        else colorScheme.onSurface,
                                        disabledContainerColor = colorScheme.surfaceBright.copy(
                                            alpha = 0.6f
                                        ),
                                        disabledContentColor = colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                ) {
                                    Icon(Icons.Rounded.Person, contentDescription = "Contacts")
                                }
                            }
                        }

                        val settingsIconAlpha by animateFloatAsState(
                            targetValue = if (isSearchActive) 0f else 1f, animationSpec = tween(
                                durationMillis = iconsAlphaDuration,
                                delayMillis = if (isSearchActive) 100 else 0
                            ), label = "SettingsIconAlpha"
                        )

                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.graphicsLayer(alpha = settingsIconAlpha),
                            enabled = !isSearchActive && showActionIconsExceptSearch
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                        }
                    }
                },
                onAddModeToggle = { },
                isSelectedColor = MaterialTheme.colorScheme.primary,
                selectionContentOverride = { },
                addModeContentOverride = { },
                contentOverride = null,
                fabOverride = null,
                isSpannedMode = deviceConfig.isSpannedMode,
                fabOnLeftInSpannedMode = deviceConfig.fabOnLeft,
                spannedModeHingeGap = deviceConfig.hingeGapDp,
                spannedModeFab = {
                    SpannedModeFAB(
                        hazeState = hazeState,
                        onClick = deviceConfig.toggleFabSide,
                        modifier = Modifier.padding(bottom = animatedPadding),
                        isSheetOpen = false
                    )
                })
        }) { scaffoldPadding ->
            val context = LocalContext.current
            val googleAuthUiClient = remember {
                GoogleAuthUiClient(
                    context = context.applicationContext,
                    oneTapClient = Identity.getSignInClient(context.applicationContext)
                )
            }
            val signInViewModel: SignInViewModel = viewModel()
            val state by signInViewModel.state.collectAsStateWithLifecycle()
            val userData = googleAuthUiClient.getSignedInUser()
            val navIconPadding = when (currentScreen) {
                PhoneScreen.Dialer -> 0.dp
                PhoneScreen.Contacts -> 0.dp
                PhoneScreen.CallHistory -> MediumPadding
            }

            ActivityScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState),
                titleText = when (currentScreen) {
                    PhoneScreen.Dialer -> stringResource(R.string.phone)
                    PhoneScreen.Contacts -> stringResource(R.string.contacts)
                    PhoneScreen.CallHistory -> stringResource(R.string.call_history)
                },
                expandable = isAppBarExpandable,
                navigationIconStartPadding = if (state.isSignInSuccessful) SmallPadding else navIconPadding,
                navigationIconPadding = if (state.isSignInSuccessful) SmallPadding else navIconPadding,
                navigationIconSpacing = if (state.isSignInSuccessful) NoSpacing else 0.dp,
                navigationIcon = {
                    when (currentScreen) {
                        PhoneScreen.CallHistory -> {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back_description),
                                modifier = Modifier.size(24.dp)
                            )
                        } else -> {}
                    }
                },
                onNavigationIconClick = {
                    if (currentScreen == PhoneScreen.CallHistory) {
                        currentScreen = PhoneScreen.Dialer
                    }
                },
                hasNavigationIconExtraContent = state.isSignInSuccessful,
                navigationIconExtraContent = {
                    if (state.isSignInSuccessful) {
                        Box(contentAlignment = Alignment.Center) {
                            @Suppress("KotlinConstantConditions") GoogleProfilBorder(
                                isSignedIn = state.isSignInSuccessful,
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
                actions = {},
                content = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                            userScrollEnabled = !isCallHistoryOpen
                        ) { page ->
                            when (page) {
                                0 -> DialerScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    onShowCallLog = { currentScreen = PhoneScreen.CallHistory })

                                1 -> ContactsScreen(modifier = Modifier.fillMaxSize())
                            }
                        }

                        if (isCallHistoryOpen) {
                            CallHistoryScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(colorScheme.surfaceContainer),
                                onBack = { currentScreen = PhoneScreen.Dialer })
                        }
                    }
                })
        }
    }
}

sealed class PhoneScreen {
    object Dialer : PhoneScreen()
    object CallHistory : PhoneScreen()
    object Contacts : PhoneScreen()
}
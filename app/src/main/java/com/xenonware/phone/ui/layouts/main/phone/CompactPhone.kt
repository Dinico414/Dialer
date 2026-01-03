@file:Suppress("AssignedValueIsNeverRead")

package com.xenonware.phone.ui.layouts.main.phone

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun CompactPhone(
    viewModel: PhoneViewModel,
    signInViewModel: SignInViewModel,
    isLandscape: Boolean,
    layoutType: LayoutType,
    onOpenSettings: () -> Unit,
    appSize: IntSize
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
                defaultContent = { _, _ ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceBright)
                        ) {
                            Row {
                                FilledTonalIconButton(
                                    onClick = { currentScreen = PhoneScreen.Dialer },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (currentScreen == PhoneScreen.Dialer) colorScheme.tertiary else colorScheme.surfaceBright,
                                        contentColor = if (currentScreen == PhoneScreen.Dialer) colorScheme.onTertiary else colorScheme.onSurface
                                    )
                                ) {
                                    Icon(Icons.Rounded.Dialpad, contentDescription = "Dialer")
                                }
                                FilledTonalIconButton(
                                    onClick = { currentScreen = PhoneScreen.Contacts},
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (currentScreen == PhoneScreen.Contacts) colorScheme.tertiary else colorScheme.surfaceBright,
                                        contentColor = if (currentScreen == PhoneScreen.Contacts) colorScheme.onTertiary else colorScheme.onSurface
                                    )
                                ) {
                                    Icon(
                                        Icons.Rounded.Person, contentDescription = "Contacts"
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onOpenSettings) {
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
            ActivityScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState),
                titleText = stringResource(R.string.app_name),
                expandable = isAppBarExpandable,
                navigationIconStartPadding = if (state.isSignInSuccessful) SmallPadding else 0.dp,
                navigationIconPadding = if (state.isSignInSuccessful) {
                    SmallPadding
                } else {
                    0.dp
                },
                navigationIconSpacing = if (state.isSignInSuccessful) NoSpacing else 0.dp,

                navigationIcon = {},
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding()
                    ) {
                        when (currentScreen) {
                            PhoneScreen.Dialer -> DialerScreen(
                                modifier = Modifier.fillMaxSize(),
                                onShowCallLog = { currentScreen = PhoneScreen.CallHistory })

                            PhoneScreen.CallHistory -> CallHistoryScreen(modifier = Modifier.fillMaxSize())
                            PhoneScreen.Contacts -> ContactsScreen(modifier = Modifier.fillMaxSize())
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
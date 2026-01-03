package com.xenonware.phone.ui.layouts.main.phone

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.identity.Identity
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.res.GoogleProfilBorder
import com.xenon.mylibrary.res.GoogleProfilBorderNoGoogle
import com.xenon.mylibrary.res.GoogleProfilePicture
import com.xenon.mylibrary.theme.DeviceConfigProvider
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

@Composable
fun CoverPhone(
    viewModel: PhoneViewModel,
    signInViewModel: SignInViewModel,
    isLandscape: Boolean,
    layoutType: LayoutType,
    onOpenSettings: () -> Unit,
    appSize: IntSize
) {
    DeviceConfigProvider(appSize = appSize) {
        var currentScreen by remember { mutableStateOf<PhoneScreen>(PhoneScreen.Dialer) }

        val coverBackground = Color.Black

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
            modifier = Modifier.fillMaxSize(),
            titleText = stringResource(R.string.app_name),
            expandable = false,
            screenBackgroundColor = coverBackground,
            contentBackgroundColor = coverBackground,
            appBarNavigationIconContentColor = Color.White,
            contentCornerRadius = 0.dp,
            navigationIcon = {},
            hasNavigationIconExtraContent = state.isSignInSuccessful,
            navigationIconExtraContent = {
                if (state.isSignInSuccessful) {
                    Box(contentAlignment = Alignment.Center) {
                        @Suppress("KotlinConstantConditions")
                        GoogleProfilBorder(
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
                Column(modifier = Modifier.fillMaxSize()) {
                    // Optional: small indicator row at top if desired
                    // Or keep completely minimal

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(coverBackground)
                            .padding(horizontal = LargePadding)
                    ) {
                        when (currentScreen) {
                            PhoneScreen.Dialer -> DialerScreen(
                                modifier = Modifier.fillMaxSize(),
                                onShowCallLog = { currentScreen = PhoneScreen.CallHistory }
                            )
                            PhoneScreen.CallHistory -> CallHistoryScreen(modifier = Modifier.fillMaxSize())
                            PhoneScreen.Contacts -> ContactsScreen(modifier = Modifier.fillMaxSize())
                        }
                    }

                    // Bottom buttons in cover mode (minimal style)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(coverBackground)
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { currentScreen = PhoneScreen.Dialer }) {
                            Icon(Icons.Rounded.Dialpad, contentDescription = "Dialer", tint = Color.White)
                        }
                        IconButton(onClick = { currentScreen = PhoneScreen.CallHistory }) {
                            Icon(Icons.Rounded.History, contentDescription = "History", tint = Color.White)
                        }
                        IconButton(onClick = { currentScreen = PhoneScreen.Contacts }) {
                            Icon(Icons.Rounded.Contacts, contentDescription = "Contacts", tint = Color.White)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                }
            }
        )
    }
}

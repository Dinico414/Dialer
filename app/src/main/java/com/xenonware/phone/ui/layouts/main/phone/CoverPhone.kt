package com.xenonware.phone.ui.layouts.main.phone

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.xenon.mylibrary.res.GoogleProfilePicture
import com.xenon.mylibrary.theme.DeviceConfigProvider
import com.xenon.mylibrary.values.LargePadding
import com.xenonware.phone.CallHistoryActivity
import com.xenonware.phone.R
import com.xenonware.phone.presentation.sign_in.GoogleAuthUiClient
import com.xenonware.phone.presentation.sign_in.SignInViewModel
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
        val currentContext = LocalContext.current

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
                                onOpenHistory = {  val intent = Intent(currentContext,
                                    CallHistoryActivity::class.java)
                                    currentContext.startActivity(intent)
                                },
                                contentPadding = it
                            )
                            PhoneScreen.Contacts -> ContactsScreen(
                                modifier = Modifier.fillMaxSize(),
                                contactsToShow = viewModel.contacts.collectAsStateWithLifecycle().value,
                                searchQuery = viewModel.searchQuery.collectAsState().value,
                                contentPadding = it
                            )
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

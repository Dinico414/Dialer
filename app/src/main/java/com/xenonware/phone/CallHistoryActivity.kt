package com.xenonware.phone

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.theme.LocalDeviceConfig
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenon.mylibrary.values.NoCornerRadius
import com.xenonware.phone.data.SharedPreferenceManager
import com.xenonware.phone.ui.layouts.call_history.CallHistoryScreen
import com.xenonware.phone.ui.theme.ScreenEnvironment
import com.xenonware.phone.viewmodel.CallHistoryViewModel
import com.xenonware.phone.viewmodel.LayoutType
import java.util.Locale

class CallHistoryActivity : ComponentActivity() {

    private val viewModel: CallHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val sharedPreferenceManager = SharedPreferenceManager(applicationContext)

            val themePreference = sharedPreferenceManager.theme
            val blackedOutModeEnabled = sharedPreferenceManager.blackedOutModeEnabled

            val containerSize = LocalWindowInfo.current.containerSize
            val applyCoverTheme = sharedPreferenceManager.isCoverThemeApplied(containerSize)
            val isCover = applyCoverTheme

            ScreenEnvironment(
                themePreference = themePreference,
                coverTheme = applyCoverTheme,
                blackedOutModeEnabled = blackedOutModeEnabled
            ) { layoutType, isLandscape ->

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

                Surface(color = Color.Transparent, modifier = Modifier.fillMaxSize()) {
                    ActivityScreen(
                        titleText = stringResource(R.string.call_history),
                        expandable = isAppBarExpandable,
                        onNavigationIconClick = { finish() },
                        navigationIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back_description),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        screenBackgroundColor = if (isCover) Color.Black else MaterialTheme.colorScheme.surface,
                        contentBackgroundColor = if (isCover) Color.Black else MaterialTheme.colorScheme.surface,
                        appBarNavigationIconContentColor = if (isCover) Color.White else MaterialTheme.colorScheme.onSurface,
                        contentCornerRadius = if (isCover) NoCornerRadius else MediumCornerRadius,
                        actions = {},
                        content = { paddingValues ->
                            CallHistoryScreen(
                                viewModel = viewModel,
                                contentPadding = paddingValues,
                                isCoverMode = isCover
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadCallLogs(applicationContext)
    }

    override fun attachBaseContext(newBase: Context) {
        var context = newBase
        val prefs = SharedPreferenceManager(newBase)
        val savedTag = prefs.languageTag
        if (savedTag.isNotEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale = Locale.forLanguageTag(savedTag)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            context = newBase.createConfigurationContext(config)
        }
        super.attachBaseContext(ContextWrapper(context))
    }
}
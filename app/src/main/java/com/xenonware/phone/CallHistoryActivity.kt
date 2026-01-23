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
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import com.xenonware.phone.data.SharedPreferenceManager
import com.xenonware.phone.ui.layouts.CallHistoryLayout
import com.xenonware.phone.ui.theme.ScreenEnvironment
import com.xenonware.phone.viewmodel.CallHistoryViewModel
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

            ScreenEnvironment(
                themePreference = themePreference,
                coverTheme = applyCoverTheme,
                blackedOutModeEnabled = blackedOutModeEnabled
            ) { layoutType, isLandscape ->
                Surface(color = Color.Transparent, modifier = Modifier.fillMaxSize()) {
                    CallHistoryLayout(
                        onNavigateBack = { finish() },
                        isLandscape = isLandscape,
                        layoutType = layoutType,
                        viewModel = viewModel
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
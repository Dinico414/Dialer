package com.xenonware.phone.ui.layouts.callscreen

import android.content.res.Configuration
import android.telecom.Call
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import com.xenon.mylibrary.theme.DeviceConfigProvider
import com.xenonware.phone.viewmodel.LayoutType

@Composable
fun CoverCallScreen(
    call: Call?,
    isLandscape: Boolean,
    layoutType: LayoutType,
    appSize: IntSize
) {
    DeviceConfigProvider(appSize = appSize) {
        val isLandscape = when (layoutType) {
            LayoutType.COVER -> false
            LayoutType.SMALL -> false
            LayoutType.COMPACT -> LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            LayoutType.MEDIUM,
            LayoutType.EXPANDED -> true
        }
        CallScreenUi(
            call = call,
            isLandscape = isLandscape,
            forceCompactMode = true
        )
    }
}
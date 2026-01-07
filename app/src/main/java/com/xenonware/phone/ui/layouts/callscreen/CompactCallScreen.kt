package com.xenonware.phone.ui.layouts.callscreen

import android.telecom.Call
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import com.xenon.mylibrary.theme.DeviceConfigProvider
import com.xenonware.phone.viewmodel.LayoutType

@Composable
fun CompactCallScreen(
    call: Call?,
    isLandscape: Boolean,
    layoutType: LayoutType,
    appSize: IntSize
) {
    DeviceConfigProvider(appSize = appSize) {

        val isLandscape = when (layoutType) {
            LayoutType.COVER,
            LayoutType.SMALL,
            LayoutType.COMPACT -> false
            LayoutType.MEDIUM,
            LayoutType.EXPANDED -> true
        }
        CallScreenUi(
            call = call,
            isLandscape = isLandscape,
            forceCompactMode = isLandscape
        )
    }
}
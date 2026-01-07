package com.xenonware.phone.ui.layouts

import android.telecom.Call
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import com.xenonware.phone.ui.layouts.callscreen.CompactCallScreen
import com.xenonware.phone.ui.layouts.callscreen.CoverCallScreen
import com.xenonware.phone.viewmodel.LayoutType

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun CallScreenLayout(
    currentCall: Call?,
    isLandscape: Boolean,
    layoutType: LayoutType,
    appSize: IntSize,
) {
    when (layoutType) {
        LayoutType.COVER -> {
            CoverCallScreen(
                call = currentCall,
                isLandscape = isLandscape,
                layoutType = layoutType,
                appSize = appSize,
            )
        }
        LayoutType.SMALL,
        LayoutType.COMPACT,
        LayoutType.MEDIUM,
        LayoutType.EXPANDED -> {
            CompactCallScreen(
                call = currentCall,
                isLandscape = isLandscape,
                layoutType = layoutType,
                appSize = appSize,
            )
        }
    }
}
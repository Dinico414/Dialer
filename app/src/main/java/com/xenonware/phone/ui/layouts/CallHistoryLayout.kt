package com.xenonware.phone.ui.layouts

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xenonware.phone.ui.layouts.call_history.CompactHistoryScreen
import com.xenonware.phone.ui.layouts.call_history.CoverHistoryScreen
import com.xenonware.phone.viewmodel.CallHistoryViewModel
import com.xenonware.phone.viewmodel.LayoutType

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun CallHistoryLayout(
    onNavigateBack: () -> Unit,
    isLandscape: Boolean,
    layoutType: LayoutType,
    modifier: Modifier = Modifier,
    viewModel: CallHistoryViewModel
    ) {
    when (layoutType) {
        LayoutType.COVER -> {
            CoverHistoryScreen(
                onNavigateBack = onNavigateBack,
                isLandscape = isLandscape,
                layoutType = layoutType,
                modifier = modifier,
                viewModel = viewModel
            )
        }
        LayoutType.SMALL,
        LayoutType.COMPACT,
        LayoutType.MEDIUM,
        LayoutType.EXPANDED -> {
            CompactHistoryScreen(
                onNavigateBack = onNavigateBack,
                isLandscape = isLandscape,
                layoutType = layoutType,
                modifier = modifier,
                viewModel = viewModel
            )
        }
    }
}
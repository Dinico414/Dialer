package com.xenonware.phone.ui.layouts.call_history

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.LargePadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.NoSpacing
import com.xenon.mylibrary.values.SmallSpacing
import com.xenonware.phone.R
import com.xenonware.phone.viewmodel.CallHistoryViewModel
import com.xenonware.phone.viewmodel.LayoutType

@Composable
fun CoverHistoryScreen(
    onNavigateBack: () -> Unit,
    layoutType: LayoutType,
    isLandscape: Boolean,
    viewModel: CallHistoryViewModel,
    modifier: Modifier = Modifier
) {
    val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadCallLogs(context)
        }
    }

    LaunchedEffect(Unit) {
        if (hasPermission) {
            viewModel.loadCallLogs(context)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
        }
    }

    val configuration = LocalConfiguration.current
    val appHeight = configuration.screenHeightDp.dp

    val isAppBarExpandable = when (layoutType) {
        LayoutType.COVER -> false
        LayoutType.SMALL -> false
        LayoutType.COMPACT -> !isLandscape && appHeight >= 460.dp
        LayoutType.MEDIUM -> true
        LayoutType.EXPANDED -> true
    }

    ActivityScreen(
        titleText = stringResource(R.string.call_history),
        expandable = isAppBarExpandable,
        navigationIconStartPadding = MediumPadding,
        navigationIconPadding = MediumPadding,
        navigationIconSpacing = NoSpacing,
        navigationIcon = {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.navigate_back_description),
                modifier = Modifier.size(24.dp)
            )
        },
        onNavigationIconClick = onNavigateBack,
        hasNavigationIconExtraContent = false,
        actions = {},
        content = { _ ->
            // For COVER layout you can later adjust padding, font sizes, item spacing etc.
            // Right now using same content as compact (most common approach for cover screens)
            Column(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    callLogs.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (hasPermission) "No call history yet" else "Permission required\nto view call history",
                                fontSize = 18.sp,
                                lineHeight = 28.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        val groupedCalls = remember(callLogs) { groupCallLogsByDate(callLogs) }
                        val listState = rememberLazyListState()

                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(SmallSpacing),
                            modifier = Modifier
                                .padding(horizontal = 12.dp)  // possibly smaller horizontal padding on cover
                                .fillMaxSize(),
                            contentPadding = PaddingValues(
                                bottom = with(LocalDensity.current) {
                                    WindowInsets.navigationBars.asPaddingValues()
                                        .calculateBottomPadding() + 48.dp + LargePadding
                                })) {
                            groupedCalls.forEach { group ->
                                item(key = "header_${group.title}") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceContainer)
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = group.title,
                                            fontSize = 18.sp,           // ← smaller on cover?
                                            fontFamily = QuicksandTitleVariable,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                items(
                                    items = group.entries, key = { it.date }) { entry ->
                                    val index = group.entries.indexOf(entry)
                                    CallHistoryItemCard(
                                        entry = entry,
                                        isFirstInGroup = index == 0,
                                        isLastInGroup = index == group.entries.lastIndex,
                                        isSingle = group.entries.size == 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        })
}
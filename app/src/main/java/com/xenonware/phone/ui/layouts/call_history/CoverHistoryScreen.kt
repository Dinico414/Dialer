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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.LargePadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.NoSpacing
import com.xenon.mylibrary.values.SmallSpacing
import com.xenonware.phone.R
import com.xenonware.phone.viewmodel.LayoutType

@Composable
fun CoverHistoryScreen(
    onNavigateBack: () -> Unit,
    layoutType: LayoutType,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
) {
    var callLogs by remember { mutableStateOf<List<CallLogEntry>>(emptyList()) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            callLogs = loadCallLogEntries(context)
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

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
    }

    ActivityScreen(
        titleText = stringResource(id = R.string.call_history),

        expandable = isAppBarExpandable,

        navigationIconStartPadding = MediumPadding,
        navigationIconPadding = MediumPadding,
        navigationIconSpacing = NoSpacing,
        navigationIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.navigate_back_description),
                modifier = Modifier.size(24.dp)
            )
        },
        onNavigationIconClick = onNavigateBack,
        hasNavigationIconExtraContent = false,
        actions = {},
        content = { _ ->
            Column(modifier = modifier.fillMaxSize()) {
                if (callLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No call history\nor permission denied",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val groupedCalls = remember(callLogs) { groupCallLogsByDate(callLogs) }

                    val listState = rememberLazyListState()


                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(SmallSpacing),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = with(LocalDensity.current) {
                                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                        64.dp + LargePadding * 2
                            }
                        )
                    ) {
                        groupedCalls.forEach { group ->
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = group.title,
                                        fontSize = 20.sp,
                                        fontFamily = QuicksandTitleVariable,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(group.entries.indices.toList()) { index ->
                                val entry = group.entries[index]
                                val isFirst = index == 0
                                val isLast = index == group.entries.lastIndex
                                val isSingle = group.entries.size == 1

                                CallHistoryItemCard(
                                    entry = entry,
                                    isFirstInGroup = isFirst,
                                    isLastInGroup = isLast,
                                    isSingle = isSingle
                                )
                            }
                        }
                    }
                }
            }
        })


}

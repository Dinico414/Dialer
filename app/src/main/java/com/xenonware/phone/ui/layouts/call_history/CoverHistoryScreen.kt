package com.xenonware.phone.ui.layouts.call_history

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.MediumSpacing
import com.xenon.mylibrary.values.NoCornerRadius
import com.xenon.mylibrary.values.SmallSpacing
import com.xenonware.phone.R
import com.xenonware.phone.ui.layouts.main.contacts.isScrolledToEnd
import com.xenonware.phone.viewmodel.CallHistoryViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch


@OptIn(ExperimentalHazeMaterialsApi::class)
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun CoverHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: CallHistoryViewModel,
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
    val coverScreenBackgroundColor = Color.Black
    val coverScreenContentColor = Color.White

    ActivityScreen(
        titleText = stringResource(R.string.call_history),
        expandable = false,
        screenBackgroundColor = coverScreenBackgroundColor,
        contentBackgroundColor = coverScreenBackgroundColor,
        appBarNavigationIconContentColor = coverScreenContentColor,
        contentCornerRadius = NoCornerRadius,
        navigationIconStartPadding = MediumPadding,
        navigationIconPadding = MediumPadding,
        navigationIconSpacing = MediumSpacing,
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
                                text = if (hasPermission) stringResource(R.string.no_calls_yet) else stringResource(
                                    R.string.permission_required
                                ),
                                fontSize = 18.sp,
                                lineHeight = 28.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        val groupedCalls = groupCallLogsByDate(
                            entries = callLogs,
                            todayStr = stringResource(R.string.today),
                            yesterdayStr = stringResource(R.string.yesterday),
                        )
                        val listState = rememberLazyListState()
                        val coroutineScope = rememberCoroutineScope()

                        val hazeState = remember { HazeState() }

                        val showScrollToTop by remember {
                            derivedStateOf {
                                listState.firstVisibleItemIndex > 0 ||
                                        listState.firstVisibleItemScrollOffset > 200
                            }
                        }

                        val showScrollToBottom by remember {
                            derivedStateOf {
                                !listState.isScrolledToEnd()
                            }
                        }

                        val showButton by remember {
                            derivedStateOf { showScrollToTop && showScrollToBottom }
                        }


                        val buttonAlpha by animateFloatAsState(
                            targetValue = if (showButton) 1f else 0f,
                            animationSpec = tween(300),
                            label = "scroll button alpha"
                        )

                        val buttonScale by animateFloatAsState(
                            targetValue = if (showButton) 1f else 0.8f, animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            ), label = "scroll button scale"
                        )

                        val hazeThinColor = MaterialTheme.colorScheme.primary

                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {

                            LazyColumn(
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(SmallSpacing),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .hazeSource(state = hazeState),
                                contentPadding = PaddingValues(
                                    bottom = with(LocalDensity.current) {
                                        WindowInsets.navigationBars.asPaddingValues()
                                            .calculateBottomPadding()
                                    })) {
                                groupedCalls.forEach { group ->
                                    item(key = "header_${group.title}") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
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

                            if (buttonAlpha > 0.02f) {
                                IconButton(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(
                                            bottom = with(LocalDensity.current) {
                                                WindowInsets.navigationBars.asPaddingValues()
                                                    .calculateBottomPadding() + 16.dp
                                            })
                                        .scale(buttonScale)
                                        .clip(CircleShape)
                                        .alpha(buttonAlpha)
                                        .hazeEffect(
                                            state = hazeState,
                                            style = HazeMaterials.ultraThin(hazeThinColor)
                                        ), colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ), onClick = {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(0)
                                        }
                                    }) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowUp,
                                        contentDescription = "Scroll to top",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        })
}
package com.xenonware.phone.ui.layouts.main.contacts

import android.content.Intent
import android.telecom.Call
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenon.mylibrary.values.SmallSpacing
import com.xenon.mylibrary.values.SmallestCornerRadius
import com.xenonware.phone.R
import com.xenonware.phone.data.Contact
import com.xenonware.phone.ui.layouts.main.dialer_screen.safePlaceCall
import com.xenonware.phone.util.PhoneNumberFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch

data class ContactGroup(val letter: Char, val contacts: List<Contact>)

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun ContactsScreen(
    modifier: Modifier = Modifier,
    contactsToShow: List<Contact>,
    searchQuery: String = "",
    contentPadding: PaddingValues
) {
    val hazeState = remember { HazeState() }
    if (contactsToShow.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (contactsToShow.isEmpty() && searchQuery.isNotBlank())
                    stringResource(R.string.no_contacts_found)
                else
                    stringResource(R.string.no_contacts_found),
                color = colorScheme.onSurfaceVariant,
                fontSize = 18.sp
            )
        }
        return
    }

    val groupedContacts = remember(contactsToShow) {
        contactsToShow.sortedBy { it.name }
            .groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
            .map { (letter, list) -> ContactGroup(letter, list) }
            .sortedBy { it.letter }
    }

    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {

        val coroutineScope = rememberCoroutineScope()

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(SmallSpacing),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .hazeSource(state = hazeState),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding()
            )
        ) {
            groupedContacts.forEach { group ->
                item(key = "header_${group.letter}") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorScheme.surfaceContainer)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = group.letter.toString(),
                            fontSize = 20.sp,
                            fontFamily = QuicksandTitleVariable,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                itemsIndexed(
                    items = group.contacts,
                    key = { _, contact -> contact.id }
                ) { index, contact ->
                    ContactItemCard(
                        contact = contact,
                        isFirstInGroup = index == 0,
                        isLastInGroup = index == group.contacts.lastIndex,
                        isSingle = group.contacts.size == 1
                    )
                }
            }
        }

        // Scroll to top button
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
            targetValue = if (showButton) 1f else 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "scroll button scale"
        )

        val hazeThinColor = colorScheme.primary

        if (buttonAlpha > 0.02f) {
            IconButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = contentPadding.calculateBottomPadding(),
                    )
                    .scale(buttonScale)
                    .clip(CircleShape)
                    .alpha(buttonAlpha)
                    .hazeEffect(
                        state = hazeState,
                        style = HazeMaterials.ultraThin(hazeThinColor)
                    ),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = colorScheme.onSurface
                ),
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Scroll to top",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ContactItemCard(
    contact: Contact,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    isSingle: Boolean,
) {
    val context = LocalContext.current

    var isExpanded by remember { mutableStateOf(false) }

    val shape = when {
        isSingle -> RoundedCornerShape(MediumCornerRadius)
        isFirstInGroup -> RoundedCornerShape(
            topStart = MediumCornerRadius,
            topEnd = MediumCornerRadius,
            bottomStart = SmallestCornerRadius,
            bottomEnd = SmallestCornerRadius
        )
        isLastInGroup -> RoundedCornerShape(
            topStart = SmallestCornerRadius,
            topEnd = SmallestCornerRadius,
            bottomStart = MediumCornerRadius,
            bottomEnd = MediumCornerRadius
        )
        else -> RoundedCornerShape(SmallestCornerRadius)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceBright)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContactAvatar(contact = contact)

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        fontFamily = QuicksandTitleVariable,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = PhoneNumberFormatter.formatForDisplay(contact.phone, LocalContext.current),
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(48.dp)
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand actions",
                        tint = colorScheme.onSurface,
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(rotation)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(300, delayMillis = 100)),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeOut(animationSpec = tween(220))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // SMS Button
                    FilledTonalIconButton(
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFFFFB300)
                        ),
                        shape = RoundedCornerShape(
                            topStart = 12.dp, topEnd = 4.dp,
                            bottomStart = 12.dp, bottomEnd = 4.dp
                        ),
                        onClick = {
                            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "smsto:${contact.phone}".toUri()
                            }
                            context.startActivity(smsIntent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Message,
                            contentDescription = "Send SMS",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Call button
                    FilledTonalIconButton(
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        onClick = {
                            if (contact.phone.isNotEmpty()) {
                                safePlaceCall(context, contact.phone)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = "Call",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Info Button
                    FilledTonalIconButton(
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(
                            topStart = 4.dp, topEnd = 12.dp,
                            bottomStart = 4.dp, bottomEnd = 12.dp
                        ),
                        onClick = {
                            // TODO: Open contact detail / edit screen
                        },
                        modifier = Modifier
                            .weight(0.5f)
                            .widthIn(max = 56.dp)
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Contact Info",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactAvatar(contact: Contact, modifier: Modifier = Modifier) {
    val firstLetter = contact.name.firstOrNull()?.uppercaseChar() ?: '?'
    val pastelBackground = remember(contact.name) {
        val hash = contact.name.hashCode()
        val hue = (hash % 360).toFloat().let { if (it < 0) it + 360 else it }
        Color.hsl(hue = hue, saturation = 0.5f, lightness = 0.80f)
    }
    val textColor = remember(contact.name) {
        val hash = contact.name.hashCode()
        val hue = (hash % 360).toFloat().let { if (it < 0) it + 360 else it }
        Color.hsl(hue = hue, saturation = 0.6f, lightness = 0.25f)
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .background(pastelBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = firstLetter.toString(),
            fontFamily = QuicksandTitleVariable,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
fun RingingContactAvatar(
    modifier: Modifier = Modifier,
    contact: Contact,
    state: Int = -1,
    size: androidx.compose.ui.unit.Dp = 64.dp,
) {
    val isRinging = state in setOf(
        Call.STATE_RINGING, Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_PULLING_CALL
    )

    val stateMorphProgress by animateFloatAsState(
        targetValue = if (isRinging) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "StateMorph"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "expressiveIndeterminate")

    val baseRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BaseRotation"
    )

    val kickRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1f)),
            repeatMode = RepeatMode.Restart
        ),
        label = "KickRotation"
    )

    val liquidPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val finalRotation = (baseRotation + kickRotation) * stateMorphProgress
    val finalScale = 1f + 0.5f * stateMorphProgress
    val pulseFactor = 1f + (liquidPulse - 1f) * stateMorphProgress
    val finalProgress = (stateMorphProgress * pulseFactor).coerceIn(0f, 1f)

    val circle = remember { RoundedPolygon.circle(numVertices = 8) }
    val expressiveShape = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.6f,
            rounding = CornerRounding(0.22f)
        )
    }
    val morph = remember(circle, expressiveShape) { Morph(circle, expressiveShape) }

    val hash = contact.name.hashCode()
    val hue = (hash % 360).toFloat().let { if (it < 0) it + 360 else it }
    val pastelBackground = Color.hsl(hue = hue, saturation = 0.45f, lightness = 0.85f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        val currentMorphShape = remember(finalProgress) {
            MorphPolygonShape(morph, finalProgress)
        }

        val shadowTint = colorScheme.scrim.copy(alpha = 0.6f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = finalRotation
                    scaleX = finalScale
                    scaleY = finalScale
                    shadowElevation = 10.dp.toPx()
                    spotShadowColor = shadowTint
                    ambientShadowColor = shadowTint
                    shape = currentMorphShape
                    clip = true
                }
                .background(pastelBackground)
        )
        Text(
            text = contact.name.firstOrNull()?.uppercase() ?: "?",
            fontSize = (size.value * 0.42).sp,
            fontFamily = QuicksandTitleVariable,
            fontWeight = FontWeight.Bold,
            color = Color.hsl(hue = hue, saturation = 0.7f, lightness = 0.2f)
        )
    }
}

class MorphPolygonShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    private val matrix = android.graphics.Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = morph.toPath(progress).asComposePath()
        matrix.reset()
        matrix.setScale(size.width / 2f, size.height / 2f)
        matrix.postTranslate(size.width / 2f, size.height / 2f)
        path.asAndroidPath().transform(matrix)
        return Outline.Generic(path)
    }
}

fun LazyListState.isScrolledToEnd(): Boolean {
    val info = layoutInfo
    if (info.totalItemsCount == 0) return true
    val last = info.visibleItemsInfo.lastOrNull() ?: return true
    return last.index == info.totalItemsCount - 1 &&
            last.offset + last.size <= info.viewportEndOffset
}
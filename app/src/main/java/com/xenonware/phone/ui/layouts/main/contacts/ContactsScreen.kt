package com.xenonware.phone.ui.layouts.main.contacts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import android.telecom.Call
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenon.mylibrary.values.SmallSpacing
import com.xenon.mylibrary.values.SmallestCornerRadius

data class Contact(val name: String, val phoneNumber: String? = null)
data class ContactGroup(val letter: Char, val contacts: List<Contact>)

@Composable
fun ContactsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) contacts = loadContacts(context)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (!hasPermission) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Contacts permission required", fontSize = 20.sp)
            }
        } else if (contacts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No contacts found", fontSize = 20.sp)
            }
        } else {
            val groupedContacts = remember(contacts) {
                contacts.sortedBy { it.name }
                    .groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
                    .map { (letter, list) ->
                        ContactGroup(letter, list.sortedBy { it.name })
                    }.sortedBy { it.letter }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(SmallSpacing),
                modifier = Modifier.padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    bottom = with(LocalDensity.current) {
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues()
                            .calculateBottomPadding() + 64.dp + LargestPadding * 2
                    })
            ) {
                groupedContacts.forEach { group ->
                    // Letter Header
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colorScheme.surfaceContainer)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .padding(top = 8.dp)
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

                    // Individual contact cards with dynamic corner radius
                    items(group.contacts.indices.toList()) { index ->
                        val contact = group.contacts[index]
                        val isFirst = index == 0
                        val isLast = index == group.contacts.lastIndex
                        val isSingle = group.contacts.size == 1

                        ContactItemCard(
                            contact = contact,
                            isFirstInGroup = isFirst,
                            isLastInGroup = isLast,
                            isSingle = isSingle
                        )

                    }
                }
            }
        }
    }
}

@Composable
fun ContactItemCard(
    contact: Contact, isFirstInGroup: Boolean, isLastInGroup: Boolean, isSingle: Boolean,
) {
    val context = LocalContext.current

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
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceBright),
    ) {
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
                    text = contact.phoneNumber.toString(),
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    shape = RoundedCornerShape(
                        topStart = 24.dp, bottomStart = 24.dp, topEnd = 4.dp, bottomEnd = 4.dp
                    ), onClick = {
                        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "smsto:${contact.phoneNumber}".toUri()
                        }
                        context.startActivity(smsIntent)
                    }, modifier = Modifier
                        .size(48.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 24.dp,
                                bottomStart = 24.dp,
                                topEnd = 4.dp,
                                bottomEnd = 4.dp

                            )
                        )
                        .background(colorScheme.surfaceContainerHigh)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Message,
                        contentDescription = "Send SMS",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(24.dp)
                    )
                }

                IconButton(
                    shape = RoundedCornerShape(
                        topStart = 4.dp, bottomStart = 4.dp, topEnd = 24.dp, bottomEnd = 24.dp
                    ),
                    onClick = { safePlaceCall(context, contact.phoneNumber.toString()) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 4.dp,
                                bottomStart = 4.dp,
                                topEnd = 24.dp,
                                bottomEnd = 24.dp
                            )
                        )
                        .background(colorScheme.surfaceContainerHigh)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Call",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(24.dp)
                    )
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
        Call.STATE_RINGING,
        Call.STATE_DIALING,
        Call.STATE_CONNECTING,
        Call.STATE_PULLING_CALL
    )

    val stateMorphProgress by animateFloatAsState(
        targetValue = if (isRinging) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "StateMorph"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "expressiveIndeterminate")

    // 1. THE CONSTANT FLOW (Linear)
    // This ensures there is ALWAYS motion. It never stops.
    val baseRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing), repeatMode = RepeatMode.Restart
        ), label = "BaseRotation"
    )

    // 2. THE EXPRESSIVE KICK (Bezier)
    // This adds the "speed bursts" on top of the base rotation.
    val kickRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1f)
            ), repeatMode = RepeatMode.Restart
        ), label = "KickRotation"
    )

    // 3. THE LIQUID PULSE
    val liquidPulse by infiniteTransition.animateFloat(
        initialValue = 0.82f, targetValue = 1.12f, animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse
        ), label = "Pulse"
    )

    // Logic: Combine base flow + expressive kick only when ringing
    val finalRotation = if (isRinging) baseRotation + kickRotation else 0f
    val finalProgress =
        if (isRinging) (stateMorphProgress * liquidPulse).coerceIn(0f, 1f) else stateMorphProgress

    val circle = remember { RoundedPolygon.circle(numVertices = 8) }
    val expressiveShape = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 8, innerRadius = 0.6f, rounding = CornerRounding(0.22f)
        )
    }
    val morph = remember(circle, expressiveShape) { Morph(circle, expressiveShape) }

    val hash = contact.name.hashCode()
    val hue = (hash % 360).toFloat().let { if (it < 0) it + 360 else it }
    val pastelBackground = Color.hsl(hue = hue, saturation = 0.45f, lightness = 0.85f)

    Box(
        modifier = modifier.size(size), contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = finalRotation

                    // Pop the scale slightly higher to make it feel "Big"
                    val scaleFactor = if (isRinging) 1.5f else 1.0f
                    scaleX = scaleFactor
                    scaleY = scaleFactor

                    clip = true
                    shape = MorphPolygonShape(morph, finalProgress)
                }
                .background(pastelBackground))

        Text(
            text = contact.name.firstOrNull()?.uppercase() ?: "?",
            fontSize = (size.value * 0.42).sp,
            fontFamily = QuicksandTitleVariable,
            fontWeight = FontWeight.Bold,
            color = Color.hsl(hue = hue, saturation = 0.7f, lightness = 0.2f)
        )
    }
}

// Custom Shape to bridge RoundedPolygon and Compose Shape
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
        // Scale path to fit the actual Box size
        matrix.reset()
        matrix.setScale(size.width / 2f, size.height / 2f)
        matrix.postTranslate(size.width / 2f, size.height / 2f)
        path.asAndroidPath().transform(matrix)

        return Outline.Generic(path)
    }
}

private fun loadContacts(context: Context): List<Contact> {
    val list = mutableListOf<Contact>()
    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ), null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
    )
    cursor?.use {
        val nameIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (it.moveToNext()) {
            val name = it.getString(nameIdx) ?: "Unknown"
            val number = it.getString(numIdx) ?: ""
            if (number.isNotBlank() && name.isNotBlank()) {
                list.add(Contact(name.trim(), number))
            }
        }
    }
    return list.distinctBy { it.phoneNumber }
}

private fun safePlaceCall(context: Context, phoneNumber: String) {
    val uri = "tel:$phoneNumber".toUri()
    val intent = Intent(Intent.ACTION_CALL, uri)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to place call", Toast.LENGTH_SHORT).show()
    }
}
package com.vinay.tvlauncher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class TileLook(
    val style: Int,
    val sizeMul: Float,
    val corner: Dp,
    val labels: Boolean,
    val glass: Int,
    val blur: Int
) {
    val shape: Shape = RoundedCornerShape(corner)
}

@Composable
private fun rememberTileLook(): TileLook {
    val ctx = LocalContext.current
    val t = Prefs.tick
    return remember(t) {
        TileLook(
            style = Prefs.iconStyle(ctx),
            sizeMul = when (Prefs.iconSize(ctx)) {
                0 -> 0.8f
                2 -> 1.3f
                else -> 1f
            },
            corner = when (Prefs.tileCorner(ctx)) {
                0 -> 8.dp
                2 -> 28.dp
                else -> 16.dp
            },
            labels = Prefs.showLabels(ctx),
            glass = Prefs.glassStyle(ctx),
            blur = Prefs.blur(ctx)
        )
    }
}

private fun Modifier.tileSurface(focused: Boolean, look: TileLook): Modifier {
    val frosted = look.glass == 1
    val a = (if (frosted) 0.55f + look.blur * 0.04f else 0.80f + look.blur * 0.02f)
        .coerceAtMost(1f)
    val top = if (frosted) {
        if (focused) Color(0xFFB5BBC6) else Color(0xFF9AA1AD)
    } else {
        if (focused) Color(0xFF55555C) else Color(0xFF3C3C42)
    }
    val bot = if (frosted) {
        if (focused) Color(0xFF858B96) else Color(0xFF6B717C)
    } else {
        if (focused) Color(0xFF1F1F23) else Color(0xFF131316)
    }
    return this
        .clip(look.shape)
        .background(
            Brush.verticalGradient(listOf(top.copy(alpha = a), bot.copy(alpha = a)))
        )
        .then(
            if (focused) {
                Modifier.border(2.5.dp, Color.White.copy(alpha = 0.9f), look.shape)
            } else {
                Modifier.border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.03f))
                    ),
                    look.shape
                )
            }
        )
}

@Composable
private fun TileGloss() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.13f),
                    0.45f to Color.Transparent,
                    1f to Color.Transparent
                )
            )
    )
}

@Composable
private fun TileIcon(item: AppItem, base: Dp, look: TileLook, tileW: Dp, tileH: Dp) {
    val isSettings = item.pkg == "com.android.tv.settings" ||
        item.pkg == "com.android.settings"
    val maxS = minOf(tileW, tileH) - 8.dp
    val plainSize = (base * look.sizeMul).coerceAtMost(maxS)
    val logoSize = (base * look.sizeMul * 1.2f).coerceAtMost(maxS)

    when (look.style) {
        0 -> {
            // tvOS Icons: sirf logo, bina safed peeche ke
            if (isSettings) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = item.label,
                    tint = Color(0xFFD5D8DD),
                    modifier = Modifier.size(logoSize)
                )
            } else {
                Image(
                    item.logo ?: item.icon,
                    contentDescription = item.label,
                    modifier = Modifier.size(logoSize)
                )
            }
        }
        1 -> {
            // Original Banners: app ka TV banner, na ho to logo
            val bn = item.banner
            if (bn != null) {
                Image(
                    bn,
                    contentDescription = item.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Image(
                    item.logo ?: item.icon,
                    contentDescription = item.label,
                    modifier = Modifier.size(logoSize)
                )
            }
        }
        2 -> Image(
            item.icon,
            contentDescription = item.label,
            modifier = Modifier
                .size(plainSize)
                .clip(RoundedCornerShape(plainSize * 0.22f))
        )
        3 -> Image(
            item.icon,
            contentDescription = item.label,
            modifier = Modifier
                .size(plainSize)
                .clip(CircleShape)
        )
        4 -> Image(
            item.icon,
            contentDescription = item.label,
            modifier = Modifier.size(plainSize),
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        )
        else -> Image(
            item.icon,
            contentDescription = item.label,
            modifier = Modifier.size(plainSize)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRectTile(
    item: AppItem,
    onClick: () -> Unit,
    width: Dp,
    height: Dp,
    iconSize: Dp,
    requester: FocusRequester? = null,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onFocused: (() -> Unit)? = null
) {
    val look = rememberTileLook()
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.12f)
    val labelAlpha = animateFloatAsState(
        targetValue = if (focused || look.labels) 1f else 0f,
        label = "labelAlpha"
    ).value
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier
    var longFired by remember { mutableStateOf(false) }
    val click = if (onLongClick != null) {
        Modifier
            .onPreviewKeyEvent { e ->
                val ok = e.key == Key.Enter || e.key == Key.DirectionCenter ||
                    e.key == Key.NumPadEnter
                if (!ok) {
                    false
                } else if (e.type == KeyEventType.KeyDown) {
                    if (e.nativeKeyEvent.repeatCount > 0) {
                        // OK dabaye rakha: popup kholo (sirf ek baar)
                        if (!longFired) {
                            longFired = true
                            onLongClick()
                        }
                        true
                    } else {
                        longFired = false
                        false
                    }
                } else {
                    // Chhodte waqt, agar popup khul chuka to click mat karo
                    if (longFired) {
                        longFired = false
                        true
                    } else false
                }
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Column(
        base
            .width(width)
            .graphicsLayer { scaleX = s; scaleY = s }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused?.invoke()
            }
            .then(click),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(width, height).tileSurface(focused, look),
            contentAlignment = Alignment.Center
        ) {
            TileGloss()
            TileIcon(item, iconSize, look, width, height)
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFC107))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("FAV", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            item.label,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .height(22.dp)
                .graphicsLayer { alpha = labelAlpha }
        )
    }
}

@Composable
fun FolderTile(
    name: String,
    apps: List<AppItem>,
    onClick: () -> Unit,
    width: Dp,
    height: Dp
) {
    val look = rememberTileLook()
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.12f)
    val shown = apps.take(4)

    Column(
        Modifier
            .width(width)
            .graphicsLayer { scaleX = s; scaleY = s }
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(width, height).tileSurface(focused, look),
            contentAlignment = Alignment.Center
        ) {
            TileGloss()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (r in 0 until 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in 0 until 2) {
                            val a = shown.getOrNull(r * 2 + c)
                            if (a != null) {
                                Image(
                                    a.logo ?: a.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Spacer(Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
        Text(
            name,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(22.dp)
        )
    }
}

@Composable
fun AllAppsTile(onClick: () -> Unit, width: Dp, height: Dp) {
    val look = rememberTileLook()
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.12f)
    val labelAlpha = animateFloatAsState(
        targetValue = if (focused || look.labels) 1f else 0f,
        label = "labelAlpha"
    ).value

    Column(
        Modifier
            .width(width)
            .graphicsLayer { scaleX = s; scaleY = s }
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(width, height).tileSurface(focused, look),
            contentAlignment = Alignment.Center
        ) {
            TileGloss()
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(2) {
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.85f))
                            )
                        }
                    }
                }
            }
        }
        Text(
            "All Apps",
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .height(22.dp)
                .graphicsLayer { alpha = labelAlpha }
        )
    }
}

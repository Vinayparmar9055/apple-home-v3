package com.vinay.tvlauncher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TileShape = RoundedCornerShape(18.dp)

private fun Modifier.tileSurface(focused: Boolean): Modifier = this
    .clip(TileShape)
    .background(Brush.verticalGradient(listOf(Color(0xFF3A3A40), Color(0xFF1C1C20))))
    .then(
        if (focused) Modifier.border(2.dp, Color.White.copy(alpha = 0.85f), TileShape)
        else Modifier
    )

@Composable
fun AppRectTile(
    item: AppItem,
    onClick: () -> Unit,
    width: Dp,
    height: Dp,
    iconSize: Dp,
    requester: FocusRequester? = null,
    selected: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.12f)
    val labelAlpha = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        label = "labelAlpha"
    ).value
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier

    Column(
        base
            .width(width)
            .graphicsLayer { scaleX = s; scaleY = s }
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(width, height).tileSurface(focused),
            contentAlignment = Alignment.Center
        ) {
            Image(item.icon, contentDescription = item.label, modifier = Modifier.size(iconSize))
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
fun AllAppsTile(onClick: () -> Unit, width: Dp, height: Dp) {
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.12f)
    val labelAlpha = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
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
            Modifier.size(width, height).tileSurface(focused),
            contentAlignment = Alignment.Center
        ) {
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

package com.vinay.tvlauncher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppIconTile(
    item: AppItem,
    onClick: () -> Unit,
    requester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.15f)
    val labelAlpha = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        label = "labelAlpha"
    ).value
    val shape = RoundedCornerShape(22.dp)
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier

    Column(
        base
            .width(104.dp)
            .graphicsLayer { scaleX = s; scaleY = s }
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF3A3A40), Color(0xFF1C1C20))
                    )
                )
                .border(
                    if (focused) 2.dp else 0.dp,
                    Color.White.copy(alpha = 0.85f),
                    shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(item.icon, contentDescription = item.label, modifier = Modifier.size(64.dp))
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

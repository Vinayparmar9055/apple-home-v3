package com.vinay.tvlauncher

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

object OrderStore {
    private fun sp(ctx: Context) =
        ctx.getSharedPreferences("apple_home", Context.MODE_PRIVATE)

    fun list(ctx: Context): List<String> =
        (sp(ctx).getString("app_order", "") ?: "").split(",").filter { it.isNotBlank() }

    fun save(ctx: Context, l: List<String>) {
        sp(ctx).edit().putString("app_order", l.joinToString(",")).apply()
    }
}

@Composable
fun MoveHint(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.92f))
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Text(text, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DRow(
    icon: ImageVector,
    text: String,
    requester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier
    Row(
        base
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Color(0xFFF2F2F2) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (focused) Color.Black else Color.White,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text,
            color = if (focused) Color.Black else Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun DockMenu(
    app: AppItem,
    onMove: () -> Unit,
    onRemove: () -> Unit,
    onClose: () -> Unit
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { first.requestFocus() }
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .width(380.dp)
                    .glass(RoundedCornerShape(26.dp), false)
                    .padding(14.dp)
            ) {
                Text(
                    app.label,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
                )
                DRow(Icons.Rounded.GridView, "Move tile", requester = first) { onMove() }
                DRow(Icons.Rounded.Star, "Remove from Favourites") { onRemove() }
            }
        }
    }
}

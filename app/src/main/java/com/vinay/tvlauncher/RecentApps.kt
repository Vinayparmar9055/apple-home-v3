package com.vinay.tvlauncher

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

object RecentStore {
    private fun sp(ctx: Context) =
        ctx.getSharedPreferences("apple_home", Context.MODE_PRIVATE)

    fun list(ctx: Context): List<String> =
        (sp(ctx).getString("recent", "") ?: "").split(",").filter { it.isNotBlank() }

    private fun save(ctx: Context, l: List<String>) {
        sp(ctx).edit().putString("recent", l.joinToString(",")).apply()
    }

    fun push(ctx: Context, pkg: String) {
        val l = list(ctx).toMutableList()
        if (l.firstOrNull() == pkg) return
        l.remove(pkg)
        l.add(0, pkg)
        save(ctx, l.take(12))
    }

    fun remove(ctx: Context, pkg: String) {
        save(ctx, list(ctx) - pkg)
    }

    fun clear(ctx: Context) {
        save(ctx, emptyList())
    }
}

@Composable
private fun RecentCard(
    app: AppItem,
    requester: FocusRequester?,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.1f)
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier
    Column(
        base
            .width(240.dp)
            .graphicsLayer { scaleX = s; scaleY = s }
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionUp) {
                    onClose()
                    true
                } else false
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(240.dp, 150.dp)
                .glass(RoundedCornerShape(22.dp), focused),
            contentAlignment = Alignment.Center
        ) {
            Image(app.icon, contentDescription = app.label, modifier = Modifier.size(76.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            app.label,
            color = Color.White.copy(alpha = if (focused) 1f else 0.75f),
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RecentAppsScreen(
    apps: List<AppItem>,
    onLaunch: (AppItem) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var pkgs by remember { mutableStateOf(RecentStore.list(ctx)) }
    val items = pkgs.mapNotNull { p -> apps.firstOrNull { it.pkg == p } }
    val first = remember { FocusRequester() }

    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) {
            delay(100)
            runCatching { first.requestFocus() }
        }
    }

    fun closeAll() {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        pkgs.forEach { p -> runCatching { am.killBackgroundProcesses(p) } }
        RecentStore.clear(ctx)
        pkgs = emptyList()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlassButton("‹ Back", onBack)
            Spacer(Modifier.weight(1f))
            Text(
                "Recent Apps",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.weight(1f))
            GlassButton("Close All", { closeAll() })
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (items.isEmpty()) {
                Text(
                    "Koi recent app nahi hai",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 20.sp
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    itemsIndexed(items, key = { _, a -> a.pkg }) { i, app ->
                        RecentCard(
                            app = app,
                            requester = if (i == 0) first else null,
                            onClick = { onLaunch(app) },
                            onClose = {
                                RecentStore.remove(ctx, app.pkg)
                                pkgs = pkgs - app.pkg
                            }
                        )
                    }
                }
            }
        }

        Text(
            "Card par upar ka button dabao: band karne ke liye",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

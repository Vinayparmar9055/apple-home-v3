package com.vinay.tvlauncher

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val USER_NAME = "Vinay"

private fun defaultFavs(apps: List<AppItem>): List<String> {
    val preferred = listOf(
        "com.google.android.youtube.tv",
        "com.amazon.amazonvideo.livingroom",
        "com.netflix.ninja",
        "com.android.vending",
        "com.google.android.youtube.tvmusic"
    )
    val have = preferred.filter { p -> apps.any { it.pkg == p } }
    return have.ifEmpty { apps.take(5).map { it.pkg } }
}

@Composable
fun HomeScreen(
    apps: List<AppItem>,
    onSettings: () -> Unit,
    onControl: () -> Unit,
    onLaunch: (AppItem) -> Unit
) {
    val ctx = LocalContext.current
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1000)
        }
    }
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    val hour = Calendar.getInstance().apply { time = now }.get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else -> "Good night"
    }

    val favPkgs = remember(apps) {
        Prefs.favourites(ctx).ifEmpty { defaultFavs(apps) }
    }
    val favApps = favPkgs.mapNotNull { p -> apps.firstOrNull { it.pkg == p } }

    val first = remember { FocusRequester() }
    LaunchedEffect(apps.isNotEmpty()) {
        if (apps.isNotEmpty()) {
            delay(200)
            runCatching { first.requestFocus() }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 48.dp, end = 48.dp, top = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column {
                Text(
                    timeFmt.format(now),
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light
                )
                Text(dateFmt.format(now), color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "$greeting, $USER_NAME",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton("Settings", onSettings)
                GlassButton("Control Center", onControl)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (apps.isEmpty()) {
            Text("Apps load ho rahe hain...", color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (favApps.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        FavShelf(favApps, onLaunch, first)
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "All Apps",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                    )
                }
                items(apps, key = { it.pkg }) { app ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        AppIconTile(
                            item = app,
                            onClick = { onLaunch(app) },
                            requester = if (favApps.isEmpty() && app == apps.first()) first else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavShelf(
    favs: List<AppItem>,
    onLaunch: (AppItem) -> Unit,
    first: FocusRequester
) {
    Box(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(28.dp), false)
            .padding(vertical = 14.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            itemsIndexed(favs, key = { _, a -> a.pkg }) { index, app ->
                AppIconTile(
                    item = app,
                    onClick = { onLaunch(app) },
                    requester = if (index == 0) first else null
                )
            }
        }
    }
}

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val USER_NAME = "Vinay"

fun defaultFavs(apps: List<AppItem>): List<String> {
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
    favPkgs: List<String>,
    onSettings: () -> Unit,
    onControl: () -> Unit,
    onAllApps: () -> Unit,
    onLaunch: (AppItem) -> Unit
) {
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
            .padding(start = 48.dp, end = 48.dp, top = 28.dp, bottom = 24.dp)
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

        Spacer(Modifier.weight(1f))

        Dock(favApps, onLaunch, onAllApps, first)
    }
}

@Composable
fun Dock(
    favs: List<AppItem>,
    onLaunch: (AppItem) -> Unit,
    onAllApps: () -> Unit,
    first: FocusRequester
) {
    Box(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(28.dp), false)
            .padding(vertical = 16.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            itemsIndexed(favs, key = { _, a -> a.pkg }) { index, app ->
                AppRectTile(
                    item = app,
                    onClick = { onLaunch(app) },
                    width = 104.dp,
                    height = 72.dp,
                    iconSize = 44.dp,
                    requester = if (index == 0) first else null
                )
            }
            item { AllAppsTile(onAllApps, 104.dp, 72.dp) }
        }
    }
}

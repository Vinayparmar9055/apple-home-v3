package com.vinay.tvlauncher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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

@Composable
fun HomeScreen(
    apps: List<AppItem>,
    onSettings: () -> Unit,
    onControl: () -> Unit,
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

    val first = remember { FocusRequester() }
    LaunchedEffect(apps.isNotEmpty()) {
        if (apps.isNotEmpty()) {
            delay(150)
            runCatching { first.requestFocus() }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = 40.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column {
                Text(
                    timeFmt.format(now),
                    color = Color.White,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light
                )
                Text(dateFmt.format(now), color = Color.White.copy(alpha = 0.7f), fontSize = 20.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "$greeting, $USER_NAME",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassButton("Settings", onSettings)
                GlassButton("Control Center", onControl)
            }
        }

        Spacer(Modifier.weight(1f))

        if (apps.isEmpty()) {
            Text("Apps load ho rahe hain...", color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 24.dp, vertical = 36.dp
                )
            ) {
                itemsIndexed(apps, key = { _, a -> a.pkg }) { index, app ->
                    AppTile(
                        item = app,
                        onClick = { onLaunch(app) },
                        requester = if (index == 0) first else null
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

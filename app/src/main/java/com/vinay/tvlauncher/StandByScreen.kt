package com.vinay.tvlauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StandByScreen(onExit: () -> Unit) {
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1000)
        }
    }
    val timeFmt = remember { SimpleDateFormat("hh:mm", Locale.getDefault()) }
    val ampmFmt = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    val req = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(200)
        runCatching { req.requestFocus() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent {
                if (it.type == KeyEventType.KeyUp) onExit()
                true
            }
            .focusRequester(req)
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                timeFmt.format(now),
                color = Color.White,
                fontSize = 120.sp,
                fontWeight = FontWeight.Light
            )
            Text(ampmFmt.format(now), color = Color.White.copy(alpha = 0.7f), fontSize = 28.sp)
            Text(dateFmt.format(now), color = Color.White.copy(alpha = 0.6f), fontSize = 22.sp)
        }
    }
}

package com.vinay.tvlauncher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val compactShadow = TextStyle(
    shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 8f)
)

// Left upar chhota block: sirf "9:21 AM" aur peela icon + temperature
// (kate wali ghadi ho to chhoti ghadi). OK = bada popup, OK dabake rakho = ghadi ka style badlo.
// Focus aane par Vinay Parmar wali patti jitna glass dabba dikhta hai.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LeftInfoCompact(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { ClockPrefs.load(ctx) }
    val style = ClockPrefs.style
    val ver = WeatherStore.version
    var data by remember { mutableStateOf<WeatherData?>(null) }
    var aqi by remember { mutableStateOf<Int?>(null) }
    var showPopup by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.06f)

    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1000)
        }
    }
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    LaunchedEffect(ver) {
        while (true) {
            val city = WeatherStore.city(ctx)
            val w = fetchWeather(city)
            if (w != null) {
                data = w
                aqi = fetchAqi(city)
            }
            delay(30 * 60 * 1000L)
        }
    }

    val d = data
    val showWeather = d != null && WeatherStore.enabled(ctx)

    Row(
        modifier
            .graphicsLayer {
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .onFocusChanged { focused = it.isFocused }
            .then(if (focused) Modifier.glass(RoundedCornerShape(50), true) else Modifier)
            .combinedClickable(
                onClick = { showPopup = true },
                onLongClick = { ClockPrefs.set(ctx, (style + 1) % 3) }
            )
            .heightIn(min = 60.dp)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (style == 0) {
            Text(
                timeFmt.format(now).uppercase(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                style = compactShadow
            )
        } else {
            ClockFace(now, 40.dp, style == 2)
        }
        if (d != null && showWeather) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(weatherIcon(d.code, d.isDay), fontSize = 20.sp)
                Text(
                    "${d.temp}°",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    style = compactShadow
                )
            }
        }
    }

    if (showPopup) {
        LeftPopup(
            d = d,
            aqi = aqi,
            onChangeCity = {
                showPopup = false
                showPicker = true
            },
            onClose = { showPopup = false }
        )
    }
    if (showPicker) {
        CityPicker(onClose = { showPicker = false })
    }
}

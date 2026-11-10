package com.vinay.tvlauncher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val textShadow = TextStyle(
    shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 8f)
)

// Hawa ki quality (AQI) - Open-Meteo se
suspend fun fetchAqi(city: City): Int? = withContext(Dispatchers.IO) {
    try {
        val u = "https://air-quality-api.open-meteo.com/v1/air-quality" +
            "?latitude=${city.lat}&longitude=${city.lon}&current=us_aqi"
        val con = URL(u).openConnection() as HttpURLConnection
        con.connectTimeout = 10000
        con.readTimeout = 10000
        try {
            val txt = con.inputStream.bufferedReader().use { it.readText() }
            JSONObject(txt).getJSONObject("current").getDouble("us_aqi").roundToInt()
        } finally {
            con.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}

// Left upar ka block: tareekh + ghadi + mausam line, seedha wallpaper par
// OK = bada popup, OK dabake rakho = ghadi ka style badlo
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LeftInfoBlock(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { ClockPrefs.load(ctx) }
    val style = ClockPrefs.style
    val ver = WeatherStore.version
    var data by remember { mutableStateOf<WeatherData?>(null) }
    var aqi by remember { mutableStateOf<Int?>(null) }
    var showPopup by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.04f)

    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1000)
        }
    }
    val timeFmt = remember { SimpleDateFormat("hh:mm", Locale.getDefault()) }
    val ampmFmt = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE d MMMM", Locale.getDefault()) }

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

    Column(
        modifier
            .graphicsLayer {
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .onFocusChanged { focused = it.isFocused }
            .then(if (focused) Modifier.glass(RoundedCornerShape(28.dp), true) else Modifier)
            .combinedClickable(
                onClick = { showPopup = true },
                onLongClick = { ClockPrefs.set(ctx, (style + 1) % 3) }
            )
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            dateFmt.format(now),
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            style = textShadow
        )
        if (style == 0) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    timeFmt.format(now),
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    style = textShadow
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    ampmFmt.format(now).uppercase(),
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 22.sp,
                    style = textShadow,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        } else {
            Spacer(Modifier.height(6.dp))
            ClockFace(now, 110.dp, style == 2)
            Spacer(Modifier.height(6.dp))
        }
        if (d != null && showWeather) {
            Text(
                "${weatherIcon(d.code, d.isDay)}  ${d.city} ${d.temp}°  " +
                    "${weatherText(d.code)}  ${d.low}°~${d.high}°",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                style = textShadow
            )
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

@Composable
private fun AqiChip(aqi: Int) {
    val (label, bg) = when {
        aqi <= 50 -> "Good" to Color(0xFF34C759)
        aqi <= 100 -> "Moderate" to Color(0xFFE6B800)
        aqi <= 150 -> "Unhealthy for some" to Color(0xFFFF9500)
        aqi <= 200 -> "Unhealthy" to Color(0xFFFF3B30)
        aqi <= 300 -> "Very unhealthy" to Color(0xFFAF52DE)
        else -> "Hazardous" to Color(0xFF8E1B3A)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            "AQI $aqi · $label",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LeftWeatherColumn(
    d: WeatherData?,
    aqi: Int?,
    onChangeCity: () -> Unit,
    modifier: Modifier
) {
    Column(modifier) {
        if (d == null) {
            Text(
                "Mausam load ho raha hai…",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        } else {
            Text(
                d.city.uppercase(),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "${d.temp}°",
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Light
                )
                Text(weatherIcon(d.code, d.isDay), fontSize = 32.sp)
                Text(weatherText(d.code), color = Color.White, fontSize = 18.sp)
            }
            Text(
                "H:${d.high}°  L:${d.low}°  ·  Humidity ${d.humidity}%  ·  Wind ${d.wind} km/h",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
            if (aqi != null) {
                Spacer(Modifier.height(8.dp))
                AqiChip(aqi)
            }
            Spacer(Modifier.height(14.dp))
            Text("Hourly", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                d.hours.forEach { h ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(h.label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(weatherIcon(h.code, true), fontSize = 20.sp)
                        Text("${h.temp}°", color = Color.White, fontSize = 15.sp)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Forecast", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            d.days.take(4).forEach { day ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        day.label,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.width(70.dp)
                    )
                    Text(weatherIcon(day.code, true), fontSize = 18.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("${day.min}° – ${day.max}°", color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${day.rain}%",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Open-Meteo · Updated ${d.updated}",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        GlassButton("Change City", onChangeCity, Modifier)
    }
}

@Composable
private fun TithiCalendar(year: Int, month: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    val now = remember { Calendar.getInstance() }
    val ty = now.get(Calendar.YEAR)
    val tm = now.get(Calendar.MONTH)
    val td = now.get(Calendar.DAY_OF_MONTH)
    val first = remember(year, month) {
        Calendar.getInstance().apply { clear(); set(year, month, 1) }
            .get(Calendar.DAY_OF_WEEK) - 1
    }
    val days = remember(year, month) {
        Calendar.getInstance().apply { clear(); set(year, month, 1) }
            .getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val tithis = remember(year, month) { (1..days).map { Tithi.forDay(year, month, it) } }
    val title = remember(year, month) {
        val c = Calendar.getInstance()
        c.clear()
        c.set(year, month, 1)
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(c.time)
    }
    val m1 = tithis.first().month
    val m2 = tithis.last().month
    val guMonths = if (m1 == m2) Tithi.monthName(m1)
    else Tithi.monthName(m1) + " – " + Tithi.monthName(m2)
    val rows = (first + days + 6) / 7
    val orange = Color(0xFFFF453A)
    val gold = Color(0xFFFFD60A)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassButton("◀", onPrev, Modifier)
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title.uppercase(),
                    color = orange,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(guMonths, color = gold, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            GlassButton("▶", onNext, Modifier)
        }
        Spacer(Modifier.height(6.dp))
        Row {
            for (dn in listOf("S", "M", "T", "W", "T", "F", "S")) {
                Box(Modifier.size(58.dp, 26.dp), contentAlignment = Alignment.Center) {
                    Text(dn, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }
        }
        for (r in 0 until rows) {
            Row {
                for (col in 0 until 7) {
                    val day = r * 7 + col - first + 1
                    Box(Modifier.size(58.dp, 44.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..days) {
                            val t = tithis[day - 1]
                            val isToday = year == ty && month == tm && day == td
                            val special = t.num == 15 || t.num == 30
                            Column(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isToday) orange else Color.Transparent)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "$day",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = if (isToday) FontWeight.SemiBold
                                    else FontWeight.Normal
                                )
                                Text(
                                    Tithi.label(t.num),
                                    color = if (isToday) Color.White
                                    else if (special) gold
                                    else Color.White.copy(alpha = 0.55f),
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftPopup(
    d: WeatherData?,
    aqi: Int?,
    onChangeCity: () -> Unit,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { first.requestFocus() }
    }
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1000)
        }
    }
    val style = ClockPrefs.style
    val today = remember { Calendar.getInstance() }
    var year by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(today.get(Calendar.MONTH)) }
    val todayTithi = remember {
        Tithi.forDay(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        )
    }
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

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
            Row(
                Modifier
                    .heightIn(max = 520.dp)
                    .glass(RoundedCornerShape(28.dp), false)
                    .padding(22.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                LeftWeatherColumn(
                    d = d,
                    aqi = aqi,
                    onChangeCity = onChangeCity,
                    modifier = Modifier
                        .width(380.dp)
                        .verticalScroll(rememberScrollState())
                )
                Column(
                    Modifier
                        .width(420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (style == 0) {
                            Text(
                                timeFmt.format(now),
                                color = Color.White,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Light
                            )
                        } else {
                            ClockFace(now, 100.dp, style == 2)
                        }
                        Column {
                            if (style != 0) {
                                Text(
                                    timeFmt.format(now),
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                dateFmt.format(now),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                            Text(
                                Tithi.full(todayTithi),
                                color = Color(0xFFFFD60A),
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton(
                            if (style == 0) "✓ Number" else "Number",
                            { ClockPrefs.set(ctx, 0) },
                            Modifier.focusRequester(first)
                        )
                        GlassButton(
                            if (style == 1) "✓ Gol" else "Gol",
                            { ClockPrefs.set(ctx, 1) },
                            Modifier
                        )
                        GlassButton(
                            if (style == 2) "✓ Chorus" else "Chorus",
                            { ClockPrefs.set(ctx, 2) },
                            Modifier
                        )
                    }
                          Spacer(Modifier.height(8.dp))
                    GlassButton("StandBy", { onClose(); StandByLink.open() }, Modifier)
                    Spacer(Modifier.height(10.dp))
                    TithiCalendar(
                        year = year,
                        month = month,
                        onPrev = {
                            if (month == 0) {
                                month = 11
                                year -= 1
                            } else {
                                month -= 1
                            }
                        },
                        onNext = {
                            if (month == 11) {
                                month = 0
                                year += 1
                            } else {
                                month += 1
                            }
                        }
                    )
                }
            }
        }
    }
}

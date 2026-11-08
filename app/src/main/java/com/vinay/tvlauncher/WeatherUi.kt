package com.vinay.tvlauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun PRow(
    title: String,
    sub: String = "",
    requester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier
    Column(
        base
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Color(0xFFF2F2F2) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            title,
            color = if (focused) Color.Black else Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
        if (sub.isNotEmpty()) {
            Text(
                sub,
                color = if (focused) Color(0xFF666666) else Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CityPicker(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf("list") }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<City>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    val first = remember(stage) { FocusRequester() }

    LaunchedEffect(stage) {
        delay(150)
        runCatching { first.requestFocus() }
    }

    fun pick(c: City) {
        WeatherStore.setCity(ctx, c)
        onClose()
    }

    fun doSearch() {
        if (query.isBlank()) return
        busy = true
        scope.launch {
            results = searchCities(query.trim())
            searched = true
            busy = false
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .width(440.dp)
                    .heightIn(max = 480.dp)
                    .glass(RoundedCornerShape(26.dp), false)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text(
                    "Choose City",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 8.dp)
                )
                if (stage == "list") {
                    PresetCities.forEachIndexed { i, c ->
                        PRow(c.name, c.sub, requester = if (i == 0) first else null) { pick(c) }
                    }
                    PRow("Search another city…", "Naam likh kar dhundo") { stage = "search" }
                } else {
                    Text(
                        "Sheher ka naam likho, phir keyboard me Search dabao",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(first)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    PRow(if (busy) "Dhundh raha hai…" else "Search") { doSearch() }
                    results.forEach { c -> PRow(c.name, c.sub) { pick(c) } }
                    if (searched && !busy && results.isEmpty()) {
                        Text(
                            "Koi sheher nahi mila",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeBar(day: DayW, gMin: Int, gMax: Int) {
    val span = (gMax - gMin).coerceAtLeast(1).toFloat()
    val s = ((day.min - gMin) / span).coerceIn(0f, 1f)
    val e = ((day.max - gMin) / span).coerceIn(0f, 1f)
    val len = (e - s).coerceAtLeast(0.05f)
    val rest = (1f - s - len).coerceAtLeast(0.001f)
    Row(
        Modifier
            .width(130.dp)
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.15f))
    ) {
        Spacer(Modifier.weight(s.coerceAtLeast(0.001f)))
        Box(
            Modifier
                .weight(len)
                .height(5.dp)
                .background(Color(0xFFFFB340))
        )
        Spacer(Modifier.weight(rest))
    }
}

@Composable
private fun WeatherPopup(d: WeatherData, onChangeCity: () -> Unit, onClose: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { first.requestFocus() }
    }
    val days = d.days.take(5)
    val gMin = days.minOfOrNull { it.min } ?: 0
    val gMax = days.maxOfOrNull { it.max } ?: 1

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
                    .width(500.dp)
                    .heightIn(max = 500.dp)
                    .glass(RoundedCornerShape(28.dp), false)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        d.city,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    GlassButton("Change City", onChangeCity, Modifier.focusRequester(first))
                }
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${d.temp}°",
                        color = Color.White,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        weatherIcon(d.code, d.isDay) + "  " + weatherText(d.code),
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Text(
                        "H:${d.high}°  L:${d.low}°",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp
                    )
                }

                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "HOURLY FORECAST",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Humidity ${d.humidity}% · Wind ${d.wind} km/h",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    d.hours.forEach { h ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(h.label, color = Color.White, fontSize = 13.sp)
                            Text(weatherIcon(h.code, true), fontSize = 22.sp)
                            Text("${h.temp}°", color = Color.White, fontSize = 15.sp)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "5-DAY FORECAST",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                days.forEach { day ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            day.label,
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.width(64.dp)
                        )
                        Column(
                            Modifier.width(54.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(weatherIcon(day.code, true), fontSize = 20.sp)
                            if (day.rain >= 30) {
                                Text("${day.rain}%", color = Color(0xFF64D2FF), fontSize = 11.sp)
                            }
                        }
                        Text(
                            "${day.min}°",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 15.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(40.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        RangeBar(day, gMin, gMax)
                        Spacer(Modifier.width(10.dp))
                        Text("${day.max}°", color = Color.White, fontSize = 15.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Open-Meteo · Updated ${d.updated}",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun WeatherPill(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val ver = WeatherStore.version
    var data by remember { mutableStateOf<WeatherData?>(null) }
    var showPopup by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.08f)

    LaunchedEffect(ver) {
        while (true) {
            val d = fetchWeather(WeatherStore.city(ctx))
            if (d != null) data = d
            delay(30 * 60 * 1000L)
        }
    }

    val d = data
    if (d != null) {
        Row(
            modifier
                .graphicsLayer { scaleX = s; scaleY = s }
                .onFocusChanged { focused = it.isFocused }
                .glass(RoundedCornerShape(50), focused)
                .clickable { showPopup = true }
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(weatherIcon(d.code, d.isDay), fontSize = 20.sp)
            Text("${d.temp}°", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Column {
                Text(weatherText(d.code), color = Color.White, fontSize = 12.sp, maxLines = 1)
                Text(d.city, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
            }
        }
    }

    if (showPopup && d != null) {
        WeatherPopup(
            d = d,
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

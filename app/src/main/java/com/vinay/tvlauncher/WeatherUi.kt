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
import androidx.compose.runtime.produceState
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
fun CityPicker(onClose: () -> Unit) {
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
private fun WeatherPopup(
    d: WeatherData,
    timeText: String,
    dateText: String,
    onChangeCity: () -> Unit,
    onClose: () -> Unit
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
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
                    .width(500.dp)
                    .heightIn(max = 500.dp)
                    .glass(RoundedCornerShape(28.dp), false)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            timeText,
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            dateText,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    GlassButton("Change City", onChangeCity, Modifier.focusRequester(first))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    d.city,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
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
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Humidity ${d.humidity}% · Wind ${d.wind} km/h",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(12.dp))
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
fun TimeWeatherPill(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val ver = WeatherStore.version
    var data by remember { mutableStateOf<WeatherData?>(null) }
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
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    val timeText = timeFmt.format(now)
    val dateText = dateFmt.format(now)

    LaunchedEffect(ver) {
        while (true) {
            val w = fetchWeather(WeatherStore.city(ctx))
            if (w != null) data = w
            delay(30 * 60 * 1000L)
        }
    }

    val d = data
    val showWeather = d != null && WeatherStore.enabled(ctx)

    Row(
        modifier
            .graphicsLayer { scaleX = s; scaleY = s }
            .onFocusChanged { focused = it.isFocused }
            .glass(RoundedCornerShape(50), focused)
            .clickable { if (showWeather) showPopup = true }
            .padding(horizontal = 26.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                timeText,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium
            )
            Text(dateText, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        if (d != null && showWeather) {
            Box(
                Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(Color.White.copy(alpha = 0.25f))
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(weatherIcon(d.code, d.isDay), fontSize = 20.sp)
                Text(
                    "${d.temp}°",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                Column {
                    Text(weatherText(d.code), color = Color.White, fontSize = 12.sp, maxLines = 1)
                    Text(
                        d.city,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }

    if (showPopup && d != null) {
        WeatherPopup(
            d = d,
            timeText = timeText,
            dateText = dateText,
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
fun WeatherPill(modifier: Modifier = Modifier) {
    TimeWeatherPill(modifier)
}

package com.vinay.tvlauncher

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class City(val name: String, val lat: Double, val lon: Double, val sub: String = "")

data class HourW(val label: String, val temp: Int, val code: Int)

data class DayW(val label: String, val min: Int, val max: Int, val code: Int, val rain: Int)

data class WeatherData(
    val city: String,
    val temp: Int,
    val code: Int,
    val isDay: Boolean,
    val high: Int,
    val low: Int,
    val humidity: Int,
    val wind: Int,
    val hours: List<HourW>,
    val days: List<DayW>,
    val updated: String
)

val PresetCities = listOf(
    City("Surat", 21.1702, 72.8311, "Gujarat"),
    City("Ahmedabad", 23.0225, 72.5714, "Gujarat"),
    City("Vadodara", 22.3072, 73.1812, "Gujarat"),
    City("Rajkot", 22.3039, 70.8022, "Gujarat"),
    City("Bhavnagar", 21.7645, 72.1519, "Gujarat"),
    City("Mumbai", 19.0760, 72.8777, "Maharashtra"),
    City("Pune", 18.5204, 73.8567, "Maharashtra"),
    City("Nagpur", 21.1458, 79.0882, "Maharashtra"),
    City("Delhi", 28.6139, 77.2090, "Delhi")
)

object WeatherStore {
    var version by mutableIntStateOf(0)

    private fun sp(ctx: Context) =
        ctx.getSharedPreferences("apple_home", Context.MODE_PRIVATE)

    fun city(ctx: Context): City {
        val p = sp(ctx)
        val name = p.getString("weather_city", null)
        val lat = p.getString("weather_lat", null)?.toDoubleOrNull()
        val lon = p.getString("weather_lon", null)?.toDoubleOrNull()
        return if (name != null && lat != null && lon != null) City(name, lat, lon)
        else PresetCities[0]
    }

    fun setCity(ctx: Context, c: City) {
        sp(ctx).edit()
            .putString("weather_city", c.name)
            .putString("weather_lat", c.lat.toString())
            .putString("weather_lon", c.lon.toString())
            .apply()
        version++
    }
}

private fun httpGet(u: String): String {
    val c = URL(u).openConnection() as HttpURLConnection
    c.connectTimeout = 10000
    c.readTimeout = 10000
    return try {
        c.inputStream.bufferedReader().use { it.readText() }
    } finally {
        c.disconnect()
    }
}

private fun hourLabel(time: String): String {
    val h = time.substring(11, 13).toInt()
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "$h12${if (h < 12) "AM" else "PM"}"
}

suspend fun fetchWeather(city: City): WeatherData? = withContext(Dispatchers.IO) {
    try {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${city.lat}&longitude=${city.lon}" +
            "&current=temperature_2m,relative_humidity_2m,weather_code,is_day,wind_speed_10m" +
            "&hourly=temperature_2m,weather_code" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&timezone=auto&forecast_days=6"
        val j = JSONObject(httpGet(url))
        val cur = j.getJSONObject("current")
        val hourly = j.getJSONObject("hourly")
        val daily = j.getJSONObject("daily")

        val ht = hourly.getJSONArray("time")
        val hT = hourly.getJSONArray("temperature_2m")
        val hC = hourly.getJSONArray("weather_code")
        val curHour = cur.getString("time").substring(0, 13)
        var start = 0
        for (i in 0 until ht.length()) {
            if (ht.getString(i).substring(0, 13) >= curHour) {
                start = i
                break
            }
        }
        val hours = (0 until 6).mapNotNull { k ->
            val i = start + k
            if (i < ht.length()) {
                HourW(
                    if (k == 0) "Now" else hourLabel(ht.getString(i)),
                    hT.getDouble(i).roundToInt(),
                    hC.getInt(i)
                )
            } else null
        }

        val dt = daily.getJSONArray("time")
        val dC = daily.getJSONArray("weather_code")
        val dMax = daily.getJSONArray("temperature_2m_max")
        val dMin = daily.getJSONArray("temperature_2m_min")
        val dRain = daily.getJSONArray("precipitation_probability_max")
        val parse = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
        val days = (0 until dt.length()).map { i ->
            val label = if (i == 0) "Today" else {
                runCatching { dayFmt.format(parse.parse(dt.getString(i))!!) }
                    .getOrDefault(dt.getString(i))
            }
            DayW(
                label,
                dMin.getDouble(i).roundToInt(),
                dMax.getDouble(i).roundToInt(),
                dC.getInt(i),
                if (dRain.isNull(i)) 0 else dRain.getInt(i)
            )
        }

        WeatherData(
            city = city.name,
            temp = cur.getDouble("temperature_2m").roundToInt(),
            code = cur.getInt("weather_code"),
            isDay = cur.getInt("is_day") == 1,
            high = days.firstOrNull()?.max ?: 0,
            low = days.firstOrNull()?.min ?: 0,
            humidity = cur.getInt("relative_humidity_2m"),
            wind = cur.getDouble("wind_speed_10m").roundToInt(),
            hours = hours,
            days = days,
            updated = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        )
    } catch (e: Exception) {
        null
    }
}

suspend fun searchCities(q: String): List<City> = withContext(Dispatchers.IO) {
    try {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=" +
            URLEncoder.encode(q, "UTF-8") + "&count=6&language=en&format=json"
        val j = JSONObject(httpGet(url))
        val arr = j.optJSONArray("results") ?: return@withContext emptyList<City>()
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            City(
                o.getString("name"),
                o.getDouble("latitude"),
                o.getDouble("longitude"),
                listOf(o.optString("admin1", ""), o.optString("country", ""))
                    .filter { it.isNotBlank() }.joinToString(", ")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

fun weatherIcon(code: Int, isDay: Boolean): String = when (code) {
    0 -> if (isDay) "☀️" else "🌙"
    1, 2 -> if (isDay) "⛅" else "☁️"
    3 -> "☁️"
    45, 48 -> "🌫️"
    51, 53, 55, 56, 57 -> "🌦️"
    61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️"
    71, 73, 75, 77, 85, 86 -> "❄️"
    95, 96, 99 -> "⛈️"
    else -> "☁️"
}

fun weatherText(code: Int): String = when (code) {
    0 -> "Clear"
    1, 2 -> "Partly Cloudy"
    3 -> "Cloudy"
    45, 48 -> "Fog"
    51, 53, 55, 56, 57 -> "Drizzle"
    61, 63, 65, 66, 67 -> "Rain"
    71, 73, 75, 77 -> "Snow"
    80, 81, 82 -> "Showers"
    85, 86 -> "Snow Showers"
    95, 96, 99 -> "Thunderstorm"
    else -> "Cloudy"
}
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

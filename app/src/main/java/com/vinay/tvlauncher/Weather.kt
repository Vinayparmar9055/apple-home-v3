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

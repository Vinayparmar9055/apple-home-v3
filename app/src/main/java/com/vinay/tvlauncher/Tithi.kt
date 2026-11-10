package com.vinay.tvlauncher

import java.util.Calendar
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

// num: 1..30 (1-15 sud, 16-29 vad, 15 = poonam, 30 = amas)
// month: 0..11 (0 = Chaitra ... 7 = Kartik ... 11 = Fagan)
data class TithiInfo(val num: Int, val month: Int, val samvat: Int)

object Tithi {
    private val months = listOf(
        "ચૈત્ર", "વૈશાખ", "જેઠ", "અષાઢ", "શ્રાવણ", "ભાદરવો",
        "આસો", "કારતક", "માગશર", "પોષ", "મહા", "ફાગણ"
    )
    private const val DIGITS = "૦૧૨૩૪૫૬૭૮૯"

    fun gu(n: Int): String = n.toString().map { DIGITS[it - '0'] }.joinToString("")

    fun label(n: Int): String = when {
        n == 15 -> "પૂનમ"
        n == 30 -> "અમાસ"
        n < 15 -> "સુદ " + gu(n)
        else -> "વદ " + gu(n - 15)
    }

    fun monthName(i: Int): String = months[((i % 12) + 12) % 12]

    // "કારતક સુદ ૧ · વિ.સં. ૨૦૮૩"
    fun full(t: TithiInfo): String =
        monthName(t.month) + " " + label(t.num) + " · વિ.સં. " + gu(t.samvat)

    private fun rad(x: Double) = x * PI / 180.0
    private fun norm(x: Double) = x - 360.0 * floor(x / 360.0)
    private fun jd(ms: Long) = ms / 86400000.0 + 2440587.5

    private fun sunLon(j: Double): Double {
        val t = (j - 2451545.0) / 36525.0
        val l0 = 280.46646 + 36000.76983 * t
        val m = rad(357.52911 + 35999.05029 * t)
        val c = (1.914602 - 0.004817 * t) * sin(m) +
            0.019993 * sin(2 * m) + 0.000289 * sin(3 * m)
        return norm(l0 + c)
    }

    private fun moonLon(j: Double): Double {
        val t = (j - 2451545.0) / 36525.0
        val lp = 218.3164477 + 481267.88123421 * t
        val d = rad(297.8501921 + 445267.1114034 * t)
        val ms = rad(357.5291092 + 35999.0502909 * t)
        val mp = rad(134.9633964 + 477198.8675055 * t)
        val f = rad(93.2720950 + 483202.0175233 * t)
        val l = lp +
            6.288774 * sin(mp) +
            1.274027 * sin(2 * d - mp) +
            0.658314 * sin(2 * d) +
            0.213618 * sin(2 * mp) -
            0.185116 * sin(ms) -
            0.114332 * sin(2 * f) +
            0.058793 * sin(2 * d - 2 * mp) +
            0.057066 * sin(2 * d - ms - mp) +
            0.053322 * sin(2 * d + mp) +
            0.045758 * sin(2 * d - ms) -
            0.040923 * sin(ms - mp) -
            0.034720 * sin(d) -
            0.030383 * sin(ms + mp) +
            0.015327 * sin(2 * d - 2 * f) -
            0.012528 * sin(mp + 2 * f) +
            0.010980 * sin(mp - 2 * f)
        return norm(l)
    }

    private fun elong(ms: Long): Double {
        val j = jd(ms)
        return norm(moonLon(j) - sunLon(j))
    }

    private fun ayanamsa(j: Double) = 23.853 + (j - 2451545.0) / 365.25 * 0.013972

    // month0: 0 = January. Tithi savere 6:30 baje ki li jati hai (panchang jaisi)
    fun forDay(year: Int, month0: Int, day: Int): TithiInfo {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month0, day, 6, 30, 0)
        val t = cal.timeInMillis
        val e = elong(t)
        val num = floor(e / 12.0).toInt() + 1

        // pichhli amas (new moon) dhundho
        var nm = t - (e / 12.19 * 86400000.0).toLong()
        repeat(4) {
            var diff = elong(nm)
            if (diff > 180.0) diff -= 360.0
            nm -= (diff / 12.19 * 86400000.0).toLong()
        }
        val j = jd(nm)
        val sid = norm(sunLon(j) - ayanamsa(j))
        val rashi = floor(sid / 30.0).toInt()
        val month = (rashi + 1) % 12

        // Gujarati saal Kartik sud 1 (Diwali ke baad) se badalta hai
        val samvat = year + 56 + (if (month0 >= 9 && month in 7..9) 1 else 0)
        return TithiInfo(num.coerceIn(1, 30), month, samvat)
    }
}

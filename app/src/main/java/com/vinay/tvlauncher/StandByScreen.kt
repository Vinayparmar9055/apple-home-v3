package com.vinay.tvlauncher

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StandByScreen(onExit: () -> Unit) {
    val ctx = LocalContext.current
    val style = remember { Prefs.standbyStyle(ctx) }
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1000)
        }
    }
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
        when (style) {
            0 -> Row(
                horizontalArrangement = Arrangement.spacedBy(70.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnalogueClock(now, 330.dp)
                CalendarWidget(now)
            }
            1 -> AnalogueClock(now, 420.dp)
            2 -> FloatClock(now)
            else -> DigitalClock(now)
        }
    }
}

@Composable
private fun DigitalClock(now: Date) {
    val timeFmt = remember { SimpleDateFormat("hh:mm", Locale.getDefault()) }
    val ampmFmt = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
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

@Composable
private fun FloatClock(now: Date) {
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    val t = rememberInfiniteTransition(label = "float")
    val px = t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(50000, easing = LinearEasing), RepeatMode.Restart),
        label = "px"
    )
    val py = t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(70000, easing = LinearEasing), RepeatMode.Restart),
        label = "py"
    )
    Column(
        Modifier.graphicsLayer {
            translationX = sin(px.value * 2f * PI.toFloat()) * 320f
            translationY = cos(py.value * 2f * PI.toFloat()) * 160f
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            timeFmt.format(now),
            color = Color.White,
            fontSize = 90.sp,
            fontWeight = FontWeight.Light
        )
        Text(dateFmt.format(now), color = Color.White.copy(alpha = 0.6f), fontSize = 22.sp)
    }
}

@Composable
private fun AnalogueClock(now: Date, dia: Dp) {
    Canvas(Modifier.size(dia)) {
        val c = center
        val r = this.size.minDimension / 2f

        for (i in 0 until 60) {
            val a = Math.toRadians(i * 6.0 - 90.0)
            val major = i % 5 == 0
            val len = if (major) r * 0.07f else r * 0.035f
            val start = Offset(
                c.x + cos(a).toFloat() * (r - len),
                c.y + sin(a).toFloat() * (r - len)
            )
            val end = Offset(c.x + cos(a).toFloat() * r, c.y + sin(a).toFloat() * r)
            drawLine(
                Color.White.copy(alpha = if (major) 1f else 0.6f),
                start, end,
                strokeWidth = if (major) 5f else 2.5f,
                cap = StrokeCap.Round
            )
        }

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = r * 0.17f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        for (n in 1..12) {
            val a = Math.toRadians(n * 30.0 - 90.0)
            val x = c.x + cos(a).toFloat() * r * 0.78f
            val y = c.y + sin(a).toFloat() * r * 0.78f + paint.textSize * 0.35f
            drawContext.canvas.nativeCanvas.drawText("$n", x, y, paint)
        }

        val cal = Calendar.getInstance().apply { time = now }
        val h = cal.get(Calendar.HOUR)
        val m = cal.get(Calendar.MINUTE)
        val s = cal.get(Calendar.SECOND)

        fun hand(deg: Float, len: Float, w: Float, color: Color) {
            val a = Math.toRadians(deg.toDouble())
            drawLine(
                color, c,
                Offset(c.x + cos(a).toFloat() * len, c.y + sin(a).toFloat() * len),
                strokeWidth = w, cap = StrokeCap.Round
            )
        }

        hand(h * 30f + m * 0.5f - 90f, r * 0.50f, 12f, Color.White)
        hand(m * 6f + s * 0.1f - 90f, r * 0.75f, 8f, Color.White)
        val yellow = Color(0xFFFFD60A)
        hand(s * 6f - 90f, r * 0.85f, 3.5f, yellow)
        hand(s * 6f + 90f, r * 0.15f, 3.5f, yellow)
        drawCircle(yellow, radius = 8f, center = c)
    }
}

@Composable
private fun CalendarWidget(now: Date) {
    val cal = Calendar.getInstance().apply { time = now }
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(now).uppercase()
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val first = Calendar.getInstance().apply {
        time = now
        set(Calendar.DAY_OF_MONTH, 1)
    }.get(Calendar.DAY_OF_WEEK) - 1
    val rows = (first + days + 6) / 7
    val orange = Color(0xFFFF453A)

    Column {
        Text(monthName, color = orange, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        Row {
            for (d in listOf("S", "M", "T", "W", "T", "F", "S")) {
                Box(Modifier.size(46.dp, 36.dp), contentAlignment = Alignment.Center) {
                    Text(d, color = Color.White.copy(alpha = 0.6f), fontSize = 17.sp)
                }
            }
        }
        for (r in 0 until rows) {
            Row {
                for (col in 0 until 7) {
                    val day = r * 7 + col - first + 1
                    Box(Modifier.size(46.dp, 40.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..days) {
                            if (day == today) {
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .background(orange, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$day",
                                        color = Color.White,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Text("$day", color = Color.White, fontSize = 19.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

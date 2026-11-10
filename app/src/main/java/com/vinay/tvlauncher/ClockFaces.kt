package com.vinay.tvlauncher

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import java.util.Calendar
import java.util.Date
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// Left block ki ghadi ka style: 0 = Number wali, 1 = Gol kate wali, 2 = Chorus kate wali
object ClockPrefs {
    private var loaded = false

    var style by mutableIntStateOf(0)
        private set

    private fun sp(ctx: Context) =
        ctx.getSharedPreferences("apple_home", Context.MODE_PRIVATE)

    fun load(ctx: Context) {
        if (!loaded) {
            style = sp(ctx).getInt("left_clock_style", 0)
            loaded = true
        }
    }

    fun set(ctx: Context, value: Int) {
        style = value
        loaded = true
        sp(ctx).edit().putInt("left_clock_style", value).apply()
    }
}

@Composable
fun ClockFace(now: Date, dia: Dp, square: Boolean) {
    val showNumbers = dia.value >= 150f
    Canvas(Modifier.size(dia)) {
        val c = center
        val r = this.size.minDimension / 2f
        val dark = Color.Black.copy(alpha = 0.35f)
        val edge = Color.White.copy(alpha = 0.35f)

        if (square) {
            val corner = CornerRadius(r * 0.30f, r * 0.30f)
            drawRoundRect(color = dark, size = this.size, cornerRadius = corner)
            drawRoundRect(
                color = edge,
                size = this.size,
                cornerRadius = corner,
                style = Stroke(width = 3f)
            )
        } else {
            drawCircle(dark, radius = r, center = c)
            drawCircle(edge, radius = r, center = c, style = Stroke(width = 3f))
        }

        // kone ki taraf square me door tak jaane ke liye
        fun pt(a: Double, rad: Float): Offset {
            val dx = cos(a).toFloat()
            val dy = sin(a).toFloat()
            val k = if (square) 1f / max(abs(dx), abs(dy)) else 1f
            return Offset(c.x + dx * rad * k, c.y + dy * rad * k)
        }

        val outer = if (square) r * 0.90f else r * 0.93f
        for (i in 0 until 60) {
            val a = Math.toRadians(i * 6.0 - 90.0)
            val major = i % 5 == 0
            val len = if (major) r * 0.08f else r * 0.04f
            drawLine(
                Color.White.copy(alpha = if (major) 1f else 0.55f),
                pt(a, outer - len),
                pt(a, outer),
                strokeWidth = if (major) r * 0.035f else r * 0.018f,
                cap = StrokeCap.Round
            )
        }

        if (showNumbers) {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = r * 0.16f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            val numR = if (square) r * 0.66f else r * 0.76f
            for (n in 1..12) {
                val a = Math.toRadians(n * 30.0 - 90.0)
                val p = pt(a, numR)
                drawContext.canvas.nativeCanvas.drawText(
                    "$n", p.x, p.y + paint.textSize * 0.35f, paint
                )
            }
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

        hand(h * 30f + m * 0.5f - 90f, r * 0.48f, r * 0.075f, Color.White)
        hand(m * 6f + s * 0.1f - 90f, r * 0.72f, r * 0.055f, Color.White)
        val yellow = Color(0xFFFFD60A)
        hand(s * 6f - 90f, r * 0.80f, r * 0.025f, yellow)
        hand(s * 6f + 90f, r * 0.14f, r * 0.025f, yellow)
        drawCircle(yellow, radius = r * 0.04f, center = c)
    }
}

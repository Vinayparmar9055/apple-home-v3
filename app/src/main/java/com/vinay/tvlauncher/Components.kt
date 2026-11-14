package com.vinay.tvlauncher

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

val Navy = Color(0xFF0A0F1D)

// Dark mode ka asar glass par: MainActivity ye value badalti hai
object ThemeState {
    var dark by mutableStateOf(false)
}

// Glass Style (Liquid / Frosted), Blur Value, Dark Mode aur (agar ON ho to) ghumne wala rang
// ka asar yahin se sab panels par aata hai
fun Modifier.glass(shape: Shape, focused: Boolean): Modifier = composed {
    val ctx = LocalContext.current
    val t = Prefs.tick
    val style = remember(t) { Prefs.glassStyle(ctx) }
    val blur = remember(t) { Prefs.blur(ctx) }
    // Shimmer pehle se BAND hai; Desktop > Appearance se ON kar sakte ho
    val shimmerOn = remember(t) { Prefs.shimmer(ctx) }
    val dk = ThemeState.dark
    val frosted = style == 1

    val tr = rememberInfiniteTransition(label = "glassShimmer")
    val phase = tr.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(16000, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "phase"
    )

    val a0 = if (frosted) 0.35f + blur * 0.04f else 0.62f + blur * 0.03f
    val a = if (dk) a0 + 0.12f else a0
    val boost = if (focused) 0.10f else 0f
    val aTop = (a + boost).coerceAtMost(0.97f)
    val aBot = (a + 0.08f + boost).coerceAtMost(0.97f)

    val top = if (frosted) {
        if (dk) {
            if (focused) Color(0xFF70757F) else Color(0xFF4E525A)
        } else {
            if (focused) Color(0xFFB5BBC6) else Color(0xFF9AA1AD)
        }
    } else {
        if (dk) {
            if (focused) Color(0xFF3A3A40) else Color(0xFF1C1C20)
        } else {
            if (focused) Color(0xFF5A5A62) else Color(0xFF3A3A40)
        }
    }
    val bot = if (frosted) {
        if (dk) {
            if (focused) Color(0xFF4A4E56) else Color(0xFF30333A)
        } else {
            if (focused) Color(0xFF858B96) else Color(0xFF6B717C)
        }
    } else {
        if (dk) {
            if (focused) Color(0xFF121214) else Color(0xFF060608)
        } else {
            if (focused) Color(0xFF222226) else Color(0xFF131316)
        }
    }

    this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(top.copy(alpha = aTop), bot.copy(alpha = aBot))
            )
        )
        .background(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = if (focused) 0.16f else 0.10f),
                0.5f to Color.Transparent,
                1f to Color.Transparent
            )
        )
        .drawBehind {
            if (shimmerOn) {
                val p = phase.value
                val w = size.width
                val h = size.height
                val r = max(w, h) * 0.6f
                val c1 = Offset((-0.2f + 1.4f * p) * w, h * 0.35f)
                val c2 = Offset((1.2f - 1.4f * p) * w, h * 0.75f)
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0x383B82F6), Color.Transparent), c1, r
                    ),
                    r, c1
                )
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0x30A855F7), Color.Transparent), c2, r
                    ),
                    r, c2
                )
            }
        }
        .border(
            if (focused) 2.dp else 1.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (focused) 0.95f else 0.22f),
                    Color.White.copy(alpha = if (focused) 0.70f else 0.04f)
                )
            ),
            shape
        )
}

@Composable
fun focusScale(focused: Boolean, target: Float): Float =
    animateFloatAsState(
        targetValue = if (focused) target else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "focusScale"
    ).value

@Composable
fun GlassButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.08f)
    Box(
        modifier
            .graphicsLayer { scaleX = s; scaleY = s }
            .onFocusChanged { focused = it.isFocused }
            .glass(RoundedCornerShape(50), focused)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AppTile(
    item: AppItem,
    onClick: () -> Unit,
    requester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.22f)
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier
    Column(
        base
            .width(140.dp)
            .graphicsLayer { scaleX = s; scaleY = s }
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(112.dp)
                .glass(RoundedCornerShape(26.dp), focused),
            contentAlignment = Alignment.Center
        ) {
            Image(item.icon, contentDescription = item.label, modifier = Modifier.size(76.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            item.label,
            color = Color.White.copy(alpha = if (focused) 1f else 0.7f),
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun ActionTile(emoji: String, title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.06f)
    Column(
        modifier
            .graphicsLayer { scaleX = s; scaleY = s }
            .onFocusChanged { focused = it.isFocused }
            .glass(RoundedCornerShape(24.dp), focused)
            .clickable(onClick = onClick)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 36.sp)
        Spacer(Modifier.height(8.dp))
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

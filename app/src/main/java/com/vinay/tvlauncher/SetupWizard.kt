package com.vinay.tvlauncher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private fun wizOpen(ctx: Context, intent: Intent) {
    try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(ctx, "Ye option is TV par nahi khul paya", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun WRow(
    title: String,
    sub: String? = null,
    checked: Boolean = false,
    requester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier
    Row(
        base
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (focused) Color(0xFFF2F2F2) else Color.White.copy(alpha = 0.10f))
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (focused) Color.Black else Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium
            )
            if (sub != null) {
                Text(
                    sub,
                    color = if (focused) Color(0xFF666666) else Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp
                )
            }
        }
        if (checked) {
            Text("✓", color = Color(0xFF30D158), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WHeader(title: String, sub: String) {
    Text(
        title,
        color = Color.White,
        fontSize = 36.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        sub,
        color = Color.White.copy(alpha = 0.65f),
        fontSize = 15.sp,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
fun SetupWizard(
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onFinish: () -> Unit
) {
    val ctx = LocalContext.current
    var step by remember { mutableStateOf(0) }
    var glass by remember { mutableStateOf(Prefs.glassStyle(ctx)) }
    var iconStyle by remember { mutableStateOf(Prefs.iconStyle(ctx)) }
    var dock by remember { mutableStateOf(Prefs.dockCount(ctx)) }
    var weatherOn by remember { mutableStateOf(WeatherStore.enabled(ctx)) }
    var showPicker by remember { mutableStateOf(false) }
    val wver = WeatherStore.version
    val cityName = remember(wver) { WeatherStore.city(ctx).name }
    val total = 7

    BackHandler {
        if (step == 0) onFinish() else step -= 1
    }

    val first = remember(step) { FocusRequester() }
    LaunchedEffect(step, showPicker) {
        if (!showPicker) {
            delay(60)
            runCatching { first.requestFocus() }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.width(560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (step) {
                0 -> {
                    WHeader("Welcome", "Welcome to tvOS Launcher for your TV.")
                    WRow("Get Started", "Start setup wizard", requester = first) { step = 1 }
                }

                1 -> {
                    WHeader(
                        "Look & Style",
                        "Choose the glass aesthetic and dark mode for menus and panels."
                    )
                    WRow(
                        "Liquid Glass", "Translucent, refractive fluid glass effect",
                        checked = glass == 0, requester = first
                    ) {
                        glass = 0
                        Prefs.setGlassStyle(ctx, 0)
                    }
                    WRow("Frosted Glass", "Classic soft blurred surface", checked = glass == 1) {
                        glass = 1
                        Prefs.setGlassStyle(ctx, 1)
                    }
                    WRow("Dark Mode", "Deep black surfaces and dark styling", checked = dark) {
                        onDarkChange(!dark)
                    }
                    WRow("Continue") { step = 2 }
                }

                2 -> {
                    WHeader(
                        "Icon Style",
                        "Choose the visual style for your app tiles."
                    )
                    WRow(
                        "tvOS Icons (Default)",
                        "Full-bleed brand colors, centered logos, and glass shine",
                        checked = iconStyle == 0, requester = first
                    ) {
                        iconStyle = 0
                        Prefs.setIconStyle(ctx, 0)
                    }
                    WRow(
                        "Original Banners",
                        "Use each app's standard Android TV banner artwork",
                        checked = iconStyle == 1
                    ) {
                        iconStyle = 1
                        Prefs.setIconStyle(ctx, 1)
                    }
                    WRow("Continue") { step = 3 }
                }

                3 -> {
                    WHeader(
                        "Favourites Dock",
                        "Choose how many favourite app icons appear in the dock row."
                    )
                    WRow(
                        "6 App Icons (Default)", "Show 6 favourite apps",
                        checked = dock == 6, requester = first
                    ) {
                        dock = 6
                        Prefs.setDockCount(ctx, 6)
                    }
                    WRow("5 App Icons", "Show 5 favourite apps", checked = dock == 5) {
                        dock = 5
                        Prefs.setDockCount(ctx, 5)
                    }
                    WRow("4 App Icons", "Show 4 favourite apps", checked = dock == 4) {
                        dock = 4
                        Prefs.setDockCount(ctx, 4)
                    }
                    WRow("Continue") { step = 4 }
                }

                4 -> {
                    WHeader(
                        "Weather",
                        "A live weather pill on the home screen with conditions and temperature."
                    )
                    WRow(
                        "$cityName (Default)", "Show weather for this city",
                        checked = weatherOn, requester = first
                    ) {
                        WeatherStore.setEnabled(ctx, true)
                        weatherOn = true
                    }
                    WRow("Choose Another City…", "Search and select any city") {
                        showPicker = true
                    }
                    WRow(
                        "Don't Show Weather", "Hide weather pill from the home screen",
                        checked = !weatherOn
                    ) {
                        WeatherStore.setEnabled(ctx, false)
                        weatherOn = false
                    }
                    WRow("Continue") { step = 5 }
                }

                5 -> {
                    WHeader(
                        "Default Launcher & Accessibility",
                        "Set tvOS as your primary TV Home screen and turn on Accessibility to catch remote buttons."
                    )
                    WRow(
                        "Make tvOS the Home Screen", "Set tvOS as default launcher",
                        requester = first
                    ) { wizOpen(ctx, Intent(Settings.ACTION_HOME_SETTINGS)) }
                    WRow(
                        "Turn On Accessibility",
                        "Required for Home button catching & button remapping"
                    ) { wizOpen(ctx, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    WRow("Continue") { step = 6 }
                }

                else -> {
                    Box(
                        Modifier
                            .size(76.dp)
                            .background(Color(0xFF30D158), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    WHeader(
                        "You're All Set",
                        "Setup is complete. You can change any of these settings anytime or run setup again from Settings."
                    )
                    WRow("Go to Home Screen", requester = first) { onFinish() }
                }
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .size(7.dp)
                        .background(
                            if (i == step) Color.White else Color.White.copy(alpha = 0.3f),
                            CircleShape
                        )
                )
            }
        }
    }

    if (showPicker) {
        CityPicker(onClose = {
            showPicker = false
            WeatherStore.setEnabled(ctx, true)
            weatherOn = true
        })
    }
}

package com.vinay.tvlauncher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val StandByNames = listOf("Two Widgets", "Analogue", "Float", "Digital")

@Composable
private fun SRow(
    title: String,
    value: String = "",
    checked: Boolean = false,
    requester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier
    Row(
        base
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.08f))
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = if (focused) Color.Black else Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        if (value.isNotEmpty()) {
            Text(
                value,
                color = if (focused) Color(0xFF555555) else Color.White.copy(alpha = 0.6f),
                fontSize = 15.sp
            )
        }
        if (checked) {
            Text(
                "✓",
                color = if (focused) Color.Black else Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BlurRow(value: Int, requester: FocusRequester, onChange: (Int) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier
            .focusRequester(requester)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.08f))
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown) {
                    when (e.key) {
                        Key.DirectionLeft -> { onChange((value - 1).coerceAtLeast(0)); true }
                        Key.DirectionRight -> { onChange((value + 1).coerceAtMost(10)); true }
                        else -> false
                    }
                } else false
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable { }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Blur Value",
                color = if (focused) Color.Black else Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                "$value",
                color = if (focused) Color(0xFF555555) else Color.White.copy(alpha = 0.6f),
                fontSize = 15.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (focused) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.15f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(value / 10f)
                    .height(4.dp)
                    .background(if (focused) Color.Black else Color.White)
            )
        }
    }
}

@Composable
private fun SLabel(text: String) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.55f),
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 6.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingsPanel(
    apps: List<AppItem>,
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onAndroidSettings: () -> Unit,
    onRunSetup: () -> Unit,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    var page by remember { mutableStateOf("main") }
    var showRemap by remember { mutableStateOf(false) }
    var glass by remember { mutableStateOf(Prefs.glassStyle(ctx)) }
    var blur by remember { mutableStateOf(Prefs.blur(ctx)) }
    var standby by remember { mutableStateOf(Prefs.standbyStyle(ctx)) }

    BackHandler {
        when (page) {
            "main" -> onClose()
            "glass", "standby" -> page = "appearance"
            else -> page = "main"
        }
    }

    val first = remember(page) { FocusRequester() }
    LaunchedEffect(page, showRemap) {
        if (!showRemap) {
            delay(60)
            runCatching { first.requestFocus() }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            Modifier
                .padding(end = 32.dp, top = 24.dp, bottom = 24.dp)
                .width(440.dp)
                .fillMaxHeight()
                .glass(RoundedCornerShape(28.dp), false)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            val title = when (page) {
                "main" -> "Settings"
                "appearance" -> "Appearance"
                "glass" -> "Glass Style"
                else -> "StandBy Style"
            }
            Text(
                title,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(6.dp))

            when (page) {
                "main" -> {
                    SLabel("Launcher")
                    SRow("Appearance", "›", requester = first) { page = "appearance" }
                    Spacer(Modifier.height(8.dp))
                    SRow("Button Remap", "›") { showRemap = true }
                    Spacer(Modifier.height(8.dp))
                    SRow("Run Setup Again", "›") { onRunSetup() }
                    Spacer(Modifier.height(8.dp))
                    SLabel("TV")
                    SRow("Android Settings", "›") { onAndroidSettings() }
                }

                "appearance" -> {
                    SLabel("Style")
                    SRow(
                        "Glass Style",
                        if (glass == 0) "Liquid Glass ›" else "Frosted Glass ›",
                        requester = first
                    ) { page = "glass" }
                    Spacer(Modifier.height(8.dp))
                    SRow("Icon Pack", "tvOS ›") { }
                    Spacer(Modifier.height(8.dp))
                    SRow("Dark Mode", if (dark) "On" else "Off") { onDarkChange(!dark) }
                    SLabel("StandBy")
                    SRow("Style", StandByNames[standby] + " ›") { page = "standby" }
                }

                "glass" -> {
                    SLabel("Glass Style")
                    SRow("Liquid Glass", checked = glass == 0, requester = first) {
                        glass = 0
                        Prefs.setGlassStyle(ctx, 0)
                    }
                    Spacer(Modifier.height(8.dp))
                    SRow("Frosted Glass", checked = glass == 1) {
                        glass = 1
                        Prefs.setGlassStyle(ctx, 1)
                    }
                    SLabel("Blur")
                    BlurRow(blur, remember { FocusRequester() }) {
                        blur = it
                        Prefs.setBlur(ctx, it)
                    }
                }

                else -> {
                    StandByNames.forEachIndexed { i, name ->
                        SRow(
                            name,
                            checked = standby == i,
                            requester = if (i == 0) first else null
                        ) {
                            standby = i
                            Prefs.setStandbyStyle(ctx, i)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showRemap) {
        ButtonRemapScreen(apps = apps, onClose = { showRemap = false })
    }
}

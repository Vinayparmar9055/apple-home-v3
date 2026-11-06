package com.vinay.tvlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class Screen { HOME, SETTINGS, CONTROL }

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)
    private var homeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addOnNewIntentListener { homeTick++ }
        setContent { LauncherApp(resumeTick, homeTick) }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

@Composable
fun LauncherApp(resumeTick: Int, homeTick: Int) {
    val ctx = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }

    LaunchedEffect(resumeTick) {
        apps = withContext(Dispatchers.IO) { loadApps(ctx) }
    }
    LaunchedEffect(homeTick) { screen = Screen.HOME }

    BackHandler { if (screen != Screen.HOME) screen = Screen.HOME }

    Box(
        Modifier
            .fillMaxSize()
            .background(Navy)
            .drawBehind {
                val r = size.width * 0.45f
                val c1 = Offset(size.width * 0.15f, size.height * 0.1f)
                val c2 = Offset(size.width * 0.9f, size.height * 0.95f)
                drawCircle(
                    Brush.radialGradient(listOf(Color(0x553B82F6), Color.Transparent), c1, r),
                    r, c1
                )
                drawCircle(
                    Brush.radialGradient(listOf(Color(0x44A855F7), Color.Transparent), c2, r),
                    r, c2
                )
            }
    ) {
        when (screen) {
            Screen.HOME -> HomeScreen(
                apps = apps,
                onSettings = { screen = Screen.SETTINGS },
                onControl = { screen = Screen.CONTROL },
                onLaunch = { launchApp(ctx, it) }
            )
            Screen.SETTINGS -> ActionScreen(
                "Settings", settingsActions, 3
            ) { screen = Screen.HOME }
            Screen.CONTROL -> ActionScreen(
                "Control Center", controlActions, 3
            ) { screen = Screen.HOME }
        }
    }
}

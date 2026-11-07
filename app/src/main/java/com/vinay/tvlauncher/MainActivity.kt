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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class Screen { HOME, SETTINGS, CONTROL, APPS, STANDBY, ANDROID, RECENTS }

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)
    private var homeTick by mutableIntStateOf(0)
    private var recentsTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addOnNewIntentListener { i ->
            if (i.getBooleanExtra("recents", false)) recentsTick++ else homeTick++
        }
        if (intent?.getBooleanExtra("recents", false) == true) recentsTick++
        setContent { LauncherApp(resumeTick, homeTick, recentsTick) }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

@Composable
fun LauncherApp(resumeTick: Int, homeTick: Int, recentsTick: Int) {
    val ctx = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var favs by remember { mutableStateOf(Prefs.favourites(ctx)) }
    var dark by remember { mutableStateOf(Prefs.dark(ctx)) }
    var setupDone by remember { mutableStateOf(Prefs.setupDone(ctx)) }
    var dockCount by remember { mutableStateOf(Prefs.dockCount(ctx)) }
    var wallTick by remember { mutableStateOf(0) }
    val effectiveFavs = favs ?: defaultFavs(apps)

    val bmps by produceState<WallBitmaps?>(initialValue = null, wallTick) {
        value = withContext(Dispatchers.IO) { Wallpaper.loadBitmaps(ctx) }
    }
    val wallSettings = remember(wallTick) { Wallpaper.settings(ctx) }

    LaunchedEffect(resumeTick) {
        apps = withContext(Dispatchers.IO) { loadApps(ctx) }
    }
    LaunchedEffect(homeTick) { screen = Screen.HOME }
    LaunchedEffect(recentsTick) {
        if (recentsTick > 0) screen = Screen.RECENTS
    }

    BackHandler {
        when (screen) {
            Screen.HOME -> {}
            Screen.ANDROID -> screen = Screen.SETTINGS
            else -> screen = Screen.HOME
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(if (dark) Color(0xFF050507) else Navy)
            .drawBehind {
                if (!dark) {
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
            }
    ) {
        val b = bmps
        if (b != null && setupDone) {
            val showSharp = screen == Screen.HOME
            WallpaperLayer(
                sharp = b.sharp,
                blurBg = b.blur,
                s = wallSettings,
                blurred = !showSharp,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (showSharp) 0.30f else 0.45f))
            )
        }

        if (!setupDone) {
            SetupWizard(
                dark = dark,
                onDarkChange = {
                    dark = it
                    Prefs.setDark(ctx, it)
                },
                onFinish = {
                    Prefs.setSetupDone(ctx, true)
                    dockCount = Prefs.dockCount(ctx)
                    setupDone = true
                    screen = Screen.HOME
                }
            )
        } else {
            when (screen) {
                Screen.HOME, Screen.CONTROL, Screen.SETTINGS -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .focusProperties { canFocus = screen != Screen.SETTINGS }
                    ) {
                        HomeScreen(
                            apps = apps,
                            favPkgs = effectiveFavs.take(dockCount),
                            onSettings = { screen = Screen.SETTINGS },
                            onControl = { screen = Screen.CONTROL },
                            onAllApps = { screen = Screen.APPS },
                            onLaunch = { launchApp(ctx, it) }
                        )
                    }
                    if (screen == Screen.CONTROL) {
                        ControlCenter(
                            dark = dark,
                            onToggleDark = {
                                dark = !dark
                                Prefs.setDark(ctx, dark)
                            },
                            onStandBy = { screen = Screen.STANDBY },
                            onSettings = { screen = Screen.SETTINGS },
                            onClose = { screen = Screen.HOME }
                        )
                    }
                    if (screen == Screen.SETTINGS) {
                        SettingsPanel(
                            apps = apps,
                            dark = dark,
                            onDarkChange = {
                                dark = it
                                Prefs.setDark(ctx, it)
                            },
                            onAndroidSettings = { screen = Screen.ANDROID },
                            onRunSetup = {
                                Prefs.setSetupDone(ctx, false)
                                setupDone = false
                                screen = Screen.HOME
                            },
                            onWallpaperChanged = { wallTick++ },
                            onClose = { screen = Screen.HOME }
                        )
                    }
                }
                Screen.APPS -> AppsScreen(
                    apps = apps,
                    favPkgs = effectiveFavs,
                    onFavsChange = {
                        favs = it
                        Prefs.setFavourites(ctx, it)
                    },
                    onLaunch = { launchApp(ctx, it) },
                    onBack = { screen = Screen.HOME }
                )
                Screen.ANDROID -> ActionScreen(
                    "Android Settings", settingsActions, 3
                ) { screen = Screen.SETTINGS }
                Screen.STANDBY -> StandByScreen { screen = Screen.HOME }
                Screen.RECENTS -> RecentAppsScreen(
                    apps = apps,
                    onLaunch = { launchApp(ctx, it) },
                    onBack = { screen = Screen.HOME }
                )
            }
        }
    }
}

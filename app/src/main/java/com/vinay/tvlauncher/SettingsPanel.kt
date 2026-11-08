package com.vinay.tvlauncher

import android.widget.Toast
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val StandByNames = listOf("Two Widgets", "Analogue", "Float", "Digital")
private val IconPackNames =
    listOf("tvOS Icons", "Original Banners", "Rounded", "Circle", "Black & White")
private val IconSizeNames = listOf("Small", "Medium", "Large")
private val CornerNames = listOf("Sharp", "Normal", "Round")
private val IntervalOptions = listOf(1, 3, 5, 15)

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
                "Blur Value  ◀ ▶",
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
    onWallpaperChanged: () -> Unit,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf("main") }
    var showRemap by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }
    var glass by remember { mutableStateOf(Prefs.glassStyle(ctx)) }
    var blur by remember { mutableStateOf(Prefs.blur(ctx)) }
    var standby by remember { mutableStateOf(Prefs.standbyStyle(ctx)) }
    var wallCount by remember { mutableStateOf(Wallpaper.count(ctx)) }
    var slide by remember { mutableStateOf(Wallpaper.slideshowOn(ctx)) }
    var interval by remember { mutableStateOf(Wallpaper.intervalMin(ctx)) }
    var iconPack by remember { mutableStateOf(Prefs.iconStyle(ctx)) }
    var iconSize by remember { mutableStateOf(Prefs.iconSize(ctx)) }
    var gridCols by remember { mutableStateOf(Prefs.gridCols(ctx)) }
    var dockCount by remember { mutableStateOf(Prefs.dockCount(ctx)) }
    var corner by remember { mutableStateOf(Prefs.tileCorner(ctx)) }
    var labels by remember { mutableStateOf(Prefs.showLabels(ctx)) }
    var topShelf by remember { mutableStateOf(Prefs.topShelf(ctx)) }

    BackHandler {
        when (page) {
            "main" -> onClose()
            "glass", "standby", "wallpaper", "iconpack" -> page = "appearance"
            else -> page = "main"
        }
    }

    val first = remember(page) { FocusRequester() }
    LaunchedEffect(page, showRemap, showEditor, showGallery) {
        if (!showRemap && !showEditor && !showGallery) {
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
                "wallpaper" -> "Wallpaper"
                "iconpack" -> "Icon Pack"
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
                    SRow(
                        "Wallpaper",
                        when {
                            wallCount == 0 -> "Default ›"
                            wallCount == 1 -> "Custom ›"
                            else -> "$wallCount photos ›"
                        }
                    ) { page = "wallpaper" }
                    Spacer(Modifier.height(8.dp))
                    SRow("Dark Mode", if (dark) "On" else "Off") { onDarkChange(!dark) }

                    SLabel("Home")
                    SRow("Top Shelf", if (topShelf) "On" else "Off") {
                        topShelf = !topShelf
                        Prefs.setTopShelf(ctx, topShelf)
                    }

                    SLabel("Icons")
                    SRow("Icon Pack", IconPackNames[iconPack.coerceIn(0, 4)] + " ›") {
                        page = "iconpack"
                    }
                    Spacer(Modifier.height(8.dp))
                    SRow("Icon Size", IconSizeNames[iconSize.coerceIn(0, 2)]) {
                        iconSize = (iconSize + 1) % 3
                        Prefs.setIconSize(ctx, iconSize)
                    }
                    Spacer(Modifier.height(8.dp))
                    SRow("Icons Per Row (All Apps)", "$gridCols") {
                        gridCols = if (gridCols >= 6) 4 else gridCols + 1
                        Prefs.setGridCols(ctx, gridCols)
                    }
                    Spacer(Modifier.height(8.dp))
                    SRow("Favourites Max", "$dockCount") {
                        dockCount = if (dockCount >= 6) 4 else dockCount + 1
                        Prefs.setDockCount(ctx, dockCount)
                    }
                    Spacer(Modifier.height(8.dp))
                    SRow("Tile Corners", CornerNames[corner.coerceIn(0, 2)]) {
                        corner = (corner + 1) % 3
                        Prefs.setTileCorner(ctx, corner)
                    }
                    Spacer(Modifier.height(8.dp))
                    SRow("App Names", if (labels) "Always" else "On Focus") {
                        labels = !labels
                        Prefs.setShowLabels(ctx, labels)
                    }

                    SLabel("StandBy")
                    SRow("Style", StandByNames[standby] + " ›") { page = "standby" }
                }

                "iconpack" -> {
                    IconPackNames.forEachIndexed { i, name ->
                        SRow(
                            name,
                            checked = iconPack == i,
                            requester = if (i == 0) first else null
                        ) {
                            iconPack = i
                            Prefs.setIconStyle(ctx, i)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
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
                        onWallpaperChanged()
                    }
                }

                "wallpaper" -> {
                    SLabel("Photos (max ${Wallpaper.MAX})")
                    SRow("Add Photo", "$wallCount/${Wallpaper.MAX}", requester = first) {
                        if (wallCount >= Wallpaper.MAX) {
                            Toast.makeText(
                                ctx,
                                "${Wallpaper.MAX} se zyada photo nahi jod sakte",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            showGallery = true
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SRow("Adjust Photo", "Rotate, Zoom ›") {
                        if (wallCount > 0) {
                            showEditor = true
                        } else {
                            Toast.makeText(ctx, "Pehle photo jodo", Toast.LENGTH_SHORT).show()
                        }
                    }

                    SLabel("Slideshow")
                    SRow("Slideshow", if (slide) "On" else "Off") {
                        slide = !slide
                        Wallpaper.setSlideshow(ctx, slide)
                        if (slide && wallCount < 2) {
                            Toast.makeText(
                                ctx,
                                "Slideshow ke liye kam se kam 2 photo chahiye",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        onWallpaperChanged()
                    }
                    Spacer(Modifier.height(8.dp))
                    SRow("Change Every", "$interval min") {
                        val i = IntervalOptions.indexOf(interval)
                        interval = IntervalOptions[(i + 1) % IntervalOptions.size]
                        Wallpaper.setIntervalMin(ctx, interval)
                        onWallpaperChanged()
                    }
                    if (wallCount > 1) {
                        Spacer(Modifier.height(8.dp))
                        SRow("Next Photo") {
                            Wallpaper.next(ctx)
                            onWallpaperChanged()
                        }
                    }

                    SLabel("Hatao")
                    SRow("Remove Current Photo", if (wallCount > 0) "" else "Koi nahi") {
                        if (wallCount > 0) {
                            Wallpaper.removeCurrent(ctx)
                            wallCount = Wallpaper.count(ctx)
                            onWallpaperChanged()
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SRow("Remove All Photos", if (wallCount > 0) "" else "Koi nahi") {
                        if (wallCount > 0) {
                            Wallpaper.clear(ctx)
                            wallCount = 0
                            onWallpaperChanged()
                        }
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
    if (showGallery) {
        GalleryPicker(
            onPick = { uri ->
                showGallery = false
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { Wallpaper.add(ctx, uri) }
                    if (ok) {
                        wallCount = Wallpaper.count(ctx)
                        onWallpaperChanged()
                        if (wallCount == 1) showEditor = true
                    } else {
                        Toast.makeText(ctx, "Ye photo load nahi ho payi", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onClose = { showGallery = false }
        )
    }
    if (showEditor) {
        WallpaperEditor(
            onChanged = { onWallpaperChanged() },
            onClose = { showEditor = false }
        )
    }
}

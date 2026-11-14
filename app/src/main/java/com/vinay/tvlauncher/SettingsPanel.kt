package com.vinay.tvlauncher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
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
    var showProfileGallery by remember { mutableStateOf(false) }
    var showNameEdit by remember { mutableStateOf(false) }
    var hasProfile by remember { mutableStateOf(ProfilePhoto.exists(ctx)) }
    var profileName by remember { mutableStateOf(loadProfileName(ctx)) }
    var glass by remember { mutableStateOf(Prefs.glassStyle(ctx)) }
    var blur by remember { mutableStateOf(Prefs.blur(ctx)) }
    var standby by remember { mutableStateOf(Prefs.standbyStyle(ctx)) }
    var wallCount by remember { mutableStateOf(Wallpaper.count(ctx)) }
    var slide by remember { mutableStateOf(Wallpaper.slideshowOn(ctx)) }
    var interval by remember { mutableStateOf(Wallpaper.intervalMin(ctx)) }
    var bingOn by remember { mutableStateOf(Wallpaper.bingOn(ctx)) }
    var bingBusy by remember { mutableStateOf(false) }
    var iconPack by remember { mutableStateOf(Prefs.iconStyle(ctx)) }
    var iconSize by remember { mutableStateOf(Prefs.iconSize(ctx)) }
    var gridCols by remember { mutableStateOf(Prefs.gridCols(ctx)) }
    var dockCount by remember { mutableStateOf(Prefs.dockCount(ctx)) }
    var corner by remember { mutableStateOf(Prefs.tileCorner(ctx)) }
    var labels by remember { mutableStateOf(Prefs.showLabels(ctx)) }
    var topShelf by remember { mutableStateOf(Prefs.topShelf(ctx)) }
    var hiddenSet by remember { mutableStateOf(loadHidden(ctx)) }
    val hiddenApps = apps.filter { it.pkg in hiddenSet }

    // Bing se aaj ki photo abhi lao
    fun fetchBing(turnOn: Boolean) {
        if (bingBusy) return
        bingBusy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) { Wallpaper.bingRefresh(ctx, true) }
            bingBusy = false
            if (ok) {
                if (turnOn) {
                    Wallpaper.setBing(ctx, true)
                    bingOn = true
                }
                onWallpaperChanged()
                Toast.makeText(
                    ctx,
                    "Aaj ki Bing photo lag gayi. Roz apne aap badlegi",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    ctx,
                    "Photo nahi mili. Internet check karo",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    BackHandler {
        when (page) {
            "main" -> onClose()
            "glass", "standby", "iconpack" -> page = "appearance"
            else -> page = "main"
        }
    }

    val first = remember(page) { FocusRequester() }
    LaunchedEffect(
        page, showRemap, showEditor, showGallery, showProfileGallery, showNameEdit
    ) {
        if (!showRemap && !showEditor && !showGallery &&
            !showProfileGallery && !showNameEdit
        ) {
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
                "main" -> "Desktop"
                "profile" -> "Profile"
                "appearance" -> "Appearance"
                "glass" -> "Glass Style"
                "wallpaper" -> "Wallpaper"
                "iconpack" -> "Icon Pack"
                "hidden" -> "Hidden Apps"
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
                    SpLabel("Launcher")
                    SpRow("Profile", profileName + " ›", requester = first) { page = "profile" }
                    Spacer(Modifier.height(8.dp))
                    SpRow(
                        "Wallpaper",
                        when {
                            bingOn -> "Bing Daily ›"
                            wallCount == 0 -> "Default ›"
                            wallCount == 1 -> "Custom ›"
                            else -> "$wallCount photos ›"
                        }
                    ) { page = "wallpaper" }
                    Spacer(Modifier.height(8.dp))
                    SpRow("Appearance", "›") { page = "appearance" }
                    Spacer(Modifier.height(8.dp))
                    SpRow("Button Remap", "›") { showRemap = true }
                    Spacer(Modifier.height(8.dp))
                    SpRow("Run Setup Again", "›") { onRunSetup() }
                    Spacer(Modifier.height(8.dp))
                    SpRow("Hidden Apps", "${hiddenApps.size} ›") { page = "hidden" }
                }

                "profile" -> {
                    SpLabel("Profile")
                    SpRow(
                        "Profile Photo",
                        if (hasProfile) "Set ›" else "Not set ›",
                        requester = first
                    ) { showProfileGallery = true }
                    if (hasProfile) {
                        Spacer(Modifier.height(8.dp))
                        SpRow("Remove Profile Photo") {
                            ProfilePhoto.clear(ctx)
                            hasProfile = false
                            ProfilePhoto.bump()
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SpRow("Profile Name", profileName + " ›") { showNameEdit = true }
                }

                "hidden" -> {
                    if (hiddenApps.isEmpty()) {
                        SpLabel("Koi app hide nahi hai")
                    } else {
                        SpLabel("OK dabao to app wapas dikhega")
                        hiddenApps.forEachIndexed { i, app ->
                            SpRow(
                                app.label,
                                "Show",
                                requester = if (i == 0) first else null
                            ) {
                                val newSet = hiddenSet - app.pkg
                                hiddenSet = newSet
                                saveHidden(ctx, newSet)
                                HiddenStore.rev++
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                "appearance" -> {
                    SpLabel("Style")
                    SpRow(
                        "Glass Style",
                        if (glass == 0) "Liquid Glass ›" else "Frosted Glass ›",
                        requester = first
                    ) { page = "glass" }
                    Spacer(Modifier.height(8.dp))
                    SpRow("Dark Mode", if (dark) "On" else "Off") { onDarkChange(!dark) }

                    SpLabel("Home")
                    SpRow("Top Shelf", if (topShelf) "On" else "Off") {
                        topShelf = !topShelf
                        Prefs.setTopShelf(ctx, topShelf)
                    }

                    SpLabel("Icons")
                    SpRow("Icon Pack", IconPackNames[iconPack.coerceIn(0, 4)] + " ›") {
                        page = "iconpack"
                    }
                    Spacer(Modifier.height(8.dp))
                    SpRow("Icon Size", IconSizeNames[iconSize.coerceIn(0, 2)]) {
                        iconSize = (iconSize + 1) % 3
                        Prefs.setIconSize(ctx, iconSize)
                    }
                    Spacer(Modifier.height(8.dp))
                    SpRow("Icons Per Row (All Apps)", "$gridCols") {
                        gridCols = if (gridCols >= 6) 4 else gridCols + 1
                        Prefs.setGridCols(ctx, gridCols)
                    }
                    Spacer(Modifier.height(8.dp))
                    SpRow("Favourites Max", "$dockCount") {
                        dockCount = if (dockCount >= 6) 4 else dockCount + 1
                        Prefs.setDockCount(ctx, dockCount)
                    }
                    Spacer(Modifier.height(8.dp))
                    SpRow("Tile Corners", CornerNames[corner.coerceIn(0, 2)]) {
                        corner = (corner + 1) % 3
                        Prefs.setTileCorner(ctx, corner)
                    }
                    Spacer(Modifier.height(8.dp))
                    SpRow("App Names", if (labels) "Always" else "On Focus") {
                        labels = !labels
                        Prefs.setShowLabels(ctx, labels)
                    }

                    SpLabel("StandBy")
                    SpRow("Style", StandByNames[standby] + " ›") { page = "standby" }
                }

                "iconpack" -> {
                    IconPackNames.forEachIndexed { i, name ->
                        SpRow(
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
                    SpLabel("Glass Style")
                    SpRow("Liquid Glass", checked = glass == 0, requester = first) {
                        glass = 0
                        Prefs.setGlassStyle(ctx, 0)
                    }
                    Spacer(Modifier.height(8.dp))
                    SpRow("Frosted Glass", checked = glass == 1) {
                        glass = 1
                        Prefs.setGlassStyle(ctx, 1)
                    }
                    SpLabel("Blur")
                    SpBlurRow(blur, remember { FocusRequester() }) {
                        blur = it
                        Prefs.setBlur(ctx, it)
                        onWallpaperChanged()
                    }
                }

                "wallpaper" -> {
                    SpLabel("Bing")
                    SpRow(
                        "Bing Daily Wallpaper",
                        when {
                            bingBusy -> "Laa raha hai..."
                            bingOn -> "On"
                            else -> "Off"
                        },
                        requester = first
                    ) {
                        if (!bingBusy) {
                            if (bingOn) {
                                bingOn = false
                                Wallpaper.setBing(ctx, false)
                                onWallpaperChanged()
                            } else {
                                fetchBing(true)
                            }
                        }
                    }
                    if (bingOn) {
                        Spacer(Modifier.height(8.dp))
                        SpRow("Aaj ki photo abhi lao") { fetchBing(false) }
                        SpLabel("Bing On hai: apni photo aur slideshow band rahegi")
                    }

                    SpLabel("Photos (max ${Wallpaper.MAX})")
                    SpRow("Add Photo", "$wallCount/${Wallpaper.MAX}") {
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
                    SpRow("Adjust Photo", "Rotate, Zoom ›") {
                        if (wallCount > 0) {
                            showEditor = true
                        } else {
                            Toast.makeText(ctx, "Pehle photo jodo", Toast.LENGTH_SHORT).show()
                        }
                    }

                    SpLabel("Slideshow")
                    SpRow("Slideshow", if (slide) "On" else "Off") {
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
                    SpRow("Change Every", "$interval min") {
                        val i = IntervalOptions.indexOf(interval)
                        interval = IntervalOptions[(i + 1) % IntervalOptions.size]
                        Wallpaper.setIntervalMin(ctx, interval)
                        onWallpaperChanged()
                    }
                    if (wallCount > 1) {
                        Spacer(Modifier.height(8.dp))
                        SpRow("Next Photo") {
                            Wallpaper.next(ctx)
                            onWallpaperChanged()
                        }
                    }

                    SpLabel("Hatao")
                    SpRow("Remove Current Photo", if (wallCount > 0) "" else "Koi nahi") {
                        if (wallCount > 0) {
                            Wallpaper.removeCurrent(ctx)
                            wallCount = Wallpaper.count(ctx)
                            onWallpaperChanged()
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SpRow("Remove All Photos", if (wallCount > 0) "" else "Koi nahi") {
                        if (wallCount > 0) {
                            Wallpaper.clear(ctx)
                            wallCount = 0
                            onWallpaperChanged()
                        }
                    }
                }

                else -> {
                    StandByNames.forEachIndexed { i, name ->
                        SpRow(
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
    if (showProfileGallery) {
        GalleryPicker(
            onPick = { uri ->
                showProfileGallery = false
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { ProfilePhoto.save(ctx, uri) }
                    if (ok) {
                        hasProfile = true
                        ProfilePhoto.bump()
                    } else {
                        Toast.makeText(ctx, "Ye photo load nahi ho payi", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onClose = { showProfileGallery = false }
        )
    }
    if (showNameEdit) {
        NameEditDialog(
            initial = profileName,
            onSave = { n ->
                saveProfileName(ctx, n)
                profileName = loadProfileName(ctx)
                ProfileStore.rev++
                showNameEdit = false
            },
            onClose = { showNameEdit = false }
        )
    }
    if (showEditor) {
        WallpaperEditor(
            onChanged = { onWallpaperChanged() },
            onClose = { showEditor = false }
        )
    }
}

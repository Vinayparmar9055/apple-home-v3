package com.vinay.tvlauncher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

const val USER_NAME = "Vinay"
const val USER_FULL_NAME = "Vinay Parmar"

fun defaultFavs(apps: List<AppItem>): List<String> {
    val preferred = listOf(
        "com.google.android.youtube.tv",
        "com.amazon.amazonvideo.livingroom",
        "com.netflix.ninja",
        "com.android.vending",
        "com.google.android.youtube.tvmusic"
    )
    val have = preferred.filter { p -> apps.any { it.pkg == p } }
    return have.ifEmpty { apps.take(5).map { it.pkg } }
}

@Composable
fun ProfileChip(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.08f)
    Row(
        modifier
            .graphicsLayer { scaleX = s; scaleY = s }
            .onFocusChanged { focused = it.isFocused }
            .glass(RoundedCornerShape(50), focused)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 26.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            name,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TopShelf(app: AppItem?) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .glass(RoundedCornerShape(26.dp), false)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (app != null) {
            Box(
                Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Image(app.icon, contentDescription = app.label, modifier = Modifier.size(80.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    app.label,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    "Favourite app · OK dabao kholne ke liye",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 15.sp
                )
            }
        } else {
            Text(
                "Favourites me app jodo (All Apps → Settings → Favourite)",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun HomeScreen(
    apps: List<AppItem>,
    favPkgs: List<String>,
    onSettings: () -> Unit,
    onControl: () -> Unit,
    onAllApps: () -> Unit,
    onFavsReorder: (List<String>) -> Unit,
    onLaunch: (AppItem) -> Unit
) {
    val ctx = LocalContext.current
    val showShelf = remember(Prefs.tick) { Prefs.topShelf(ctx) }
    val favApps = favPkgs.mapNotNull { p -> apps.firstOrNull { it.pkg == p } }
    val first = remember { FocusRequester() }
    val timeReq = remember { FocusRequester() }
    val profileReq = remember { FocusRequester() }
    var lastTop by remember { mutableStateOf<FocusRequester?>(null) }
    var menuApp by remember { mutableStateOf<AppItem?>(null) }
    var movingPkg by remember { mutableStateOf<String?>(null) }
    var swallowUp by remember { mutableStateOf(false) }
    var shelfPkg by remember { mutableStateOf<String?>(null) }
    val shelfApp = favApps.firstOrNull { it.pkg == shelfPkg } ?: favApps.firstOrNull()
    val windowInfo = LocalWindowInfo.current

    LaunchedEffect(apps.isNotEmpty()) {
        if (apps.isNotEmpty()) {
            delay(200)
            runCatching { first.requestFocus() }
        }
    }

    // Popup (Control Center / Weather) band hone par focus wapas usi patti par
    LaunchedEffect(Unit) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { nowFocused ->
            if (nowFocused) {
                val r = lastTop
                if (r != null) {
                    delay(200)
                    runCatching { r.requestFocus() }
                }
            }
        }
    }

    fun moveDock(delta: Int) {
        val p = movingPkg ?: return
        val l = favPkgs.toMutableList()
        val i = l.indexOf(p)
        if (i < 0) return
        val j = (i + delta).coerceIn(0, l.lastIndex)
        if (i == j) return
        l.removeAt(i)
        l.add(j, p)
        onFavsReorder(l)
    }

    BackHandler(enabled = movingPkg != null) { movingPkg = null }

    Column(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { e ->
                if (movingPkg == null) {
                    if (swallowUp && e.type == KeyEventType.KeyUp &&
                        (e.key == Key.Enter || e.key == Key.DirectionCenter)
                    ) {
                        swallowUp = false
                        true
                    } else false
                } else if (e.type == KeyEventType.KeyDown) {
                    when (e.key) {
                        Key.DirectionLeft -> { moveDock(-1); true }
                        Key.DirectionRight -> { moveDock(1); true }
                        Key.DirectionUp, Key.DirectionDown -> true
                        Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> {
                            movingPkg = null
                            swallowUp = true
                            true
                        }
                        else -> false
                    }
                } else true
            }
            .padding(start = 48.dp, end = 48.dp, top = 28.dp, bottom = 24.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            TimeWeatherPill(
                Modifier
                    .focusProperties { down = first }
                    .focusRequester(timeReq)
                    .onFocusChanged { if (it.isFocused) lastTop = timeReq }
            )
            Spacer(Modifier.weight(1f))
            ProfileChip(
                name = USER_FULL_NAME,
                onClick = onControl,
                modifier = Modifier
                    .focusProperties { down = first }
                    .focusRequester(profileReq)
                    .onFocusChanged { if (it.isFocused) lastTop = profileReq }
            )
        }

        Spacer(Modifier.weight(1f))

        if (showShelf) {
            TopShelf(shelfApp)
            Spacer(Modifier.height(14.dp))
        }

        if (movingPkg != null) {
            MoveHint("◀ ▶ se tile hilao · OK dabao jab ho jaye")
            Spacer(Modifier.height(10.dp))
        }

        Dock(
            favApps,
            onLaunch,
            onAllApps,
            first,
            onLongClick = { menuApp = it },
            onFocused = {
                shelfPkg = it.pkg
                lastTop = null
            }
        )
    }

    val ma = menuApp
    if (ma != null) {
        DockMenu(
            app = ma,
            onMove = {
                movingPkg = ma.pkg
                menuApp = null
            },
            onRemove = {
                onFavsReorder(favPkgs - ma.pkg)
                menuApp = null
            },
            onClose = { menuApp = null }
        )
    }
}

@Composable
fun Dock(
    favs: List<AppItem>,
    onLaunch: (AppItem) -> Unit,
    onAllApps: () -> Unit,
    first: FocusRequester,
    onLongClick: (AppItem) -> Unit,
    onFocused: (AppItem) -> Unit = {}
) {
    Box(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(26.dp), false)
            .padding(vertical = 14.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 6.dp)
        ) {
            itemsIndexed(favs, key = { _, a -> a.pkg }) { index, app ->
                AppRectTile(
                    item = app,
                    onClick = { onLaunch(app) },
                    width = 112.dp,
                    height = 76.dp,
                    iconSize = 46.dp,
                    requester = if (index == 0) first else null,
                    onLongClick = { onLongClick(app) },
                    onFocused = { onFocused(app) }
                )
            }
            item { AllAppsTile(onAllApps, 112.dp, 76.dp) }
        }
    }
}

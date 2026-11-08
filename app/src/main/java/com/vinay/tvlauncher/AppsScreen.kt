package com.vinay.tvlauncher

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MAX_FAVS = 6

@Composable
fun AppsScreen(
    apps: List<AppItem>,
    favPkgs: List<String>,
    onFavsChange: (List<String>) -> Unit,
    onLaunch: (AppItem) -> Unit,
    onBack: () -> Unit,
    onSettings: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    var folders by remember { mutableStateOf(Prefs.folders(ctx)) }
    var openFolder by remember { mutableStateOf<String?>(null) }
    var menuApp by remember { mutableStateOf<AppItem?>(null) }
    var favMenu by remember { mutableStateOf<AppItem?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var order by remember { mutableStateOf(OrderStore.list(ctx)) }
    var movingPkg by remember { mutableStateOf<String?>(null) }
    var movingFav by remember { mutableStateOf<String?>(null) }
    var swallowUp by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    fun saveFolders(m: Map<String, List<String>>) {
        folders = m
        Prefs.setFolders(ctx, m)
    }

    fun withoutApp(pkg: String): Map<String, List<String>> =
        folders.mapValues { (_, v) -> v - pkg }

    fun cleaned(m: Map<String, List<String>>, keep: String? = null): Map<String, List<String>> =
        m.filter { (k, v) -> v.isNotEmpty() || k == keep }

    fun toggleFav(app: AppItem) {
        if (app.pkg in favPkgs) {
            onFavsChange(favPkgs - app.pkg)
        } else if (favPkgs.size < MAX_FAVS) {
            onFavsChange(favPkgs + app.pkg)
        } else {
            Toast.makeText(ctx, "$MAX_FAVS se zyada favourites nahi ho sakte", Toast.LENGTH_SHORT).show()
        }
    }

    fun appInfo(pkg: String) {
        try {
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Toast.makeText(ctx, "App Info nahi khul paya", Toast.LENGTH_SHORT).show()
        }
    }

    fun openSettings() {
        val s = onSettings
        if (s != null) {
            s()
        } else {
            try {
                ctx.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                Toast.makeText(ctx, "Settings nahi khul payi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val inFolder = folders.values.flatten().toSet()
    val folderNames = folders.keys.toList()
    val orderIdx = order.withIndex().associate { it.value to it.index }
    val looseApps = apps
        .filter { it.pkg !in inFolder }
        .sortedBy { orderIdx[it.pkg] ?: Int.MAX_VALUE }
    val favApps = favPkgs.mapNotNull { p -> apps.firstOrNull { it.pkg == p } }

    fun moveBy(pkg: String, delta: Int) {
        val list = looseApps.map { it.pkg }
        val idx = list.indexOf(pkg)
        if (idx < 0) return
        val ni = (idx + delta).coerceIn(0, list.lastIndex)
        if (ni == idx) return
        val m = list.toMutableList()
        m.removeAt(idx)
        m.add(ni, pkg)
        val newOrder = m + order.filter { it !in m }
        order = newOrder
        OrderStore.save(ctx, newOrder)
    }

    fun moveFav(pkg: String, delta: Int) {
        val l = favPkgs.toMutableList()
        val i = l.indexOf(pkg)
        if (i < 0) return
        val j = (i + delta).coerceIn(0, l.lastIndex)
        if (i == j) return
        l.removeAt(i)
        l.add(j, pkg)
        onFavsChange(l)
    }

    BackHandler(enabled = movingPkg != null || movingFav != null) {
        movingPkg = null
        movingFav = null
    }

    LaunchedEffect(order) {
        val mp = movingPkg
        if (mp != null) {
            val idx = looseApps.indexOfFirst { it.pkg == mp }
            if (idx >= 0) {
                val target = folderNames.size + idx
                val visible = gridState.layoutInfo.visibleItemsInfo.any { it.index == target }
                if (!visible) runCatching { gridState.animateScrollToItem(target) }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { e ->
                val mp = movingPkg
                val mf = movingFav
                if (mp == null && mf == null) {
                    if (swallowUp && e.type == KeyEventType.KeyUp &&
                        (e.key == Key.Enter || e.key == Key.DirectionCenter)
                    ) {
                        swallowUp = false
                        true
                    } else false
                } else if (e.type == KeyEventType.KeyDown) {
                    when (e.key) {
                        Key.DirectionLeft -> {
                            if (mf != null) moveFav(mf, -1) else if (mp != null) moveBy(mp, -1)
                            true
                        }
                        Key.DirectionRight -> {
                            if (mf != null) moveFav(mf, 1) else if (mp != null) moveBy(mp, 1)
                            true
                        }
                        Key.DirectionUp -> {
                            if (mp != null) moveBy(mp, -6)
                            true
                        }
                        Key.DirectionDown -> {
                            if (mp != null) moveBy(mp, 6)
                            true
                        }
                        Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> {
                            movingPkg = null
                            movingFav = null
                            swallowUp = true
                            true
                        }
                        else -> false
                    }
                } else true
            }
            .padding(horizontal = 48.dp, vertical = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlassButton("‹ Back", onBack)
            Spacer(Modifier.weight(1f))
            if (movingPkg != null) {
                Text(
                    "Tile hilao: ◀ ▶ ▲ ▼ · OK = ho gaya",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light
                )
            } else if (movingFav != null) {
                Text(
                    "Favourite hilao: ◀ ▶ · OK = ho gaya",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light
                )
            }
            Spacer(Modifier.weight(1f))
            GlassButton("Settings", { openSettings() })
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 20.dp, horizontal = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(folderNames, key = { "folder_$it" }) { name ->
                val inside = (folders[name] ?: emptyList()).mapNotNull { p ->
                    apps.firstOrNull { it.pkg == p }
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    FolderTile(
                        name = name,
                        apps = inside,
                        onClick = { openFolder = name },
                        width = 116.dp,
                        height = 76.dp
                    )
                }
            }
            items(looseApps, key = { it.pkg }) { app ->
                val isFav = app.pkg in favPkgs
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    AppRectTile(
                        item = app,
                        onClick = { onLaunch(app) },
                        width = 116.dp,
                        height = 76.dp,
                        iconSize = 46.dp,
                        selected = isFav,
                        onLongClick = { menuApp = app }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(26.dp), false)
                .padding(vertical = 10.dp)
        ) {
            if (favApps.isEmpty()) {
                Text(
                    "Koi favourite nahi hai. Upar kisi app par OK dabaye rakho aur Add to Favourites chuno.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    itemsIndexed(favApps, key = { _, a -> "fav_" + a.pkg }) { _, app ->
                        AppRectTile(
                            item = app,
                            onClick = { onLaunch(app) },
                            width = 112.dp,
                            height = 76.dp,
                            iconSize = 46.dp,
                            onLongClick = { favMenu = app }
                        )
                    }
                }
            }
        }
    }

    val of = openFolder
    if (of != null && folders.containsKey(of)) {
        val inside = (folders[of] ?: emptyList()).mapNotNull { p ->
            apps.firstOrNull { it.pkg == p }
        }
        FolderPopup(
            name = of,
            apps = inside,
            onLaunch = { onLaunch(it) },
            onLongClick = { menuApp = it },
            onClose = { openFolder = null }
        )
    }

    val fm = favMenu
    if (fm != null) {
        DockMenu(
            app = fm,
            onMove = {
                movingFav = fm.pkg
                favMenu = null
            },
            onRemove = {
                onFavsChange(favPkgs - fm.pkg)
                favMenu = null
            },
            onClose = { favMenu = null }
        )
    }

    val ma = menuApp
    if (ma != null && !showNew) {
        val current = folders.entries.firstOrNull { ma.pkg in it.value }?.key
        AppContextMenu(
            app = ma,
            isFav = ma.pkg in favPkgs,
            folderNames = folderNames,
            currentFolder = current,
            onToggleFav = {
                toggleFav(ma)
                menuApp = null
            },
            onMoveTile = {
                if (current == null) {
                    movingPkg = ma.pkg
                } else {
                    Toast.makeText(
                        ctx,
                        "Folder ke andar wale app hil nahi sakte",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                menuApp = null
            },
            onMoveToFolder = { name ->
                val m = LinkedHashMap(withoutApp(ma.pkg))
                m[name] = (m[name] ?: emptyList()) + ma.pkg
                saveFolders(cleaned(m, name))
                openFolder = null
                menuApp = null
            },
            onRemoveFromFolder = {
                saveFolders(cleaned(withoutApp(ma.pkg)))
                openFolder = null
                menuApp = null
            },
            onNewFolder = { showNew = true },
            onAppInfo = {
                appInfo(ma.pkg)
                menuApp = null
            },
            onClose = { menuApp = null }
        )
    }

    if (ma != null && showNew) {
        NewFolderPicker(
            existing = folderNames,
            onPick = { name ->
                val m = LinkedHashMap(withoutApp(ma.pkg))
                m[name] = listOf(ma.pkg)
                saveFolders(cleaned(m, name))
                openFolder = null
                showNew = false
                menuApp = null
            },
            onClose = {
                showNew = false
                menuApp = null
            }
        )
    }
}

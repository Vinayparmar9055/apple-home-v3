package com.vinay.tvlauncher

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    var folders by remember { mutableStateOf(Prefs.folders(ctx)) }
    var openFolder by remember { mutableStateOf<String?>(null) }
    var menuApp by remember { mutableStateOf<AppItem?>(null) }
    var showNew by remember { mutableStateOf(false) }

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

    val inFolder = folders.values.flatten().toSet()
    val folderNames = folders.keys.toList()
    val looseApps = apps.filter { it.pkg !in inFolder }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlassButton("‹ Back", onBack)
            Spacer(Modifier.weight(1f))
            Text(
                if (editing) "Favourites chuno (${favPkgs.size}/$MAX_FAVS)" else "All Apps",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.width(16.dp))
            GlassButton(
                if (editing) "Done" else "Favourites chuno",
                { editing = !editing }
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 20.dp, horizontal = 8.dp),
            modifier = Modifier.fillMaxSize()
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
                        onClick = {
                            if (editing) toggleFav(app) else onLaunch(app)
                        },
                        width = 116.dp,
                        height = 76.dp,
                        iconSize = 46.dp,
                        selected = editing && isFav,
                        onLongClick = { menuApp = app }
                    )
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

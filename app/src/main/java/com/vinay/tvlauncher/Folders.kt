package com.vinay.tvlauncher

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

private val FolderPresets = listOf("Utilities", "Games", "Media", "Tools", "Social", "Family")

@Composable
private fun MenuRow(
    icon: ImageVector?,
    text: String,
    requester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier
    Row(
        base
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Color(0xFFF2F2F2) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (focused) Color.Black else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text,
            color = if (focused) Color.Black else Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun DialogFrame(onClose: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) { content() }
    }
}

@Composable
fun AppContextMenu(
    app: AppItem,
    isFav: Boolean,
    folderNames: List<String>,
    currentFolder: String?,
    onToggleFav: () -> Unit,
    onMoveToFolder: (String) -> Unit,
    onRemoveFromFolder: () -> Unit,
    onNewFolder: () -> Unit,
    onAppInfo: () -> Unit,
    onClose: () -> Unit
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { first.requestFocus() }
    }
    DialogFrame(onClose) {
        Column(
            Modifier
                .width(380.dp)
                .heightIn(max = 460.dp)
                .glass(RoundedCornerShape(26.dp), false)
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            Text(
                app.label,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
            )
            MenuRow(
                Icons.Rounded.Star,
                if (isFav) "Remove from Favourites" else "Add to Favourites",
                requester = first
            ) { onToggleFav() }
            folderNames.filter { it != currentFolder }.forEach { name ->
                MenuRow(Icons.Rounded.Folder, "Move to \"$name\"") { onMoveToFolder(name) }
            }
            if (currentFolder != null) {
                MenuRow(Icons.Rounded.Folder, "Remove from \"$currentFolder\"") { onRemoveFromFolder() }
            }
            MenuRow(Icons.Rounded.CreateNewFolder, "New Folder…") { onNewFolder() }
            MenuRow(Icons.Rounded.Info, "App Info") { onAppInfo() }
        }
    }
}

@Composable
fun NewFolderPicker(
    existing: List<String>,
    onPick: (String) -> Unit,
    onClose: () -> Unit
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { first.requestFocus() }
    }
    val options = FolderPresets.filter { it !in existing }
    DialogFrame(onClose) {
        Column(
            Modifier
                .width(380.dp)
                .glass(RoundedCornerShape(26.dp), false)
                .padding(14.dp)
        ) {
            Text(
                "New Folder",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
            )
            if (options.isEmpty()) {
                MenuRow(null, "Sab naam use ho gaye", requester = first) { onClose() }
            } else {
                options.forEachIndexed { i, name ->
                    MenuRow(
                        Icons.Rounded.CreateNewFolder,
                        name,
                        requester = if (i == 0) first else null
                    ) { onPick(name) }
                }
            }
        }
    }
}

@Composable
fun FolderPopup(
    name: String,
    apps: List<AppItem>,
    onLaunch: (AppItem) -> Unit,
    onLongClick: (AppItem) -> Unit,
    onClose: () -> Unit
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { first.requestFocus() }
    }
    DialogFrame(onClose) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(horizontal = 22.dp, vertical = 8.dp)
            ) {
                Text(name, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .width(560.dp)
                    .glass(RoundedCornerShape(28.dp), false)
                    .padding(20.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 320.dp)
                ) {
                    itemsIndexed(apps, key = { _, a -> a.pkg }) { i, app ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                            AppRectTile(
                                item = app,
                                onClick = { onLaunch(app) },
                                width = 116.dp,
                                height = 76.dp,
                                iconSize = 46.dp,
                                requester = if (i == 0) first else null,
                                onLongClick = { onLongClick(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.vinay.tvlauncher

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var hidden by remember { mutableStateOf(loadHidden(ctx)) }
    val visible = apps.filter { it.pkg !in hidden }
    val favApps = favPkgs.mapNotNull { p -> visible.firstOrNull { it.pkg == p } }
    val rowApps = visible.filter { it.pkg !in favPkgs }
    val favReqs = remember { List(30) { FocusRequester() } }
    val first = favReqs[0]
    val rowFirst = remember { FocusRequester() }
    val timeReq = remember { FocusRequester() }
    val profileReq = remember { FocusRequester() }
    var lastTop by remember { mutableStateOf<FocusRequester?>(null) }
    var menuApp by remember { mutableStateOf<AppItem?>(null) }
    var movingPkg by remember { mutableStateOf<String?>(null) }
    var swallowUp by remember { mutableStateOf(false) }
    var shelfPkg by remember { mutableStateOf<String?>(null) }
    var showRows by remember { mutableStateOf(false) }
    var focusedRow by remember { mutableStateOf(0) }
    var focusedCol by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
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

    // Pehli row se Up: seedha dock ke usi seedh wale app par
    fun backToDock() {
        val idx = focusedCol.coerceIn(0, (favApps.size - 1).coerceAtLeast(0))
        runCatching { favReqs[idx].requestFocus() }
        showRows = false
        scope.launch { runCatching { listState.scrollToItem(0) } }
    }

    // Menu ka kaam hone ke baad focus dock par
    fun afterChange() {
        showRows = false
        scope.launch {
            delay(200)
            runCatching { first.requestFocus() }
        }
    }

    BackHandler(enabled = movingPkg != null) { movingPkg = null }

    // Rows khuli ho to Back dabane par rows band, focus dock par
    BackHandler(enabled = showRows && movingPkg == null && menuApp == null) {
        runCatching { first.requestFocus() }
        showRows = false
        scope.launch { runCatching { listState.scrollToItem(0) } }
    }

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
            LeftInfoCompact(
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

        if (showShelf && !showRows) {
            TopShelf(shelfApp)
            Spacer(Modifier.height(14.dp))
        }

        if (movingPkg != null) {
            MoveHint("◀ ▶ se tile hilao · OK dabao jab ho jaye")
            Spacer(Modifier.height(10.dp))
        }

        Dock(
            favs = favApps,
            rowApps = rowApps,
            listState = listState,
            favReqs = favReqs,
            rowFirst = rowFirst,
            showRows = showRows,
            focusedRow = focusedRow,
            onRevealRows = {
                showRows = true
                scope.launch {
                    delay(150)
                    runCatching { rowFirst.requestFocus() }
                }
            },
            onUpToDock = { backToDock() },
            onLaunch = onLaunch,
            onLongClick = { menuApp = it },
            onRowLongClick = { menuApp = it },
            onFocused = {
                shelfPkg = it.pkg
                lastTop = null
                showRows = false
            },
            onRowFocused = { r, c ->
                lastTop = null
                focusedRow = r
                focusedCol = c
            }
        )
    }

    val ma = menuApp
    if (ma != null) {
        val isFav = ma.pkg in favPkgs
        AppMenu(
            app = ma,
            isFav = isFav,
            canMove = isFav,
            onFav = {
                menuApp = null
                if (isFav) onFavsReorder(favPkgs - ma.pkg)
                else onFavsReorder(favPkgs + ma.pkg)
                afterChange()
            },
            onMove = {
                menuApp = null
                movingPkg = ma.pkg
            },
            onHide = {
                menuApp = null
                val newHidden = hidden + ma.pkg
                hidden = newHidden
                saveHidden(ctx, newHidden)
                if (isFav) onFavsReorder(favPkgs - ma.pkg)
                afterChange()
            },
            onInfo = {
                menuApp = null
                runCatching {
                    val i = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + ma.pkg)
                    )
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(i)
                }
            },
            onClose = { menuApp = null }
        )
    }
}

@Composable
fun Dock(
    favs: List<AppItem>,
    rowApps: List<AppItem>,
    listState: LazyListState,
    favReqs: List<FocusRequester>,
    rowFirst: FocusRequester,
    showRows: Boolean,
    focusedRow: Int,
    onRevealRows: () -> Unit,
    onUpToDock: () -> Unit,
    onLaunch: (AppItem) -> Unit,
    onLongClick: (AppItem) -> Unit,
    onRowLongClick: (AppItem) -> Unit,
    onFocused: (AppItem) -> Unit,
    onRowFocused: (Int, Int) -> Unit
) {
    val rows = remember(rowApps) { rowApps.chunked(6) }
    Column(Modifier.fillMaxWidth()) {
        // Pehli line: chhota glass + patli safed line
        Box(
            Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(32.dp))
                .padding(5.dp)
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown &&
                        e.key == Key.DirectionDown &&
                        !showRows &&
                        rows.isNotEmpty()
                    ) {
                        onRevealRows()
                        true
                    } else false
                }
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(124.dp)
                    .glass(RoundedCornerShape(26.dp), false),
                contentAlignment = Alignment.CenterStart
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
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
                            requester = favReqs.getOrNull(index),
                            onLongClick = { onLongClick(app) },
                            onFocused = { onFocused(app) }
                        )
                    }
                }
            }
        }

        // Neeche ki rows: Down dabane par hi dikhengi, bina glass
        if (showRows) {
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown &&
                            e.key == Key.DirectionUp &&
                            focusedRow == 0
                        ) {
                            onUpToDock()
                            true
                        } else false
                    }
            ) {
                itemsIndexed(rows, key = { i, _ -> "row$i" }) { rowIndex, row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 27.dp, end = 27.dp, top = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        row.forEachIndexed { colIndex, app ->
                            AppRectTile(
                                item = app,
                                onClick = { onLaunch(app) },
                                width = 112.dp,
                                height = 76.dp,
                                iconSize = 46.dp,
                                requester = if (rowIndex == 0 && colIndex == 0) rowFirst else null,
                                onLongClick = { onRowLongClick(app) },
                                onFocused = { onRowFocused(rowIndex, colIndex) }
                            )
                        }
                    }
                }
            }
        }
    }
}

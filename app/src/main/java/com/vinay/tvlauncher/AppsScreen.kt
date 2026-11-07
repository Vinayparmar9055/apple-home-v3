package com.vinay.tvlauncher

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
    var editing by remember { mutableStateOf(false) }

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
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(apps, key = { it.pkg }) { app ->
                val isFav = app.pkg in favPkgs
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    AppRectTile(
                        item = app,
                        onClick = {
                            if (editing) {
                                if (isFav) onFavsChange(favPkgs - app.pkg)
                                else if (favPkgs.size < MAX_FAVS) onFavsChange(favPkgs + app.pkg)
                            } else {
                                onLaunch(app)
                            }
                        },
                        width = 148.dp,
                        height = 92.dp,
                        iconSize = 56.dp,
                        selected = editing && isFav
                    )
                }
            }
        }
    }
}

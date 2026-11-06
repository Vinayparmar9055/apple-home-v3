package com.vinay.tvlauncher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Action(val emoji: String, val title: String, val intentAction: String)

val settingsActions = listOf(
    Action("♿", "Accessibility", Settings.ACTION_ACCESSIBILITY_SETTINGS),
    Action("🏠", "Default Home", Settings.ACTION_HOME_SETTINGS),
    Action("📶", "Network", Settings.ACTION_WIFI_SETTINGS),
    Action("🕒", "Date & Time", Settings.ACTION_DATE_SETTINGS),
    Action("🌐", "Language", Settings.ACTION_LOCALE_SETTINGS),
    Action("⚙️", "All Settings", Settings.ACTION_SETTINGS)
)

val controlActions = listOf(
    Action("📶", "Wi-Fi", Settings.ACTION_WIFI_SETTINGS),
    Action("🎧", "Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS),
    Action("🖥️", "Display", Settings.ACTION_DISPLAY_SETTINGS),
    Action("🔊", "Sound", Settings.ACTION_SOUND_SETTINGS),
    Action("📦", "Apps", Settings.ACTION_APPLICATION_SETTINGS),
    Action("💾", "Storage", Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
)

private fun openAction(ctx: Context, a: Action) {
    try {
        ctx.startActivity(Intent(a.intentAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(ctx, "Ye option is TV par available nahi hai", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ActionScreen(
    title: String,
    actions: List<Action>,
    columns: Int,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = 40.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlassButton("‹ Back", onBack)
            Spacer(Modifier.weight(1f))
            Text(title, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Light)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 32.dp)
        ) {
            items(actions) { a ->
                ActionTile(a.emoji, a.title, onClick = { openAction(ctx, a) })
            }
        }
    }
}

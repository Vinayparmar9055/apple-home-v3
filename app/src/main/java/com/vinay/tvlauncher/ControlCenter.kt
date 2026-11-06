package com.vinay.tvlauncher

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsInputHdmi
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun openSafe(ctx: Context, intent: Intent) {
    try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(ctx, "Ye option is TV par nahi khul paya", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun CcTile(
    modifier: Modifier,
    shape: Shape,
    onClick: () -> Unit,
    requester: FocusRequester? = null,
    on: Boolean = false,
    content: @Composable (Color) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.05f)
    val hi = focused || on
    val fg = if (hi) Color(0xFF111111) else Color.White
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier
    Box(
        modifier
            .then(base)
            .graphicsLayer { scaleX = s; scaleY = s }
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (hi) Color.White else Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content(fg) }
}

@Composable
private fun StatusTile(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    sub: String?,
    onClick: () -> Unit
) {
    CcTile(modifier, RoundedCornerShape(22.dp), onClick) { fg ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(26.dp))
            Column {
                Text(title, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (sub != null) {
                    Text(sub, color = fg.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RoundBtn(icon: ImageVector, on: Boolean = false, onClick: () -> Unit) {
    CcTile(Modifier.size(60.dp), CircleShape, onClick, on = on) { fg ->
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun ControlCenter(
    dark: Boolean,
    onToggleDark: () -> Unit,
    onStandBy: () -> Unit,
    onSettings: () -> Unit,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1000)
        }
    }
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val wifiOn = remember {
        runCatching {
            (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
        }.getOrDefault(false)
    }
    val btOn = remember {
        runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }.getOrDefault(false)
    }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { first.requestFocus() }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .padding(top = 28.dp, end = 48.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            val panel = RoundedCornerShape(30.dp)
            Column(
                Modifier
                    .width(420.dp)
                    .clip(panel)
                    .background(Color(0xE61E1E22))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), panel)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    timeFmt.format(now),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CcTile(
                        Modifier.size(150.dp),
                        RoundedCornerShape(26.dp),
                        onStandBy,
                        requester = first
                    ) { fg ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = fg,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("StandBy", color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusTile(
                            Modifier.width(224.dp).height(70.dp),
                            Icons.Rounded.Wifi, "Wi-Fi", if (wifiOn) "On" else "Off"
                        ) { openSafe(ctx, Intent(Settings.ACTION_WIFI_SETTINGS)) }
                        StatusTile(
                            Modifier.width(224.dp).height(70.dp),
                            Icons.Rounded.Bluetooth, "Bluetooth", if (btOn) "On" else "Off"
                        ) { openSafe(ctx, Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusTile(
                        Modifier.width(187.dp).height(60.dp),
                        Icons.Rounded.SettingsInputHdmi, "Inputs", null
                    ) { openSafe(ctx, Intent("android.media.tv.action.SETUP_INPUTS")) }
                    StatusTile(
                        Modifier.width(187.dp).height(60.dp),
                        Icons.Rounded.Settings, "Settings", null
                    ) { onSettings() }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RoundBtn(Icons.Rounded.Mic) { openSafe(ctx, Intent(Intent.ACTION_VOICE_COMMAND)) }
                    RoundBtn(Icons.Rounded.Cast) { openSafe(ctx, Intent(Settings.ACTION_CAST_SETTINGS)) }
                    RoundBtn(Icons.Rounded.DarkMode, on = dark) { onToggleDark() }
                    RoundBtn(Icons.Rounded.DesktopWindows) {
                        openSafe(ctx, Intent(Settings.ACTION_DISPLAY_SETTINGS))
                    }
                }
            }
        }
    }
}

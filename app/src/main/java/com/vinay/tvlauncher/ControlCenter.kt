package com.vinay.tvlauncher

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputManager
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

private fun openFirst(ctx: Context, intents: List<Intent?>): Boolean {
    for (i in intents) {
        if (i == null) continue
        try {
            ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        } catch (e: Exception) {
            // agla vikalp try karo
        }
    }
    Toast.makeText(ctx, "Ye option is TV par nahi khul paya", Toast.LENGTH_SHORT).show()
    return false
}

private fun tvSettingsClass(cls: String): Intent =
    Intent().setClassName("com.android.tv.settings", cls)

private fun findAppIntent(ctx: Context, words: List<String>): Intent? {
    return runCatching {
        val pm = ctx.packageManager
        val cats = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        var result: Intent? = null
        for (c in cats) {
            if (result != null) break
            val list = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(c), 0)
            val hit = list.firstOrNull { ri ->
                val l = ri.loadLabel(pm).toString().lowercase()
                words.any { l.contains(it) }
            }
            if (hit != null) {
                result = pm.getLaunchIntentForPackage(hit.activityInfo.packageName)
            }
        }
        result
    }.getOrNull()
}

private data class InputEntry(val id: String, val label: String)

private fun loadInputs(ctx: Context): List<InputEntry> {
    return runCatching {
        val tm = ctx.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
        tm.tvInputList
            .filter { it.isPassthroughInput }
            .map {
                val name = (it.loadCustomLabel(ctx) ?: it.loadLabel(ctx))?.toString() ?: it.id
                InputEntry(it.id, name)
            }
    }.getOrDefault(emptyList())
}

@Suppress("DEPRECATION")
private fun wifiLabel(ctx: Context): String {
    return runCatching {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wm.isWifiEnabled) {
            "Off"
        } else {
            val raw = wm.connectionInfo?.ssid ?: ""
            val ssid = raw.removePrefix("\"").removeSuffix("\"")
            if (ssid.isBlank() || ssid.contains("unknown ssid", ignoreCase = true)) "On" else ssid
        }
    }.getOrDefault("Off")
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
            .background(if (hi) Color.White else Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = if (hi) 0f else 0.28f), shape)
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
    CcTile(modifier, RoundedCornerShape(50), onClick) { fg ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(26.dp))
            Column {
                Text(title, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (sub != null) {
                    Text(sub, color = fg.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1)
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
private fun InputsDialog(
    inputs: List<InputEntry>,
    onPick: (InputEntry) -> Unit,
    onClose: () -> Unit
) {
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
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .width(380.dp)
                    .glass(RoundedCornerShape(28.dp), false)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Inputs",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                )
                inputs.forEachIndexed { i, entry ->
                    CcTile(
                        Modifier.fillMaxWidth().height(56.dp),
                        RoundedCornerShape(50),
                        { onPick(entry) },
                        requester = if (i == 0) first else null
                    ) { fg ->
                        Text(
                            entry.label,
                            color = fg,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
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
    val wifiText = remember { wifiLabel(ctx) }
    val btOn = remember {
        runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }.getOrDefault(false)
    }
    val first = remember { FocusRequester() }
    var inputList by remember { mutableStateOf<List<InputEntry>?>(null) }

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
                    .glass(panel, false)
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
                            Icons.Rounded.Wifi, "Wi-Fi", wifiText
                        ) {
                            openFirst(
                                ctx,
                                listOf(
                                    Intent(Settings.ACTION_WIFI_SETTINGS),
                                    Intent(Settings.ACTION_SETTINGS)
                                )
                            )
                        }
                        StatusTile(
                            Modifier.width(224.dp).height(70.dp),
                            Icons.Rounded.Bluetooth, "Bluetooth", if (btOn) "On" else "Off"
                        ) {
                            openFirst(
                                ctx,
                                listOf(
                                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                                    tvSettingsClass("com.android.tv.settings.accessories.AddAccessoryActivity"),
                                    tvSettingsClass("com.android.tv.settings.accessories.BluetoothAccessoryActivity"),
                                    Intent(Settings.ACTION_SETTINGS)
                                )
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusTile(
                        Modifier.width(187.dp).height(60.dp),
                        Icons.Rounded.SettingsInputHdmi, "Inputs", null
                    ) {
                        val list = loadInputs(ctx)
                        if (list.isNotEmpty()) {
                            inputList = list
                        } else {
                            openFirst(
                                ctx,
                                listOf(
                                    Intent("android.settings.TV_INPUT_SETTINGS"),
                                    Intent(Settings.ACTION_SETTINGS)
                                )
                            )
                        }
                    }
                    StatusTile(
                        Modifier.width(187.dp).height(60.dp),
                        Icons.Rounded.Settings, "Settings", null
                    ) { onSettings() }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RoundBtn(Icons.Rounded.Mic) {
                        openFirst(
                            ctx,
                            listOf(
                                Intent(Intent.ACTION_ASSIST),
                                Intent(Intent.ACTION_VOICE_COMMAND),
                                ctx.packageManager.getLaunchIntentForPackage("com.google.android.katniss"),
                                findAppIntent(ctx, listOf("assistant", "voice"))
                            )
                        )
                    }
                    RoundBtn(Icons.Rounded.Cast) {
                        openFirst(
                            ctx,
                            listOf(
                                Intent(Settings.ACTION_CAST_SETTINGS),
                                Intent("android.settings.WIFI_DISPLAY_SETTINGS"),
                                findAppIntent(ctx, listOf("cast", "mirror", "miracast", "screen share")),
                                Intent(Settings.ACTION_SETTINGS)
                            )
                        )
                    }
                    RoundBtn(Icons.Rounded.DarkMode, on = dark) { onToggleDark() }
                    RoundBtn(Icons.Rounded.DesktopWindows) {
                        openFirst(
                            ctx,
                            listOf(
                                Intent(Settings.ACTION_DISPLAY_SETTINGS),
                                Intent(Settings.ACTION_SETTINGS)
                            )
                        )
                    }
                }
            }
        }
    }

    val il = inputList
    if (il != null) {
        InputsDialog(
            inputs = il,
            onPick = { entry ->
                inputList = null
                openFirst(
                    ctx,
                    listOf(
                        Intent(
                            Intent.ACTION_VIEW,
                            TvContract.buildChannelUriForPassthroughInput(entry.id)
                        )
                    )
                )
            },
            onClose = { inputList = null }
        )
    }
}

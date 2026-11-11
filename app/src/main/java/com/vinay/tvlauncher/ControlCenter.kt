package com.vinay.tvlauncher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsInputHdmi
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WifiBlue = Color(0xFF0A84FF)

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

private data class WifiState(val enabled: Boolean, val connected: Boolean, val name: String?)

@Suppress("DEPRECATION")
private fun wifiState(ctx: Context): WifiState {
    val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        ?: return WifiState(false, false, null)
    val enabled = runCatching { wm.isWifiEnabled }.getOrDefault(false)
    var connected = false
    var name: String? = null
    runCatching {
        val info = wm.connectionInfo
        if (info != null) {
            if (info.supplicantState == SupplicantState.COMPLETED) connected = true
            val raw = info.ssid ?: ""
            val ssid = raw.removePrefix("\"").removeSuffix("\"")
            if (ssid.isNotBlank() &&
                !ssid.contains("unknown ssid", ignoreCase = true) &&
                ssid != "0x"
            ) {
                name = ssid
                connected = true
            }
        }
    }
    runCatching {
        if ((wm.dhcpInfo?.ipAddress ?: 0) != 0) connected = true
    }
    runCatching {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            connected = true
        }
    }
    return WifiState(enabled || connected, connected, name)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CcTile(
    modifier: Modifier,
    shape: Shape,
    onClick: () -> Unit,
    requester: FocusRequester? = null,
    on: Boolean = false,
    onLongClick: (() -> Unit)? = null,
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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) { content(fg) }
}

@Composable
private fun StatusTile(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    sub: String?,
    iconTint: Color? = null,
    onClick: () -> Unit
) {
    CcTile(modifier, RoundedCornerShape(50), onClick) { fg ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint ?: fg,
                modifier = Modifier.size(26.dp)
            )
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
private fun RoundBtn(
    icon: ImageVector,
    on: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    CcTile(Modifier.size(60.dp), CircleShape, onClick, on = on, onLongClick = onLongClick) { fg ->
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

// StandBy ab left wale popup me hai, isliye yahan onStandBy use nahi hota
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
    val first = remember { FocusRequester() }
    var inputList by remember { mutableStateOf<List<InputEntry>?>(null) }
    var showPagePicker by remember { mutableStateOf(false) }

    // Wi-Fi: connect ho to neela icon + network ka naam
    var wifiTick by remember { mutableIntStateOf(0) }
    var asked by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> wifiTick++ }
    val wifi = remember(wifiTick) { wifiState(ctx) }
    val wifiSub = when {
        !wifi.enabled -> "Off"
        !wifi.connected -> "On"
        wifi.name != null -> wifi.name
        else -> "Connected"
    }

    LaunchedEffect(Unit) {
        delay(150)
        runCatching { first.requestFocus() }
    }

    // Naam dikhane ke liye Android location permission maangta hai (sirf ek baar poochta hai)
    LaunchedEffect(Unit) {
        delay(400)
        val granted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (wifi.connected && wifi.name == null && !granted && !asked) {
            asked = true
            runCatching { permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
        }
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

                // Wi-Fi: poori chaudai, taaki network ka naam dikhe
                StatusTile(
                    Modifier.fillMaxWidth().height(66.dp).focusRequester(first),
                    Icons.Rounded.Wifi, "Wi-Fi", wifiSub,
                    iconTint = if (wifi.connected) WifiBlue else null
                ) {
                    openFirst(
                        ctx,
                        listOf(
                            Intent(Settings.ACTION_WIFI_SETTINGS),
                            Intent(Settings.ACTION_SETTINGS)
                        )
                    )
                }

                // Settings ke do hisse: Launcher ki settings aur seedha TV ki settings
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusTile(
                        Modifier.width(187.dp).height(60.dp),
                        Icons.Rounded.Tune, "Launcher", "Settings"
                    ) { onSettings() }
                    StatusTile(
                        Modifier.width(187.dp).height(60.dp),
                        Icons.Rounded.Settings, "TV Settings", null
                    ) {
                        openFirst(ctx, listOf(Intent(Settings.ACTION_SETTINGS)))
                    }
                }

                StatusTile(
                    Modifier.fillMaxWidth().height(56.dp),
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
                    // Aakhri gol icon: seedha "Display & Sound" page
                    // (dabake rakho = sahi page dobara chuno)
                    RoundBtn(
                        Icons.Rounded.DesktopWindows,
                        onLongClick = { showPagePicker = true }
                    ) {
                        val saved = DisplaySoundTarget.load(ctx)
                        if (saved == null || !DisplaySoundTarget.open(ctx, saved)) {
                            showPagePicker = true
                        }
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
    if (showPagePicker) {
        SettingsPagePicker(onClose = { showPagePicker = false })
    }
}

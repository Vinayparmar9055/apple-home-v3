package com.vinay.tvlauncher

import android.content.Context
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

object RemapState {
    @Volatile
    var capturing: Boolean = false
}

object RemapStore {
    private fun sp(ctx: Context) =
        ctx.getSharedPreferences("apple_home", Context.MODE_PRIVATE)

    fun all(ctx: Context): LinkedHashMap<Int, String> {
        val out = LinkedHashMap<Int, String>()
        val s = sp(ctx).getString("remap", "") ?: ""
        s.split(";").filter { it.isNotBlank() }.forEach { part ->
            val i = part.indexOf(':')
            if (i > 0) {
                val code = part.substring(0, i).toIntOrNull()
                val action = part.substring(i + 1)
                if (code != null && action.isNotBlank()) out[code] = action
            }
        }
        return out
    }

    fun get(ctx: Context, code: Int): String? = all(ctx)[code]

    fun set(ctx: Context, code: Int, action: String) {
        val m = all(ctx)
        m[code] = action
        save(ctx, m)
    }

    fun remove(ctx: Context, code: Int) {
        val m = all(ctx)
        m.remove(code)
        save(ctx, m)
    }

    private fun save(ctx: Context, m: Map<Int, String>) {
        val s = m.entries.joinToString(";") { "${it.key}:${it.value}" }
        sp(ctx).edit().putString("remap", s).apply()
    }
}

private val BlockedKeys = setOf(
    KeyEvent.KEYCODE_DPAD_UP,
    KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_DPAD_LEFT,
    KeyEvent.KEYCODE_DPAD_RIGHT,
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_NUMPAD_ENTER,
    KeyEvent.KEYCODE_BACK,
    KeyEvent.KEYCODE_HOME
)

fun keyLabel(code: Int): String =
    KeyEvent.keyCodeToString(code)
        .removePrefix("KEYCODE_")
        .split("_")
        .joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }

fun actionLabel(action: String, apps: List<AppItem>): String = when {
    action == "recents" -> "Recent Apps"
    action == "home" -> "Home"
    action == "back" -> "Back"
    action == "playpause" -> "Play / Pause"
    action == "mute" -> "Mute"
    action == "settings" -> "Settings"
    action == "nothing" -> "Nothing"
    action.startsWith("app:") -> {
        val pkg = action.removePrefix("app:")
        "Open " + (apps.firstOrNull { it.pkg == pkg }?.label ?: pkg)
    }
    else -> action
}

private val StandardActions = listOf(
    "recents" to "Recent Apps",
    "home" to "Home",
    "back" to "Back",
    "playpause" to "Play / Pause",
    "mute" to "Mute",
    "settings" to "Settings",
    "nothing" to "Nothing"
)

@Composable
private fun RRow(
    text: String,
    icon: ImageBitmap? = null,
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
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (icon != null) {
            Image(icon, contentDescription = null, modifier = Modifier.size(24.dp))
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
fun ButtonRemapScreen(apps: List<AppItem>, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var stage by remember { mutableStateOf("list") }
    var mappings by remember { mutableStateOf<Map<Int, String>>(RemapStore.all(ctx)) }
    var pickKey by remember { mutableStateOf(0) }
    val first = remember(stage) { FocusRequester() }

    DisposableEffect(stage) {
        RemapState.capturing = stage == "capture"
        onDispose { RemapState.capturing = false }
    }
    LaunchedEffect(stage) {
        delay(120)
        runCatching { first.requestFocus() }
    }

    fun choose(action: String) {
        RemapStore.set(ctx, pickKey, action)
        mappings = RemapStore.all(ctx)
        stage = "list"
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
            when (stage) {
                "capture" -> {
                    Row(
                        Modifier
                            .width(560.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFF2F2F2))
                            .onKeyEvent { e ->
                                if (e.type == KeyEventType.KeyDown) {
                                    val code = e.nativeKeyEvent.keyCode
                                    if (code == KeyEvent.KEYCODE_BACK) {
                                        stage = "list"
                                    } else if (code in BlockedKeys) {
                                        Toast.makeText(
                                            ctx,
                                            "Ye button remap nahi ho sakta",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        pickKey = code
                                        stage = "pick"
                                    }
                                }
                                true
                            }
                            .focusRequester(first)
                            .focusable()
                            .padding(horizontal = 26.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Press the remote button you want…",
                            color = Color.Black,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.weight(1f))
                        Text("Back to cancel", color = Color(0xFF777777), fontSize = 14.sp)
                    }
                }

                "pick" -> {
                    Column(
                        Modifier
                            .width(440.dp)
                            .heightIn(max = 470.dp)
                            .glass(RoundedCornerShape(26.dp), false)
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        Text(
                            "${keyLabel(pickKey)} button does…",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
                        )
                        StandardActions.forEachIndexed { i, (actionKey, label) ->
                            RRow(label, requester = if (i == 0) first else null) { choose(actionKey) }
                        }
                        if (pickKey in mappings) {
                            RRow("Remove mapping") {
                                RemapStore.remove(ctx, pickKey)
                                mappings = RemapStore.all(ctx)
                                stage = "list"
                            }
                        }
                        apps.forEach { a ->
                            RRow("Open ${a.label}", icon = a.icon) { choose("app:${a.pkg}") }
                        }
                    }
                }

                else -> {
                    val entries = mappings.entries.toList()
                    Column(
                        Modifier
                            .width(500.dp)
                            .heightIn(max = 470.dp)
                            .glass(RoundedCornerShape(26.dp), false)
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        Text(
                            "Button Remap",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                        entries.forEachIndexed { i, e ->
                            RRow(
                                "${keyLabel(e.key)}  →  ${actionLabel(e.value, apps)}",
                                requester = if (i == 0) first else null
                            ) {
                                pickKey = e.key
                                stage = "pick"
                            }
                        }
                        RRow(
                            "+ Add Button",
                            requester = if (entries.isEmpty()) first else null
                        ) { stage = "capture" }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "OK, arrows, Back aur Home remap nahi hote.",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

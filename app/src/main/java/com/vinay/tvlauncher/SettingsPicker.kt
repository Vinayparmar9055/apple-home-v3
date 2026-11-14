package com.vinay.tvlauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

// Aakhri gol button ka Settings page: seedha DirectDisplayAndSoundActivity
object DisplaySoundTarget {
    private const val DIRECT_NAME = "DirectDisplayAndSoundActivity"

    private fun sp(ctx: Context) =
        ctx.getSharedPreferences("apple_home", Context.MODE_PRIVATE)

    // Pakka page: DirectDisplayAndSoundActivity (TV me mile to)
    fun direct(ctx: Context): ComponentName? {
        return runCatching {
            findSettingsPages(ctx).firstOrNull { it.shortName == DIRECT_NAME }?.component
        }.getOrNull()
    }

    fun load(ctx: Context): ComponentName? {
        val d = direct(ctx)
        if (d != null) return d
        val s = sp(ctx).getString("ds_target", null) ?: return null
        return ComponentName.unflattenFromString(s)
    }

    fun save(ctx: Context, c: ComponentName) {
        sp(ctx).edit().putString("ds_target", c.flattenToString()).apply()
    }

    fun open(ctx: Context, c: ComponentName): Boolean {
        return try {
            ctx.startActivity(Intent().setComponent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            false
        }
    }
}

data class SettingsPage(val component: ComponentName, val label: String, val shortName: String)

private val KEYWORDS = listOf("display", "sound", "picture", "screen", "audio", "video")

fun findSettingsPages(ctx: Context): List<SettingsPage> {
    return runCatching {
        val pm = ctx.packageManager
        val info = pm.getPackageInfo("com.android.tv.settings", PackageManager.GET_ACTIVITIES)
        val list = ArrayList<SettingsPage>()
        for (a in info.activities ?: emptyArray()) {
            if (!a.exported) continue
            val shortName = a.name.substringAfterLast('.')
            val label = runCatching { a.loadLabel(pm).toString() }.getOrDefault(shortName)
            val hay = (shortName + " " + label).lowercase()
            if (KEYWORDS.any { hay.contains(it) }) {
                list.add(SettingsPage(ComponentName(a.packageName, a.name), label, shortName))
            }
        }
        list.sortedWith(
            compareByDescending<SettingsPage> { it.label.lowercase().contains("display") }
                .thenBy { it.shortName }
        )
    }.getOrDefault(emptyList())
}

@Composable
private fun PickRow(
    title: String,
    sub: String,
    requester: FocusRequester?,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier
    Column(
        base
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Color(0xFFF2F2F2) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            title,
            color = if (focused) Color.Black else Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            sub,
            color = if (focused) Color(0xFF666666) else Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp
        )
    }
}

@Composable
fun SettingsPagePicker(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val direct = remember { DisplaySoundTarget.direct(ctx) }

    // Pakka page mil gaya: list dikhaye bina seedha kholo
    if (direct != null) {
        LaunchedEffect(Unit) {
            if (!DisplaySoundTarget.open(ctx, direct)) {
                Toast.makeText(ctx, "Ye page nahi khula", Toast.LENGTH_SHORT).show()
            }
            onClose()
        }
        return
    }

    val pages = remember { findSettingsPages(ctx) }
    val first = remember { FocusRequester() }
    var current by remember { mutableStateOf(DisplaySoundTarget.load(ctx)?.className) }

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
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .width(560.dp)
                    .heightIn(max = 500.dp)
                    .glass(RoundedCornerShape(26.dp), false)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    "Display & Sound ka sahi page chuno",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                )
                Text(
                    "Ek-ek dabake dekho. Jis page me Picture, Screen, Sound, Audio output " +
                        "dikhe, use aakhri baar kholo, wahi pakka ho jayega.",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
                if (pages.isEmpty()) {
                    Text(
                        "Koi page nahi mila",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                pages.forEachIndexed { i, p ->
                    val mark = if (p.component.className == current) "   ✓ abhi ye pakka hai" else ""
                    PickRow(
                        title = p.label,
                        sub = p.shortName + mark,
                        requester = if (i == 0) first else null
                    ) {
                        if (DisplaySoundTarget.open(ctx, p.component)) {
                            DisplaySoundTarget.save(ctx, p.component)
                            current = p.component.className
                        } else {
                            Toast.makeText(ctx, "Ye page nahi khula", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}

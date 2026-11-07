package com.vinay.tvlauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

data class WallSettings(
    val rotation: Int = 0,   // 0, 90, 180, 270
    val mode: Int = 0,       // 0 = Fill, 1 = Fit (blur ke saath)
    val zoom: Int = 100,     // 100..300
    val offX: Int = 0,       // -100..100
    val offY: Int = 0        // -100..100
)

data class WallBitmaps(val sharp: ImageBitmap, val blur: ImageBitmap)

object Wallpaper {
    private fun file(ctx: Context) = File(ctx.filesDir, "wallpaper.jpg")
    private fun sp(ctx: Context) =
        ctx.getSharedPreferences("apple_home", Context.MODE_PRIVATE)

    fun exists(ctx: Context): Boolean = file(ctx).exists()

    fun settings(ctx: Context): WallSettings {
        val p = sp(ctx)
        return WallSettings(
            rotation = p.getInt("wall_rot", 0),
            mode = p.getInt("wall_mode", 0),
            zoom = p.getInt("wall_zoom", 100),
            offX = p.getInt("wall_ox", 0),
            offY = p.getInt("wall_oy", 0)
        )
    }

    fun saveSettings(ctx: Context, s: WallSettings) {
        sp(ctx).edit()
            .putInt("wall_rot", s.rotation)
            .putInt("wall_mode", s.mode)
            .putInt("wall_zoom", s.zoom)
            .putInt("wall_ox", s.offX)
            .putInt("wall_oy", s.offY)
            .apply()
    }

    fun clear(ctx: Context) {
        file(ctx).delete()
        saveSettings(ctx, WallSettings())
    }

    fun save(ctx: Context, uri: Uri): Boolean {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
            while (maxSide / sample > 2000) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            if (bmp == null) {
                false
            } else {
                file(ctx).outputStream().use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                // Khadi (portrait) photo ho to Fit mode, warna Fill
                saveSettings(ctx, WallSettings(mode = if (bmp.height > bmp.width) 1 else 0))
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    // Saved rotation lagakar photo deta hai
    fun load(ctx: Context): Bitmap? {
        val f = file(ctx)
        if (!f.exists()) return null
        val src = runCatching { BitmapFactory.decodeFile(f.path) }.getOrNull() ?: return null
        val rot = settings(ctx).rotation
        if (rot == 0) return src
        val m = Matrix().apply { postRotate(rot.toFloat()) }
        return runCatching {
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        }.getOrDefault(src)
    }

    fun loadBitmaps(ctx: Context): WallBitmaps? {
        val b = load(ctx) ?: return null
        return WallBitmaps(
            b.asImageBitmap(),
            blur(b, Prefs.blur(ctx)).asImageBitmap()
        )
    }

    // level 0..10
    fun blur(src: Bitmap, level: Int): Bitmap {
        val f = (4 + level * 4).coerceAtLeast(2)
        val w = (src.width / f).coerceAtLeast(8)
        val h = (src.height / f).coerceAtLeast(8)
        val small = Bitmap.createScaledBitmap(src, w, h, true)
        val mid = Bitmap.createScaledBitmap(small, w * 3, h * 3, true)
        return Bitmap.createScaledBitmap(
            mid,
            (src.width / 4).coerceAtLeast(16),
            (src.height / 4).coerceAtLeast(16),
            true
        )
    }
}

@Composable
fun WallpaperLayer(
    sharp: ImageBitmap,
    blurBg: ImageBitmap,
    s: WallSettings,
    blurred: Boolean,
    modifier: Modifier = Modifier
) {
    val main = if (blurred) blurBg else sharp
    Box(modifier.clipToBounds()) {
        if (s.mode == 1) {
            Image(
                bitmap = blurBg,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Image(
            bitmap = main,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val z = s.zoom / 100f
                    scaleX = z
                    scaleY = z
                    translationX = s.offX / 100f * size.width * 0.5f
                    translationY = s.offY / 100f * size.height * 0.5f
                },
            contentScale = if (s.mode == 1) ContentScale.Fit else ContentScale.Crop
        )
    }
}

@Composable
private fun ERow(
    title: String,
    value: String = "",
    requester: FocusRequester? = null,
    onKey: ((Key) -> Boolean)? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier
    Row(
        base
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.08f))
            .onKeyEvent { e ->
                if (onKey != null && e.type == KeyEventType.KeyDown) onKey(e.key) else false
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = if (focused) Color.Black else Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        if (value.isNotEmpty()) {
            Text(
                value,
                color = if (focused) Color(0xFF555555) else Color.White.copy(alpha = 0.65f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun WallpaperEditor(onChanged: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var s by remember { mutableStateOf(Wallpaper.settings(ctx)) }
    var rev by remember { mutableStateOf(0) }
    val bmps by produceState<WallBitmaps?>(initialValue = null, rev) {
        value = withContext(Dispatchers.IO) { Wallpaper.loadBitmaps(ctx) }
    }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { first.requestFocus() }
    }

    fun update(n: WallSettings, reload: Boolean = false) {
        s = n
        Wallpaper.saveSettings(ctx, n)
        if (reload) rev++
        onChanged()
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val shape = RoundedCornerShape(16.dp)
                Box(
                    Modifier
                        .size(480.dp, 270.dp)
                        .clip(shape)
                        .background(Color.Black)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), shape)
                ) {
                    val b = bmps
                    if (b != null) {
                        WallpaperLayer(b.sharp, b.blur, s, false, Modifier.fillMaxSize())
                    }
                }

                Column(
                    Modifier
                        .width(380.dp)
                        .glass(RoundedCornerShape(26.dp), false)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Adjust Wallpaper",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                    )
                    ERow("Rotate", "${s.rotation}°", requester = first) {
                        update(
                            s.copy(rotation = (s.rotation + 90) % 360, offX = 0, offY = 0),
                            reload = true
                        )
                    }
                    ERow(
                        "Fit / Fill",
                        if (s.mode == 1) "Fit (poori photo)" else "Fill (screen bhare)"
                    ) {
                        update(s.copy(mode = if (s.mode == 1) 0 else 1))
                    }
                    ERow(
                        "Zoom  ◀ ▶",
                        "${s.zoom}%",
                        onKey = { k ->
                            when (k) {
                                Key.DirectionLeft -> {
                                    update(s.copy(zoom = (s.zoom - 10).coerceAtLeast(100)))
                                    true
                                }
                                Key.DirectionRight -> {
                                    update(s.copy(zoom = (s.zoom + 10).coerceAtMost(300)))
                                    true
                                }
                                else -> false
                            }
                        }
                    ) { }
                    ERow(
                        "Left / Right  ◀ ▶",
                        "${s.offX}",
                        onKey = { k ->
                            when (k) {
                                Key.DirectionLeft -> {
                                    update(s.copy(offX = (s.offX - 5).coerceAtLeast(-100)))
                                    true
                                }
                                Key.DirectionRight -> {
                                    update(s.copy(offX = (s.offX + 5).coerceAtMost(100)))
                                    true
                                }
                                else -> false
                            }
                        }
                    ) { }
                    ERow(
                        "Upar ◀ / Neeche ▶",
                        "${s.offY}",
                        onKey = { k ->
                            when (k) {
                                Key.DirectionLeft -> {
                                    update(s.copy(offY = (s.offY - 5).coerceAtLeast(-100)))
                                    true
                                }
                                Key.DirectionRight -> {
                                    update(s.copy(offY = (s.offY + 5).coerceAtMost(100)))
                                    true
                                }
                                else -> false
                            }
                        }
                    ) { }
                    ERow("Reset") { update(WallSettings(), reload = true) }
                    ERow("Done") { onClose() }
                }
            }
        }
    }
}

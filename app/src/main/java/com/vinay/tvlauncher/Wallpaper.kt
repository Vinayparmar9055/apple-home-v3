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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
    const val MAX = 10

    private fun dir(ctx: Context): File {
        val d = File(ctx.filesDir, "wall_images")
        if (!d.exists()) d.mkdirs()
        return d
    }

    private fun sp(ctx: Context) =
        ctx.getSharedPreferences("apple_home", Context.MODE_PRIVATE)

    // Purani ek photo wali wallpaper ko nayi list ki pehli photo bana do
    private fun migrate(ctx: Context) {
        val old = File(ctx.filesDir, "wallpaper.jpg")
        if (old.exists()) {
            runCatching {
                val target = File(dir(ctx), "img_0.jpg")
                if (!target.exists()) old.copyTo(target, true)
                old.delete()
            }
        }
    }

    fun files(ctx: Context): List<File> {
        migrate(ctx)
        val l = dir(ctx).listFiles()?.filter { it.name.endsWith(".jpg") } ?: emptyList()
        return l.sortedBy { it.name }
    }

    fun count(ctx: Context): Int = files(ctx).size

    fun exists(ctx: Context): Boolean = count(ctx) > 0

    private fun currentIndex(ctx: Context, n: Int): Int =
        if (n == 0) 0 else sp(ctx).getInt("wall_idx", 0).coerceIn(0, n - 1)

    private fun currentFile(ctx: Context): File? {
        val l = files(ctx)
        if (l.isEmpty()) return null
        return l[currentIndex(ctx, l.size)]
    }

    fun next(ctx: Context) {
        val n = count(ctx)
        if (n < 2) return
        val i = (currentIndex(ctx, n) + 1) % n
        sp(ctx).edit().putInt("wall_idx", i).apply()
    }

    fun removeCurrent(ctx: Context) {
        val f = currentFile(ctx) ?: return
        f.delete()
        val n = count(ctx)
        sp(ctx).edit().putInt("wall_idx", currentIndex(ctx, n)).apply()
        if (n == 0) saveSettings(ctx, WallSettings())
    }

    // Slideshow On/Off
    fun slideshowOn(ctx: Context): Boolean = sp(ctx).getBoolean("wall_slide", false)

    fun setSlideshow(ctx: Context, value: Boolean) {
        sp(ctx).edit().putBoolean("wall_slide", value).apply()
    }

    // Kitne minute me photo badle: 1, 3, 5, 15
    fun intervalMin(ctx: Context): Int = sp(ctx).getInt("wall_interval", 5)

    fun setIntervalMin(ctx: Context, value: Int) {
        sp(ctx).edit().putInt("wall_interval", value).apply()
    }

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
        files(ctx).forEach { it.delete() }
        sp(ctx).edit().putInt("wall_idx", 0).apply()
        saveSettings(ctx, WallSettings())
    }

    private fun decode(ctx: Context, uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
            while (maxSide / sample > 2000) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (e: Exception) {
            null
        }
    }

    // Nayi photo list me jodo (max 10). Jodne ke baad wahi photo dikhegi
    fun add(ctx: Context, uri: Uri): Boolean {
        if (count(ctx) >= MAX) return false
        val bmp = decode(ctx, uri) ?: return false
        return try {
            val wasEmpty = count(ctx) == 0
            val f = File(dir(ctx), "img_" + System.currentTimeMillis() + ".jpg")
            f.outputStream().use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            // Pehli photo khadi (portrait) ho to Fit mode, warna Fill
            if (wasEmpty) {
                saveSettings(ctx, WallSettings(mode = if (bmp.height > bmp.width) 1 else 0))
            }
            val all = files(ctx)
            val idx = all.indexOfFirst { it.name == f.name }
            sp(ctx).edit().putInt("wall_idx", if (idx < 0) 0 else idx).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Purani jagah ke liye: sab hata kar sirf ye ek photo rakho
    fun save(ctx: Context, uri: Uri): Boolean {
        clear(ctx)
        return add(ctx, uri)
    }

    // Abhi dikh rahi photo, saved rotation lagakar
    fun load(ctx: Context): Bitmap? {
        val f = currentFile(ctx) ?: return null
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

package com.vinay.tvlauncher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppItem(
    val label: String,
    val pkg: String,
    val icon: ImageBitmap,
    val intent: Intent,
    // Bina safed peeche ke sirf logo (jahan app ka icon adaptive ho)
    val logo: ImageBitmap? = null,
    // App ka TV banner (wide logo), agar app ne diya ho
    val banner: ImageBitmap? = null
)

// Adaptive icon ka sirf aage wala hissa, beech ka dikhne wala bhaag kaat kar
private fun adaptiveLogo(d: Drawable): ImageBitmap? {
    if (d !is AdaptiveIconDrawable) return null
    val fg = d.foreground ?: return null
    return runCatching {
        val size = 324
        val full = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(full)
        fg.setBounds(0, 0, size, size)
        fg.draw(canvas)

        val crop = size * 2 / 3
        val off = (size - crop) / 2
        val out = Bitmap.createBitmap(full, off, off, crop, crop)

        // Agar lagbhag khali ho to use mat lo
        val small = Bitmap.createScaledBitmap(out, 16, 16, true)
        var visible = 0
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val a = (small.getPixel(x, y) ushr 24) and 0xFF
                if (a > 40) visible++
            }
        }
        if (visible < 8) null else out.asImageBitmap()
    }.getOrNull()
}

private fun tvBanner(ri: android.content.pm.ResolveInfo, ctx: Context): ImageBitmap? {
    val pm = ctx.packageManager
    return runCatching {
        val d = ri.activityInfo.loadBanner(pm)
            ?: ri.activityInfo.applicationInfo.loadBanner(pm)
        d?.toBitmap(320, 180)?.asImageBitmap()
    }.getOrNull()
}

fun loadApps(ctx: Context): List<AppItem> {
    val pm = ctx.packageManager
    val normal = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val tv = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)

    val found = pm.queryIntentActivities(normal, 0) + pm.queryIntentActivities(tv, 0)

    return found
        .distinctBy { it.activityInfo.packageName }
        .filter { it.activityInfo.packageName != ctx.packageName }
        .mapNotNull { ri ->
            val pkg = ri.activityInfo.packageName
            val launch = pm.getLeanbackLaunchIntentForPackage(pkg)
                ?: pm.getLaunchIntentForPackage(pkg)
                ?: return@mapNotNull null
            runCatching {
                val drawable = ri.loadIcon(pm)
                AppItem(
                    label = ri.loadLabel(pm).toString(),
                    pkg = pkg,
                    icon = drawable.toBitmap(128, 128).asImageBitmap(),
                    intent = launch,
                    logo = adaptiveLogo(drawable),
                    banner = tvBanner(ri, ctx)
                )
            }.getOrNull()
        }
        .sortedBy { it.label.lowercase() }
}

fun launchApp(ctx: Context, item: AppItem) {
    try {
        ctx.startActivity(Intent(item.intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(ctx, "App open nahi ho paya", Toast.LENGTH_SHORT).show()
    }
}

package com.vinay.tvlauncher

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppItem(
    val label: String,
    val pkg: String,
    val icon: ImageBitmap,
    val intent: Intent
)

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
                AppItem(
                    label = ri.loadLabel(pm).toString(),
                    pkg = pkg,
                    icon = ri.loadIcon(pm).toBitmap(128, 128).asImageBitmap(),
                    intent = launch
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

package com.vinay.tvlauncher

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Prefs {
    // Jab bhi koi look-setting badle, ye badalta hai, taaki tiles turant badal jayein
    var tick by mutableStateOf(0)
        private set

    private fun bump() {
        tick = tick + 1
    }

    private fun sp(ctx: Context) =
        ctx.getSharedPreferences("apple_home", Context.MODE_PRIVATE)

    fun favourites(ctx: Context): List<String>? {
        val s = sp(ctx).getString("favs", null) ?: return null
        return s.split(",").filter { it.isNotBlank() }
    }

    fun setFavourites(ctx: Context, list: List<String>) {
        sp(ctx).edit().putString("favs", list.joinToString(",")).apply()
    }

    fun dark(ctx: Context): Boolean = sp(ctx).getBoolean("dark", true)

    fun setDark(ctx: Context, value: Boolean) {
        sp(ctx).edit().putBoolean("dark", value).apply()
        bump()
    }

    // 0 = Liquid Glass, 1 = Frosted Glass
    fun glassStyle(ctx: Context): Int = sp(ctx).getInt("glass_style", 0)

    fun setGlassStyle(ctx: Context, value: Int) {
        sp(ctx).edit().putInt("glass_style", value).apply()
        bump()
    }

    // 0..10
    fun blur(ctx: Context): Int = sp(ctx).getInt("blur", 6)

    fun setBlur(ctx: Context, value: Int) {
        sp(ctx).edit().putInt("blur", value).apply()
        bump()
    }

    // 0 = Two Widgets, 1 = Analogue, 2 = Float, 3 = Digital
    fun standbyStyle(ctx: Context): Int = sp(ctx).getInt("standby_style", 0)

    fun setStandbyStyle(ctx: Context, value: Int) {
        sp(ctx).edit().putInt("standby_style", value).apply()
    }

    fun setupDone(ctx: Context): Boolean = sp(ctx).getBoolean("setup_done", false)

    fun setSetupDone(ctx: Context, value: Boolean) {
        sp(ctx).edit().putBoolean("setup_done", value).apply()
    }

    // 0 = tvOS Icons, 1 = Original Banners, 2 = Rounded, 3 = Circle, 4 = Black & White
    fun iconStyle(ctx: Context): Int = sp(ctx).getInt("icon_style", 0)

    fun setIconStyle(ctx: Context, value: Int) {
        sp(ctx).edit().putInt("icon_style", value).apply()
        bump()
    }

    // 0 = Small, 1 = Medium, 2 = Large
    fun iconSize(ctx: Context): Int = sp(ctx).getInt("icon_size", 1)

    fun setIconSize(ctx: Context, value: Int) {
        sp(ctx).edit().putInt("icon_size", value).apply()
        bump()
    }

    // All Apps me ek line me kitne icon: 4, 5 ya 6
    fun gridCols(ctx: Context): Int = sp(ctx).getInt("grid_cols", 6)

    fun setGridCols(ctx: Context, value: Int) {
        sp(ctx).edit().putInt("grid_cols", value).apply()
        bump()
    }

    // Favourites me kitne app: 4, 5 ya 6
    fun dockCount(ctx: Context): Int = sp(ctx).getInt("dock_count", 6)

    fun setDockCount(ctx: Context, value: Int) {
        sp(ctx).edit().putInt("dock_count", value).apply()
        bump()
    }

    // 0 = Sharp, 1 = Normal, 2 = Round
    fun tileCorner(ctx: Context): Int = sp(ctx).getInt("tile_corner", 1)

    fun setTileCorner(ctx: Context, value: Int) {
        sp(ctx).edit().putInt("tile_corner", value).apply()
        bump()
    }

    // false = sirf focus par naam, true = hamesha naam
    fun showLabels(ctx: Context): Boolean = sp(ctx).getBoolean("show_labels", false)

    fun setShowLabels(ctx: Context, value: Boolean) {
        sp(ctx).edit().putBoolean("show_labels", value).apply()
        bump()
    }

    // Folders: naam -> app pkg ki list
    fun folders(ctx: Context): Map<String, List<String>> {
        val s = sp(ctx).getString("folders", "") ?: ""
        val out = LinkedHashMap<String, List<String>>()
        s.split(";").filter { it.isNotBlank() }.forEach { part ->
            val i = part.indexOf(':')
            if (i > 0) {
                val name = part.substring(0, i)
                val pkgs = part.substring(i + 1).split(",").filter { it.isNotBlank() }
                out[name] = pkgs
            }
        }
        return out
    }

    fun setFolders(ctx: Context, map: Map<String, List<String>>) {
        val s = map.entries.joinToString(";") { it.key + ":" + it.value.joinToString(",") }
        sp(ctx).edit().putString("folders", s).apply()
    }
}

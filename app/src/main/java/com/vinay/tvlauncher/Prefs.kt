package com.vinay.tvlauncher

import android.content.Context

object Prefs {
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
    }

    // 0 = Liquid Glass, 1 = Frosted Glass
    fun glassStyle(ctx: Context): Int = sp(ctx).getInt("glass_style", 0)

    fun setGlassStyle(ctx: Context, value: Int) {
        sp(ctx).edit().putInt("glass_style", value).apply()
    }

    // 0..10
    fun blur(ctx: Context): Int = sp(ctx).getInt("blur", 6)

    fun setBlur(ctx: Context, value: Int) {
        sp(ctx).edit().putInt("blur", value).apply()
    }

    // 0 = Two Widgets, 1 = Analogue, 2 = Float, 3 = Digital
    fun standbyStyle(ctx: Context): Int = sp(ctx).getInt("standby_style", 0)

    fun setStandbyStyle(ctx: Context, value: Int) {
        sp(ctx).edit().putInt("standby_style", value).apply()
    }
}

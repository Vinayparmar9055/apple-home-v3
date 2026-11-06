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
}

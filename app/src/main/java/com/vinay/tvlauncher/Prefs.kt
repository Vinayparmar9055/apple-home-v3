package com.vinay.tvlauncher

import android.content.Context

object Prefs {
    private fun sp(ctx: Context) =
        ctx.getSharedPreferences("apple_home", Context.MODE_PRIVATE)

    fun favourites(ctx: Context): List<String> =
        (sp(ctx).getString("favs", "") ?: "")
            .split(",").filter { it.isNotBlank() }

    fun setFavourites(ctx: Context, list: List<String>) {
        sp(ctx).edit().putString("favs", list.joinToString(",")).apply()
    }
}

package com.vinay.tvlauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

object ProfilePhoto {
    // Photo badalne par ye badalta hai, taaki Home par turant naya dikhe
    var rev by mutableStateOf(0)
        private set

    fun bump() {
        rev = rev + 1
    }

    private fun file(ctx: Context) = File(ctx.filesDir, "profile.jpg")

    fun exists(ctx: Context): Boolean = file(ctx).exists()

    fun clear(ctx: Context) {
        file(ctx).delete()
        bump()
    }

    // Photo ko beech se chorus kaat kar 256x256 me save karta hai
    fun save(ctx: Context, uri: Uri): Boolean {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            val biggest = maxOf(bounds.outWidth, bounds.outHeight)
            while (biggest / sample > 1200) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            if (bmp == null) {
                false
            } else {
                val side = minOf(bmp.width, bmp.height)
                val x = (bmp.width - side) / 2
                val y = (bmp.height - side) / 2
                val square = Bitmap.createBitmap(bmp, x, y, side, side)
                val small = Bitmap.createScaledBitmap(square, 256, 256, true)
                file(ctx).outputStream().use { out ->
                    small.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun load(ctx: Context): ImageBitmap? {
        val f = file(ctx)
        if (!f.exists()) return null
        return runCatching { BitmapFactory.decodeFile(f.path)?.asImageBitmap() }.getOrNull()
    }
}

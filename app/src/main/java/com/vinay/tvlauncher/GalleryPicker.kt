package com.vinay.tvlauncher

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private fun readPerm(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
    else Manifest.permission.READ_EXTERNAL_STORAGE

private fun queryImages(ctx: Context): List<Uri> {
    val out = ArrayList<Uri>()
    runCatching {
        val proj = arrayOf(MediaStore.Images.Media._ID)
        ctx.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            proj,
            null,
            null,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (c.moveToNext() && out.size < 80) {
                out.add(
                    ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        c.getLong(idCol)
                    )
                )
            }
        }
    }
    return out
}

private fun thumb(ctx: Context, uri: Uri): Bitmap? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        val biggest = maxOf(bounds.outWidth, bounds.outHeight)
        while (biggest / sample > 480) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }.getOrNull()
}

@Composable
private fun PhotoCell(uri: Uri, requester: FocusRequester?, onClick: () -> Unit) {
    val ctx = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    val s = focusScale(focused, 1.08f)
    val bmp by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { thumb(ctx, uri)?.asImageBitmap() }
    }
    val shape = RoundedCornerShape(14.dp)
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier
    Box(
        base
            .fillMaxWidth()
            .height(120.dp)
            .graphicsLayer { scaleX = s; scaleY = s }
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                if (focused) 3.dp else 1.dp,
                Color.White.copy(alpha = if (focused) 0.95f else 0.2f),
                shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val b = bmp
        if (b != null) {
            Image(
                bitmap = b,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun GalleryPicker(onPick: (Uri) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val perm = remember { readPerm() }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED
        )
    }
    var denied by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        granted = ok
        if (!ok) denied = true
    }

    LaunchedEffect(Unit) {
        if (!granted) runCatching { permLauncher.launch(perm) }
    }

    val images by produceState<List<Uri>?>(initialValue = null, granted) {
        value = withContext(Dispatchers.IO) { queryImages(ctx) }
    }

    val first = remember { FocusRequester() }
    val list = images
    val hasList = list != null && list.isNotEmpty()

    LaunchedEffect(hasList) {
        delay(200)
        runCatching { first.requestFocus() }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 28.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GlassButton(
                        "‹ Back",
                        onClose,
                        if (!hasList) Modifier.focusRequester(first) else Modifier
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Photo chuno",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(110.dp))
                }
                Spacer(Modifier.height(16.dp))

                if (list == null) {
                    Text(
                        "Photo dhundh raha hai…",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 18.sp
                    )
                } else if (list.isEmpty()) {
                    Text(
                        if (denied) {
                            "Permission nahi mili. TV Settings → Apps → Apple Home → Permissions me Photos allow karo."
                        } else {
                            "Koi photo nahi mili. Photo TV ke storage me ya USB me daalo."
                        },
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 18.sp
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(list, key = { _, u -> u.toString() }) { i, u ->
                            PhotoCell(u, if (i == 0) first else null) { onPick(u) }
                        }
                    }
                }
            }
        }
    }
}

package com.vinay.tvlauncher

import android.content.Context
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

// Hide/unhide badalne par Home ko khabar dene ke liye
object HiddenStore {
    var rev by mutableIntStateOf(0)
}

// Profile ka naam badalne par Home ko khabar dene ke liye
object ProfileStore {
    var rev by mutableIntStateOf(0)
}

fun loadProfileName(ctx: Context): String {
    val n = ctx.getSharedPreferences("tvl_profile", Context.MODE_PRIVATE)
        .getString("name", null)
    return if (n.isNullOrBlank()) USER_FULL_NAME else n
}

fun saveProfileName(ctx: Context, name: String) {
    val e = ctx.getSharedPreferences("tvl_profile", Context.MODE_PRIVATE).edit()
    if (name.isBlank()) e.remove("name") else e.putString("name", name.trim())
    e.apply()
}

@Composable
fun SpRow(
    title: String,
    value: String = "",
    checked: Boolean = false,
    requester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val base = if (requester != null) Modifier.focusRequester(requester) else Modifier
    Row(
        base
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.08f))
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = if (focused) Color.Black else Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        if (value.isNotEmpty()) {
            Text(
                value,
                color = if (focused) Color(0xFF555555) else Color.White.copy(alpha = 0.6f),
                fontSize = 15.sp
            )
        }
        if (checked) {
            Text(
                "✓",
                color = if (focused) Color.Black else Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SpBlurRow(value: Int, requester: FocusRequester, onChange: (Int) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier
            .focusRequester(requester)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.08f))
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown) {
                    when (e.key) {
                        Key.DirectionLeft -> { onChange((value - 1).coerceAtLeast(0)); true }
                        Key.DirectionRight -> { onChange((value + 1).coerceAtMost(10)); true }
                        else -> false
                    }
                } else false
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable { }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Blur Value  ◀ ▶",
                color = if (focused) Color.Black else Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                "$value",
                color = if (focused) Color(0xFF555555) else Color.White.copy(alpha = 0.6f),
                fontSize = 15.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (focused) Color.Black.copy(alpha = 0.15f)
                    else Color.White.copy(alpha = 0.15f)
                )
        ) {
            Box(
                Modifier
                    .fillMaxWidth(value / 10f)
                    .height(4.dp)
                    .background(if (focused) Color.Black else Color.White)
            )
        }
    }
}

@Composable
fun SpLabel(text: String) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.55f),
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 6.dp, top = 14.dp, bottom = 6.dp)
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NameEditDialog(
    initial: String,
    onSave: (String) -> Unit,
    onClose: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    val fieldReq = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(250)
        runCatching { fieldReq.requestFocus() }
        delay(150)
        keyboard?.show()
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .width(420.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF16324F).copy(alpha = 0.96f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Profile Name",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { if (it.length <= 24) text = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSave(text) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(fieldReq)
                    )
                }
                SpRow("Save") { onSave(text) }
                SpRow("Cancel") { onClose() }
            }
        }
    }
}

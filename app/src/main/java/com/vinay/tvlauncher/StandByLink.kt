package com.vinay.tvlauncher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

// Left popup se StandBy screen kholne ke liye chhota sandesha
object StandByLink {
    var tick by mutableIntStateOf(0)
        private set

    fun open() {
        tick = tick + 1
    }
}

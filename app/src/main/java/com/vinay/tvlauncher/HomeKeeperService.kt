package com.vinay.tvlauncher

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class HomeKeeperService : AccessibilityService() {

    private var lastOpen = 0L

    // Stock Google TV launcher packages
    private val stockLaunchers = setOf(
        "com.google.android.tvlauncher",
        "com.google.android.apps.tv.launcherx"
    )

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode

        if (code == KeyEvent.KEYCODE_HOME) {
            if (event.action == KeyEvent.ACTION_UP) openLauncher()
            return true
        }

        // Remap chun'ne ke waqt button app tak jaane do
        if (RemapState.capturing) return false

        val action = RemapStore.get(this, code) ?: return false
        if (event.action == KeyEvent.ACTION_UP) runAction(action)
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg in stockLaunchers) openLauncher()
    }

    override fun onInterrupt() {}

    private fun runAction(action: String) {
        try {
            when {
                action == "home" -> openLauncher()
                action == "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
                action == "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                action == "playpause" -> mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                action == "mute" -> {
                    val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_TOGGLE_MUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                }
                action == "settings" -> startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                action == "nothing" -> {}
                action.startsWith("app:") -> {
                    val pkg = action.removePrefix("app:")
                    val i = packageManager.getLeanbackLaunchIntentForPackage(pkg)
                        ?: packageManager.getLaunchIntentForPackage(pkg)
                    if (i != null) startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        } catch (e: Exception) {
            // koi galti ho to chup-chaap ignore
        }
    }

    private fun mediaKey(code: Int) {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    private fun openLauncher() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastOpen < 800) return
        lastOpen = now
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        )
    }
}

package com.example.a_eyes

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class VolumeButtonService : AccessibilityService() {

    private val sequence = listOf(
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN)
    private val inputSequence = mutableListOf<Int>()
    private var lastTime: Long = 0

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTime > 2000) {
            // Reset if too slow
            inputSequence.clear()
        }
        lastTime = currentTime

        inputSequence.add(event.keyCode)
        if (inputSequence.size > sequence.size) {
            inputSequence.removeAt(0)
        }

        if (inputSequence == sequence) {
            launchMainApp()
            inputSequence.clear()
        }

        return false // allow event to propagate
    }

    private fun launchMainApp() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launchIntent)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("VolumeButtonService", "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        TODO("Not yet implemented")
    }

    override fun onInterrupt() {}
}
package com.example.gesturecontrol.logic

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent

class CommandDispatcher(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val toneGenerator = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
    private var lastCommandTime = -1000L
    private val DEBOUNCE_MS = 1000L // 1 second debounce to prevent rapid firing

    private val gestureMapper = GestureMapper(context)

    fun onGestureRecognized(gestureCategory: String) {
        val now = SystemClock.uptimeMillis()
        if (now - lastCommandTime < DEBOUNCE_MS && gestureCategory != "None") return
        
        val action = gestureMapper.getAction(gestureCategory)
        if (action == "NONE") return

        Log.d("CommandDispatcher", "Gesture: $gestureCategory -> Action: $action")
        
        when (action) {
            "PLAY" -> {
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
                playTone(true)
            }
            "PAUSE" -> {
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
                playTone(true)
            }
            "NEXT" -> {
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
                playTone(true)
            }
            "PREVIOUS" -> {
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                playTone(true)
            }
            "VOLUME_UP" -> {
                adjustVolume(AudioManager.ADJUST_RAISE)
                playTone(true)
            }
            "VOLUME_DOWN" -> {
                adjustVolume(AudioManager.ADJUST_LOWER)
                playTone(true)
            }
            "MUTE" -> {
                toggleMute()
                playTone(true)
            }
            "HOME" -> {
                launchClusterApp() // Using Mock Cluster/Home trigger
                playTone(true, doubleBeep = true)
            }
            "FAVORITE" -> {
                Log.i("CommandDispatcher", "Saved to Favorites!")
                playTone(true, doubleBeep = true)
            }
        }
        
        lastCommandTime = now
    }

    private fun sendMediaKey(keyCode: Int) {
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        
        audioManager.dispatchMediaKeyEvent(eventDown)
        audioManager.dispatchMediaKeyEvent(eventUp)
        Log.i("CommandDispatcher", "Sent Media Key: $keyCode")
    }

    private fun adjustVolume(direction: Int) {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        Log.i("CommandDispatcher", "Adjusted Volume: $direction")
    }

    private fun launchClusterApp() {
        // Mock Implementation for Cluster Launch
        // In real AAOS 8295, this might use CarPropertyManager to set a VHAL property
        // or send a specific broadcast intent that the Cluster service listens to.
        Log.i("CommandDispatcher", "Launching Cluster App (Mock signal sent)")
        
        // Example Intent (if Cluster listens to this):
        // val intent = Intent("com.automotive.cluster.LAUNCH_APP")
        // context.sendBroadcast(intent)
    }

    private fun playTone(success: Boolean, doubleBeep: Boolean = false) {
         try {
            if (success) {
                toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
            }
         } catch (e: Exception) {
             Log.e("CommandDispatcher", "Tone error: ${e.message}")
         }
    }
    
    private fun toggleMute() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
        Log.i("CommandDispatcher", "Toggled Mute")
    }
}

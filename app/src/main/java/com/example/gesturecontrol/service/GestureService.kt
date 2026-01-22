package com.example.gesturecontrol.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.gesturecontrol.R
import com.example.gesturecontrol.camera.CameraStreamManager
import com.example.gesturecontrol.logic.CommandDispatcher
import com.example.gesturecontrol.logic.GestureFeedback
import com.example.gesturecontrol.logic.GestureProcessor
import com.example.gesturecontrol.logic.HealthProcessor
import com.example.gesturecontrol.logic.HealthState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GestureService : LifecycleService() {

    private val binder = LocalBinder()
    
    // Core Components moved from MainActivity
    private lateinit var gestureProcessor: GestureProcessor
    private lateinit var commandDispatcher: CommandDispatcher
    private lateinit var cameraStreamManager: CameraStreamManager
    private lateinit var healthProcessor: HealthProcessor
    
    // Callbacks for UI
    var onFrameUpdate: ((Bitmap) -> Unit)? = null
    var onFeedbackUpdate: ((GestureFeedback) -> Unit)? = null
    var onHealthUpdate: ((HealthState) -> Unit)? = null
    var onStatusUpdate: ((String) -> Unit)? = null

    private var lastMood = "Neutral"

    private var lastFrameTime = 0L

    inner class LocalBinder : Binder() {
        fun getService(): GestureService = this@GestureService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        
        // Initialize Core Components
        commandDispatcher = CommandDispatcher(this)
        
        gestureProcessor = GestureProcessor(this) { feedback ->
            // Capture Mood / State
            lastMood = feedback.mood
            
            // Check Critical Driver States
            when (feedback.mood) {
                "DROWSY WARNING" -> commandDispatcher.triggerDrowsinessAlarm()
                "Confused ?" -> commandDispatcher.triggerAssistance()
                "Discomfort/Wince" -> commandDispatcher.triggerSeatAdjustment()
                // Distraction is logged but maybe doesn't trigger a tone every frame? 
                // Let's assume CommandDispatcher limits tone frequency if we added debounce there.
                // But for now, let's just log or visual only for Distracted unless it persists.
                // The Processor logic already has a 2s timer for "DISTRACTED".
            }

            // Execute Command (Always, background or foreground)
            if (!feedback.isSleeping) {
                val action = com.example.gesturecontrol.logic.GestureMapper(this).getAction(feedback.gestureName)
                if (action != "NONE") {
                     // We could pass action here if CommandDispatcher supported it, 
                     // but currently CommandDispatcher handles mapping internally? 
                     // Wait, CommandDispatcher takes gestureName, it might need updating or GestureProcessor calls it.
                     // Checked MainActivity: commandDispatcher.onGestureRecognized(feedback.gestureName)
                     commandDispatcher.onGestureRecognized(feedback.gestureName)
                }
            }
            
            // Send Update to UI if bound
            onFeedbackUpdate?.invoke(feedback)
        }
        gestureProcessor.isDemoMode = true // Default
        
        healthProcessor = HealthProcessor { state ->
            if (state.stressLevel == "High" && !state.isCalmModeActive) {
                // Trigger Calm Mode
                commandDispatcher.triggerCalmMode()
                healthProcessor.setCalmMode(true) // Prevent re-trigger
            }
            onHealthUpdate?.invoke(state)
        }
        
        cameraStreamManager = CameraStreamManager(
            onFrame = { bitmap ->
                // 1. Send to UI for Preview
                onFrameUpdate?.invoke(bitmap)
                
                // 2. Process AI (in background service scope)
                lifecycleScope.launch(Dispatchers.Default) {
                     lastFrameTime = System.currentTimeMillis()
                     gestureProcessor.processingFrame(bitmap)
                     
                     // Health Processing
                     healthProcessor.processFrame(bitmap, gestureProcessor.lastFaceResult, lastMood)
                }
            },
            onConnected = {
                onStatusUpdate?.invoke("Camera Connected [Bg Service]")
            },
            onError = { error ->
                onStatusUpdate?.invoke("Error: $error")
            }
        )
        
        // Stale Frame Watchdog
        lifecycleScope.launch(Dispatchers.Default) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (System.currentTimeMillis() - lastFrameTime > 2000) {
                     // Stream stalled? Reset gesture to prevent stuck state
                     // We can manually trigger an empty/none feedback or just let it be.
                     // But GestureProcessor doesn't clear state automatically if no frames come in.
                     // Let's force a "None" update if needed, but GestureProcessor state is internal.
                     // Better: Just don't trigger commands if stream is old.
                     // But if the LAST command was "Vol Up" and it repeats? CommandDispatcher has debounce.
                     // The issue "automatically change based on last gesture" implies the feedback loop might be stuck.
                     // If processingFrame isn't called, no new feedback is generated.
                     // So the UI might show old text, but no new commands are fired.
                     // UNLESS CommandDispatcher is looping? No, it triggers once per call.
                     // So maybe the user sees the TEXT stuck and thinks it's acting?
                     // Or maybe the stream buffer is looping? 
                     // Regardless, let's update status.
                     if (lastFrameTime > 0) {
                         onStatusUpdate?.invoke("Stream Stalled / Waiting...")
                         
                         // Auto-Reconnect Attempt (Simple)
                         if (System.currentTimeMillis() - lastFrameTime > 5000) {
                              onStatusUpdate?.invoke("Attempting Auto-Reconnect...")
                              val prefs = getSharedPreferences("GestureAppPrefs", Context.MODE_PRIVATE)
                              val url = prefs.getString("camera_url", "")
                              if (!url.isNullOrEmpty()) {
                                  // Re-trigger startCamera which restarts stream
                                  cameraStreamManager.startStream(url)
                                  lastFrameTime = System.currentTimeMillis() // Reset timer to prevent rapid loop
                              }
                         }
                     }
                }
            }
        }
    }
    
    
    fun startCamera(url: String) {
        val prefs = getSharedPreferences("GestureAppPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("camera_url", url).apply()
        cameraStreamManager.startStream(url)
    }
    
    fun registerDriver(name: String) {
        gestureProcessor.registerDriver(name)
    }
    
    fun stopCamera() {
        cameraStreamManager.stop()
    }

    private fun startForegroundService() {
        val channelId = "GestureServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gesture Control Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Gesture Control Active")
            .setContentText("Camera is running in background detecting gestures...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        // ID must be > 0
        startForeground(1001, notification) 
    }

    override fun onDestroy() {
        super.onDestroy()
        gestureProcessor.close()
        cameraStreamManager.stop()
    }
}

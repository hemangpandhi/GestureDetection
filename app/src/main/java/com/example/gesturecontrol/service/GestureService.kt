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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GestureService : LifecycleService() {

    private val binder = LocalBinder()
    
    // Core Components moved from MainActivity
    private lateinit var gestureProcessor: GestureProcessor
    private lateinit var commandDispatcher: CommandDispatcher
    private lateinit var cameraStreamManager: CameraStreamManager
    
    // Callbacks for UI
    var onFrameUpdate: ((Bitmap) -> Unit)? = null
    var onFeedbackUpdate: ((GestureFeedback) -> Unit)? = null
    var onStatusUpdate: ((String) -> Unit)? = null

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
        
        cameraStreamManager = CameraStreamManager(
            onFrame = { bitmap ->
                // 1. Send to UI for Preview
                onFrameUpdate?.invoke(bitmap)
                
                // 2. Process AI (in background service scope)
                lifecycleScope.launch(Dispatchers.Default) {
                     gestureProcessor.processingFrame(bitmap)
                }
            },
            onConnected = {
                onStatusUpdate?.invoke("Camera Connected [Bg Service]")
            },
            onError = { error ->
                onStatusUpdate?.invoke("Error: $error")
            }
        )
    }
    
    fun startCamera(url: String) {
        val prefs = getSharedPreferences("GestureAppPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("camera_url", url).apply()
        cameraStreamManager.startStream(url)
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
            .setSmallIcon(R.mipmap.ic_launcher)
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

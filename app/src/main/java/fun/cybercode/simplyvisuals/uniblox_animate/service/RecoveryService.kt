package `fun`.cybercode.simplyvisuals.uniblox_animate.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.room.Room
import `fun`.cybercode.simplyvisuals.uniblox_animate.MainActivity
import `fun`.cybercode.simplyvisuals.uniblox_animate.R
import `fun`.cybercode.simplyvisuals.uniblox_animate.data.StudioDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecoveryService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            val db = Room.databaseBuilder(
                applicationContext,
                StudioDatabase::class.java, "studio-db"
            ).fallbackToDestructiveMigration().build()
            
            val session = withContext(Dispatchers.IO) {
                db.studioDao().getRecoverySession()
            }
            db.close()

            if (session != null) {
                showOverlay()
            } else {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Inflate the layout
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        // We'll create the layout programmatically or use a simple one if we had R.layout
        // But since I can create files, I'll create res/layout/recovery_overlay.xml
        
        try {
            overlayView = inflater.inflate(R.layout.recovery_overlay, null)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.CENTER
            
            overlayView?.findViewById<Button>(R.id.btn_import)?.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("RECOVER", true)
                }
                startActivity(intent)
                stopSelf()
            }

            overlayView?.findViewById<Button>(R.id.btn_abandon)?.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("ABANDON", true)
                }
                startActivity(intent)
                stopSelf()
            }

            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            windowManager?.removeView(it)
        }
    }
}

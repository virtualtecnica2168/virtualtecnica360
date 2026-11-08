package com.virtualtecnica.cam

import android.app.*
import android.content.*
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.core.app.ServiceCompat

/** Barra mínima arriba (sobre la cámara): Maximizar, Cámara, Último, Crear. */
class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private val h = Handler(Looper.getMainLooper())
    private var lastId = -1L

    private val scan = Runnable {
        Media.latest(this)?.let { if (it.first != lastId) { lastId = it.first; status.text = "Virtual 360 · Nuevo video: ${it.second}" } }
    }
    private val observer = object : ContentObserver(h) {
        override fun onChange(selfChange: Boolean) { h.removeCallbacks(scan); h.postDelayed(scan, 2000) }
    }

    override fun onBind(i: Intent?): IBinder? = null
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("vt", "Barra", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "vt").setContentTitle("Virtual 360 minimizado")
            .setSmallIcon(android.R.drawable.ic_menu_camera).build()
        ServiceCompat.startForeground(this, 1, n,
            if (Build.VERSION.SDK_INT >= 34) android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        buildBar()
        contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
    }

    private fun app(vararg extra: String) {
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .apply { extra.forEach { putExtra(it, true) } })
    }

    private fun camera() {
        val i = packageManager.getLaunchIntentForPackage("com.sec.android.app.camera")
            ?: Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA)
        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun btn(t: String, c: Int, f: () -> Unit) = Button(this).apply {
        text = t; isAllCaps = false; textSize = 12f; setTextColor(Color.WHITE)
        minHeight = 0; minimumHeight = 0; minWidth = 0; minimumWidth = 0; setPadding(dp(4), dp(8), dp(4), dp(8))
        background = GradientDrawable().apply { setColor(c); cornerRadius = dp(10).toFloat() }
        setOnClickListener { f() }
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) }
    }

    private fun buildBar() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(6), dp(4), dp(8))
            background = GradientDrawable().apply { setColor(0xF0171C24.toInt()); cornerRadius = dp(14).toFloat() }
        }
        status = TextView(this).apply { text = "Virtual 360 · Graba un video con la cámara"; setTextColor(Color.WHITE); textSize = 12f; maxLines = 1 }
        val row = LinearLayout(this).apply {
            setPadding(0, dp(6), 0, 0)
            addView(btn("Maximizar", 0xFF3A4150.toInt()) { app("max") })
            addView(btn("Cámara", 0xFF3A4150.toInt()) { camera() })
            addView(btn("Último", 0xFF3B82F6.toInt()) { app("autoload") })
            addView(btn("Crear", 0xFFFF4F81.toInt()) { app("crear") })
        }
        root.addView(status); root.addView(row)
        val lp = WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP; y = dp(28); x = 0
        }
        wm.addView(root, lp)
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(observer)
        runCatching { wm.removeView(root) }
        super.onDestroy()
    }
}

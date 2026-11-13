package com.danzabot.stepcounter

import android.app.*
import android.content.Intent
import android.os.*

class WidgetUpdateService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val interval = 1000L / 15 // ~15 fps

    private val tick = object : Runnable {
        override fun run() {
            StepCounterWidget.updateAll(this@WidgetUpdateService)
            handler.postDelayed(this, interval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val channelId = "widget_anim"
        val chan = NotificationChannel(channelId, "Widget Animation",
            NotificationManager.IMPORTANCE_MIN)
        chan.setShowBadge(false)
        getSystemService(NotificationManager::class.java).createNotificationChannel(chan)

        val notif = Notification.Builder(this, channelId).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle("Step Counter")
            setContentText("Updating widget")
            setOngoing(true)
        }.build()

        startForeground(2, notif)
        handler.post(tick)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

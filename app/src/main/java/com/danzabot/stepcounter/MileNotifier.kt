package com.danzabot.stepcounter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

object MileNotifier {

    private const val CHANNEL_ID = "mile_alerts"
    private var lastMileNotified = -1
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    fun init(ctx: Context) {
        createChannel(ctx)
        tts = TextToSpeech(ctx.applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts?.language = Locale.US
        }
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
    }

    fun check(ctx: Context, steps: Int) {
        val prefs = ctx.getSharedPreferences("step_counter_ui", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("mile_alerts", false)) { lastMileNotified = -1; return }

        val miles = (steps / 2000)
        if (miles > 0 && miles != lastMileNotified) {
            lastMileNotified = miles
            val alertType = prefs.getString("mile_alert_type", "sound") ?: "sound"
            val msg = "$miles mile${if (miles == 1) "" else "s"} reached!"
            when (alertType) {
                "voice" -> speakMile(msg)
                "silent" -> { /* notification only, no sound */ }
                else -> sendNotification(ctx, miles, msg, withSound = true)
            }
            if (alertType != "voice") sendNotification(ctx, miles, msg, withSound = alertType == "sound")
        }
    }

    private fun speakMile(msg: String) {
        if (!ttsReady) return
        tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "mile")
    }

    private fun sendNotification(ctx: Context, mile: Int, msg: String, withSound: Boolean) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Step Counter")
            .setContentText(msg)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        if (!withSound) builder.setSilent(true)
        nm.notify(mile, builder.build())
    }

    private fun createChannel(ctx: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Mile Alerts", NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when you reach a new mile"
        }
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}

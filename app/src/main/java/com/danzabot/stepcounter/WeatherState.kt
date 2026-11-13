package com.danzabot.stepcounter

import android.content.Context
import android.location.LocationManager
import java.net.URL
import java.util.Calendar

object WeatherState {

    @Volatile var weatherCode: Int = 0
    @Volatile var sunrise: String = "06:00"
    @Volatile var sunset: String = "20:00"
    @Volatile var fetched: Boolean = false

    val kind: String get() = Weather.kind(weatherCode)

    val isNight: Boolean
        get() {
            val cal = Calendar.getInstance()
            val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            return nowMin < hm(sunrise) || nowMin >= hm(sunset)
        }

    private fun hm(s: String): Int {
        val tp = s.substringAfterLast('T', s)
            .substringBefore('+').substringBefore('-').trim()
        val parts = tp.split(':')
        return if (parts.size >= 2)
            (parts[0].toIntOrNull() ?: 12) * 60 + (parts[1].toIntOrNull() ?: 0)
        else 720
    }

    fun fetch(ctx: Context) {
        if (fetched) return
        Thread {
            try {
                val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                @Suppress("MissingPermission")
                val loc = try {
                    lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                } catch (e: SecurityException) { null } ?: return@Thread

                val url = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${loc.latitude}&longitude=${loc.longitude}" +
                    "&current_weather=true&daily=sunrise,sunset" +
                    "&timezone=auto&forecast_days=1"
                )
                val json = url.readText()

                val code = Regex("\"weathercode\":\\s*(\\d+)")
                    .find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val sr = Regex("\"sunrise\":\\s*\\[\"([^\"]+)\"")
                    .find(json)?.groupValues?.get(1) ?: "06:00"
                val ss = Regex("\"sunset\":\\s*\\[\"([^\"]+)\"")
                    .find(json)?.groupValues?.get(1) ?: "20:00"

                weatherCode = code
                sunrise = sr
                sunset = ss
                fetched = true
            } catch (_: Exception) {}
        }.start()
    }

    fun reset() { fetched = false }
}

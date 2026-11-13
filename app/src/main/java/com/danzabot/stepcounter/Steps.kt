package com.danzabot.stepcounter

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

object Steps : SensorEventListener {

    private var sm: SensorManager? = null

    @Volatile var count = -1
        private set

    private var appCtx: Context? = null
    var onChanged: (() -> Unit)? = null

    fun granted(ctx: Context) = Build.VERSION.SDK_INT < 29 ||
        ctx.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    fun peekFromPrefs(ctx: Context) {
        if (count < 0) count = prefs(ctx).getInt("steps_shown", 0).coerceAtLeast(0)
    }

    fun start(ctx: Context) {
        if (sm != null || !granted(ctx)) return
        val app = ctx.applicationContext
        appCtx = app
        if (count < 0) count = prefs(app).getInt("steps_shown", -1)
        val m = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val s = m.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        m.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL)
        sm = m
    }

    fun stop() {
        sm?.unregisterListener(this)
        sm = null
    }

    fun isTripMode(ctx: Context) = prefs(ctx).getBoolean("trip_mode", false)

    fun startTrip(ctx: Context) {
        prefs(ctx).edit()
            .putBoolean("trip_mode", true)
            .putInt("steps_base", count.coerceAtLeast(0))
            .putInt("steps_carry", 0)
            .apply()
        onChanged?.invoke()
    }

    fun endTrip(ctx: Context) {
        val p = prefs(ctx)
        p.edit()
            .putBoolean("trip_mode", false)
            // reset for a fresh daily count
            .putString("steps_day", "")
            .apply()
        onChanged?.invoke()
    }

    override fun onSensorChanged(e: SensorEvent) {
        val ctx = appCtx ?: return
        val v = e.values[0].toInt()
        val st = prefs(ctx)
        val day = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        var base = st.getInt("steps_base", v)
        var last = st.getInt("steps_last", v)
        var carry = st.getInt("steps_carry", 0)
        val tripMode = st.getBoolean("trip_mode", false)
        val prevDay = st.getString("steps_day", "") ?: ""

        // In normal mode reset base at midnight; in trip mode only carry through reboots
        if (!tripMode && prevDay != day) {
            if (prevDay.isNotEmpty()) {
                val prevSteps = (carry + v - base).coerceAtLeast(0)
                saveDay(ctx, prevDay, prevSteps)
            }
            base = v; carry = 0
        } else if (v < last) {
            carry += last - base; base = 0
        }

        val steps = carry + v - base
        st.edit()
            .putString("steps_day", day)
            .putInt("steps_base", base)
            .putInt("steps_last", v)
            .putInt("steps_carry", carry)
            .putInt("steps_shown", steps)
            .apply()

        if (steps != count) {
            count = steps
            onChanged?.invoke()
        }

        saveTodayAsync(ctx, day, steps)
    }

    private var lastSaveMs = 0L

    private fun saveTodayAsync(ctx: Context, day: String, steps: Int) {
        val now = System.currentTimeMillis()
        if (now - lastSaveMs < 30_000) return
        lastSaveMs = now
        saveDay(ctx, day, steps)
    }

    private fun saveDay(ctx: Context, day: String, steps: Int) {
        val date = "${day.substring(0, 4)}-${day.substring(4, 6)}-${day.substring(6, 8)}"
        val miles = toMiles(steps)
        val cals = toCalories(steps)
        try {
            StepDatabase.get(ctx).stepDayDao().upsert(StepDay(date, steps, miles, cals))
        } catch (_: Exception) {}
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    // 2000 steps ≈ 1 mile (average adult stride)
    fun toMiles(steps: Int): Double = steps / 2000.0

    // ~0.04 kcal per step for average adult walking
    fun toCalories(steps: Int): Double = steps * 0.04

    // 3500 kcal deficit ≈ 1 pound of fat
    fun toPounds(calories: Double): Double = calories / 3500.0

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("step_counter_state", Context.MODE_PRIVATE)
}

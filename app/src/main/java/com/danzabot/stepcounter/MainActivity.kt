package com.danzabot.stepcounter

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.danzabot.stepcounter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            binding.stepView.refresh()
            if (Steps.count >= 0) MileNotifier.check(this@MainActivity, Steps.count)
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MileNotifier.init(this)

        Steps.onChanged = {
            runOnUiThread {
                binding.stepView.refresh()
                if (Steps.count >= 0) MileNotifier.check(this, Steps.count)
                StepCounterWidget.updateAll(this)
            }
        }

        binding.stepView.setOnClickListener {
            if (!Steps.granted(this) && Build.VERSION.SDK_INT >= 29)
                requestPermissions(arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION), 7)
        }

        binding.fabSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.fabHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        Steps.start(this)
        binding.stepView.refresh()
        handler.post(tick)
        maybeStartLiveWeather()
        startWidgetServiceIfNeeded()
    }

    private fun startWidgetServiceIfNeeded() {
        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, StepCounterWidget::class.java))
        if (ids.isNotEmpty()) {
            ContextCompat.startForegroundService(this,
                Intent(this, WidgetUpdateService::class.java))
        }
    }

    private fun maybeStartLiveWeather() {
        val scene = getSharedPreferences("step_counter_ui", MODE_PRIVATE)
            .getString("scene", "sun")
        if (scene != "live") return
        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            WeatherState.fetch(this)
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION), 8)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
        if (!isChangingConfigurations) {
            Steps.stop()
            StepCounterWidget.updateAll(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Steps.onChanged = null
        MileNotifier.destroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            7 -> { Steps.start(this); binding.stepView.refresh() }
            8 -> if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
                     WeatherState.fetch(this)
        }
    }
}

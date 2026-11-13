package com.danzabot.stepcounter

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.danzabot.stepcounter.databinding.ActivitySettingsBinding
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        prefs = getSharedPreferences("step_counter_ui", MODE_PRIVATE)

        setupGoal()
        setupBarStyle()
        setupWalker()
        setupStats()
        setupAlerts()
        setupScene()
        setupTrip()
    }

    // ── Goal (unlimited: EditText + slider up to 100k, manual entry for higher) ──

    private fun setupGoal() {
        val goal = prefs.getInt("goal", 10_000)
        binding.etGoal.setText(goal.toString())
        binding.seekGoal.max = 199   // 1k–100k in 500 increments
        binding.seekGoal.progress = ((goal - 1_000) / 500).coerceIn(0, 199)

        binding.seekGoal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (!fromUser) return
                val v = 1_000 + p * 500
                prefs.edit().putInt("goal", v).apply()
                binding.etGoal.setText(v.toString())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.etGoal.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val v = s?.toString()?.toIntOrNull() ?: return
                if (v < 1) return
                prefs.edit().putInt("goal", v).apply()
                val prog = ((v - 1_000) / 500).coerceIn(0, 199)
                binding.seekGoal.progress = prog
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    // ── Bar style ─────────────────────────────────────────────────────────────

    private fun setupBarStyle() {
        val style = prefs.getString("bar_style", "solid") ?: "solid"
        when (style) {
            "solid"    -> binding.radioSolid.isChecked = true
            "segments" -> binding.radioSegments.isChecked = true
            "dots"     -> binding.radioDots.isChecked = true
        }
        binding.radioGroupBar.setOnCheckedChangeListener { _, id ->
            prefs.edit().putString("bar_style", when (id) {
                R.id.radioSolid    -> "solid"
                R.id.radioSegments -> "segments"
                R.id.radioDots     -> "dots"
                else               -> "solid"
            }).apply()
        }
    }

    // ── Walker ────────────────────────────────────────────────────────────────

    private fun setupWalker() {
        binding.switchWalker.isChecked = prefs.getBoolean("walker", true)
        binding.switchWalker.setOnCheckedChangeListener { _, v ->
            prefs.edit().putBoolean("walker", v).apply()
        }

        val styles = WalkerStyle.values().map { it.label }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, styles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerWalkerStyle.adapter = adapter

        val current = prefs.getString("walker_style", "CLASSIC") ?: "CLASSIC"
        val idx = WalkerStyle.values().indexOfFirst { it.name == current }.coerceAtLeast(0)
        binding.spinnerWalkerStyle.setSelection(idx)

        binding.spinnerWalkerStyle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                prefs.edit().putString("walker_style", WalkerStyle.values()[pos].name).apply()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    // ── Stats display ─────────────────────────────────────────────────────────

    private fun setupStats() {
        binding.switchMiles.isChecked = prefs.getBoolean("show_miles", true)
        binding.switchMiles.setOnCheckedChangeListener { _, v ->
            prefs.edit().putBoolean("show_miles", v).apply()
        }
        binding.switchStats.isChecked = prefs.getBoolean("show_stats", true)
        binding.switchStats.setOnCheckedChangeListener { _, v ->
            prefs.edit().putBoolean("show_stats", v).apply()
        }
    }

    // ── Mile alerts ───────────────────────────────────────────────────────────

    private fun setupAlerts() {
        binding.switchAlerts.isChecked = prefs.getBoolean("mile_alerts", false)
        binding.switchAlerts.setOnCheckedChangeListener { _, v ->
            prefs.edit().putBoolean("mile_alerts", v).apply()
        }

        val alertTypes = listOf("Sound", "Voice", "Silent")
        val alertAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, alertTypes)
        alertAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAlertType.adapter = alertAdapter

        val currentType = prefs.getString("mile_alert_type", "sound") ?: "sound"
        val alertIdx = listOf("sound", "voice", "silent").indexOf(currentType).coerceAtLeast(0)
        binding.spinnerAlertType.setSelection(alertIdx)

        binding.spinnerAlertType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                prefs.edit().putString("mile_alert_type", listOf("sound", "voice", "silent")[pos]).apply()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    // ── Scene ─────────────────────────────────────────────────────────────────

    private val sceneKeys   = listOf("live", "vegas")
    private val sceneLabels = listOf("Live Weather", "Las Vegas Strip")

    private fun setupScene() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sceneLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerScene.adapter = adapter

        val current = prefs.getString("scene", "sun") ?: "sun"
        binding.spinnerScene.setSelection(sceneKeys.indexOf(current).coerceAtLeast(0))

        binding.spinnerScene.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                val key = sceneKeys[pos]
                prefs.edit().putString("scene", key).apply()
                if (key == "live") { WeatherState.reset(); WeatherState.fetch(this@SettingsActivity) }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    // ── Trip mode ─────────────────────────────────────────────────────────────

    private fun setupTrip() {
        val active = Steps.isTripMode(this)
        updateTripUI(active)

        binding.btnTripToggle.setOnClickListener {
            if (Steps.isTripMode(this)) {
                Steps.endTrip(this)
                updateTripUI(false)
                Toast.makeText(this, "Trip ended — back to daily tracking", Toast.LENGTH_SHORT).show()
            } else {
                Steps.startTrip(this)
                updateTripUI(true)
                Toast.makeText(this, "Trip started — steps accumulate until you end it", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTripUI(active: Boolean) {
        binding.btnTripToggle.text = if (active) "End Trip" else "Start Trip"
        binding.tvTripStatus.text = if (active)
            "Active — steps accumulate across days until you end the trip"
        else
            "Inactive — daily mode, steps reset at midnight"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}

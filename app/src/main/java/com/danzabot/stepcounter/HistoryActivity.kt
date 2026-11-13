package com.danzabot.stepcounter

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.danzabot.stepcounter.databinding.ActivityHistoryBinding
import com.google.android.material.tabs.TabLayout
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var dao: StepDayDao
    private val adapter = HistoryAdapter()
    private val nf = NumberFormat.getNumberInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        dao = StepDatabase.get(this).stepDayDao()

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Days"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Weeks"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Months"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Year"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = load(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) = load(tab.position)
        })

        saveToday()
        load(0)
    }

    private fun saveToday() {
        if (Steps.count > 0) {
            val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            dao.upsert(StepDay(day, Steps.count, Steps.toMiles(Steps.count), Steps.toCalories(Steps.count)))
        }
    }

    private fun load(tab: Int) {
        val items = when (tab) {
            0 -> loadDays()
            1 -> loadWeeks()
            2 -> loadMonths()
            3 -> loadYears()
            else -> emptyList()
        }

        adapter.items = items
        adapter.notifyDataSetChanged()

        val total = items.sumOf { it.steps.toLong() }
        val avg = if (items.isNotEmpty()) total / items.size else 0
        binding.tvSummary.text = "${nf.format(total)} total steps · ${nf.format(avg)} avg"

        binding.recycler.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadDays(): List<HistoryRow> {
        val days = dao.recent(90)
        val dfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dfOut = SimpleDateFormat("EEE, MMM d", Locale.US)
        return days.map { d ->
            val label = try { dfOut.format(dfIn.parse(d.date)!!) } catch (_: Exception) { d.date }
            HistoryRow(label, d.steps, "%.1f mi · %d cal".format(d.miles, d.calories.toInt()))
        }
    }

    private fun loadWeeks(): List<HistoryRow> {
        val all = dao.recent(365)
        if (all.isEmpty()) return emptyList()
        val dfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val weeks = mutableMapOf<String, MutableList<StepDay>>()
        for (d in all) {
            cal.time = dfIn.parse(d.date) ?: continue
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            weeks.getOrPut(key) { mutableListOf() }.add(d)
        }
        val dfLabel = SimpleDateFormat("MMM d", Locale.US)
        return weeks.entries.sortedByDescending { it.key }.map { (weekStart, days) ->
            val total = days.sumOf { it.steps }
            val label = "Week of ${dfLabel.format(dfIn.parse(weekStart)!!)}"
            HistoryRow(label, total, "${days.size} days · ${nf.format(total / days.size)} avg/day")
        }
    }

    private fun loadMonths(): List<HistoryRow> {
        val all = dao.recent(365)
        if (all.isEmpty()) return emptyList()
        val months = mutableMapOf<String, MutableList<StepDay>>()
        for (d in all) {
            val key = d.date.substring(0, 7)
            months.getOrPut(key) { mutableListOf() }.add(d)
        }
        val dfIn = SimpleDateFormat("yyyy-MM", Locale.US)
        val dfOut = SimpleDateFormat("MMMM yyyy", Locale.US)
        return months.entries.sortedByDescending { it.key }.map { (month, days) ->
            val total = days.sumOf { it.steps }
            val label = try { dfOut.format(dfIn.parse(month)!!) } catch (_: Exception) { month }
            HistoryRow(label, total, "${days.size} days · ${nf.format(total / days.size)} avg/day")
        }
    }

    private fun loadYears(): List<HistoryRow> {
        val all = dao.recent(3650)
        if (all.isEmpty()) return emptyList()
        val years = mutableMapOf<String, MutableList<StepDay>>()
        for (d in all) {
            val key = d.date.substring(0, 4)
            years.getOrPut(key) { mutableListOf() }.add(d)
        }
        return years.entries.sortedByDescending { it.key }.map { (year, days) ->
            val total = days.sumOf { it.steps }
            HistoryRow(year, total, "${days.size} days · ${nf.format(total / days.size)} avg/day")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}

data class HistoryRow(val label: String, val steps: Int, val detail: String)

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.VH>() {
    var items = listOf<HistoryRow>()
    private val nf = NumberFormat.getNumberInstance()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvDate: TextView = v.findViewById(R.id.tvDate)
        val tvSteps: TextView = v.findViewById(R.id.tvSteps)
        val tvDetail: TextView = v.findViewById(R.id.tvDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_step_day, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        holder.tvDate.text = row.label
        holder.tvSteps.text = nf.format(row.steps)
        holder.tvDetail.text = row.detail
    }

    override fun getItemCount() = items.size
}

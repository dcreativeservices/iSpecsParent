package com.ispecs.parent

import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.database.FirebaseDatabase
import com.ispecs.parent.databinding.ActivityLogsBriefBinding
import com.ispecs.parent.model.LogEntry
import java.text.SimpleDateFormat
import java.util.*

class LogsBriefActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogsBriefBinding
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var startDate: String = ""
    private var endDate: String = ""
    private var selectedFilter = "custom"
    private var childMac: String = ""
    private var parentId: String = ""
    private lateinit var filterButtons: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogsBriefBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Logs Summary"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        filterButtons = listOf(binding.last7, binding.last30, binding.prevYear, binding.currYear)

        initDefaultRange()

        val sharedPrefs = getSharedPreferences("MySharedPrefs", MODE_PRIVATE)
        parentId = sharedPrefs.getString("parentId", null) ?: return
        val selectedChildId = sharedPrefs.getString("selectedChildId", null)

        if (selectedChildId.isNullOrEmpty()) {
            Toast.makeText(this, "Selected child ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        FirebaseDatabase.getInstance().getReference("Children")
            .child(selectedChildId)
            .child("mac")
            .get()
            .addOnSuccessListener { snapshot ->
                childMac = snapshot.getValue(String::class.java) ?: ""
                if (childMac.isEmpty()) {
                    Toast.makeText(this, "MAC address not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                updateBatteryIconColor()
                setupQuickFilters()
                setupDatePicker()
                setupIconButtons()
                loadLogs(startDate, endDate)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch MAC", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun updateBatteryIconColor() {
        val today = dateFormatter.format(Date())
        FirebaseDatabase.getInstance()
            .getReference("logs")
            .child(parentId)
            .child(childMac)
            .child(today)
            .limitToLast(1)
            .get()
            .addOnSuccessListener { snapshot ->
                var latestBattery = -1
                for (data in snapshot.children) {
                    latestBattery = data.child("battery").getValue(Int::class.java) ?: -1
                }

                // Set fixed legend once
                binding.textViewBatteryLegend.text =
                    "iSpec Battery Legend:  🔴 ≤20%   •   🔵 21–80%   •   🟢 >80%"

                if (latestBattery != -1) {
                    val tintColor = when {
                        latestBattery <= 20 -> Color.RED
                        latestBattery <= 80 -> Color.BLUE
                        else -> Color.GREEN
                    }
                    binding.btnBatteryLogs.imageTintList = ColorStateList.valueOf(tintColor)
                    binding.textViewBatteryPercentage.text = "iSpec Current Battery: $latestBattery%"
                } else {
                    binding.textViewBatteryPercentage.text = "iSpec Battery % not available"
                }
            }
            .addOnFailureListener {
                binding.textViewBatteryPercentage.text = "Battery data fetch error"
            }
    }



    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupIconButtons() {
        binding.btnDetailedLogs.setOnClickListener {
            val intent = Intent(this, DailyLogActivity::class.java).apply {
                putExtra("startDate", startDate)
                putExtra("endDate", endDate)
                putExtra("filterType", selectedFilter)
                putExtra("childMac", childMac)
            }
            startActivity(intent)
        }

        binding.btnBatteryLogs.setOnClickListener {
            val intent = Intent(this, BatteryLogActivity::class.java).apply {
                putExtra("startDate", startDate)
                putExtra("endDate", endDate)
                putExtra("childMac", childMac)
                putExtra("parentId", parentId)
            }
            startActivity(intent)
        }

        binding.btnRawLogs.setOnClickListener {
            val intent = Intent(this, RawLogsActivity::class.java).apply {
                putExtra("startDate", startDate)
                putExtra("endDate", endDate)
                putExtra("childMac", childMac)
            }
            startActivity(intent)
        }
    }

    private fun initDefaultRange() {
        val cal = Calendar.getInstance()
        val today = dateFormatter.format(cal.time)
        startDate = today
        endDate = today
        selectedFilter = "custom"
        updateDateRangeText()
    }

    private fun setupQuickFilters() {
        binding.last7.setOnClickListener {
            resetStyles()
            selectedFilter = "last7"
            styleSelected(binding.last7)
            setRangeByDays(7)
        }

        binding.last30.setOnClickListener {
            resetStyles()
            selectedFilter = "last30"
            styleSelected(binding.last30)
            setRangeByDays(30)
        }

        binding.prevYear.setOnClickListener {
            resetStyles()
            selectedFilter = "prevYear"
            styleSelected(binding.prevYear)
            val fyStart = "${Calendar.getInstance().get(Calendar.YEAR) - 1}-04-01"
            val fyEnd = "${Calendar.getInstance().get(Calendar.YEAR)}-03-31"
            startDate = fyStart
            endDate = fyEnd
            updateDateRangeText()
            loadLogs(startDate, endDate)
        }

        binding.currYear.setOnClickListener {
            resetStyles()
            selectedFilter = "currYear"
            styleSelected(binding.currYear)
            val fyStart = "${Calendar.getInstance().get(Calendar.YEAR)}-04-01"
            val fyEnd = dateFormatter.format(Date())
            startDate = fyStart
            endDate = fyEnd
            updateDateRangeText()
            loadLogs(startDate, endDate)
        }
    }

    private fun resetStyles() {
        filterButtons.forEach {
            it.setBackgroundResource(R.drawable.filter_chip_unselected)
            it.setTextColor(ContextCompat.getColor(this, R.color.primary))
        }
    }

    private fun styleSelected(view: TextView) {
        view.setBackgroundResource(R.drawable.filter_chip_selected)
        view.setTextColor(ContextCompat.getColor(this, R.color.white))
    }

    private fun setRangeByDays(days: Int) {
        val cal = Calendar.getInstance()
        endDate = dateFormatter.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, -days)
        startDate = dateFormatter.format(cal.time)
        updateDateRangeText()
        loadLogs(startDate, endDate)
    }

    private fun setupDatePicker() {
        binding.textViewDateRange.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                val fromCal = Calendar.getInstance().apply { set(year, month, day) }
                startDate = dateFormatter.format(fromCal.time)

                DatePickerDialog(this, { _, toYear, toMonth, toDay ->
                    val toCal = Calendar.getInstance().apply { set(toYear, toMonth, toDay) }
                    endDate = dateFormatter.format(toCal.time)
                    selectedFilter = "custom"
                    resetStyles()
                    updateDateRangeText()
                    loadLogs(startDate, endDate)
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()

            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun updateDateRangeText() {
        binding.textViewDateRange.text = "$startDate — $endDate"
    }

    private fun loadLogs(start: String, end: String) {
        binding.progressOverlay.visibility = View.VISIBLE

        val startDateObj = dateFormatter.parse(start) ?: return
        val endDateObj = dateFormatter.parse(end) ?: return

        val cal = Calendar.getInstance().apply { time = startDateObj }
        val datesToFetch = mutableListOf<String>()
        while (!cal.time.after(endDateObj)) {
            datesToFetch.add(dateFormatter.format(cal.time))
            cal.add(Calendar.DATE, 1)
        }

        var fetchedCount = 0
        var totalActive = 0L
        var totalInactive = 0L
        val barEntries = mutableListOf<BarEntry>()
        val dateLabels = mutableListOf<String>()

        for ((index, date) in datesToFetch.withIndex()) {
            val dbRef = FirebaseDatabase.getInstance()
                .getReference("logs")
                .child(parentId)
                .child(childMac)
                .child(date)

            dbRef.get().addOnSuccessListener { snapshot ->
                val rawLogs = snapshot.children.mapNotNull {
                    val time = it.child("uploaded_at").getValue(String::class.java)
                    val status = it.child("status").getValue(Int::class.java)
                    if (time != null && status != null) LogEntry(-1, status, time, emptyMap()) else null
                }.sortedBy { it.time }

                var currentLabel: String? = null
                var groupStart: Long? = null
                var groupEnd: Long? = null

                var dailyActive = 0L
                var dailyInactive = 0L

                for (i in 0 until rawLogs.size - 1) {
                    val curr = rawLogs[i]
                    val next = rawLogs[i + 1]

                    val startTime = dateTimeFormat.parse("$date ${curr.time}")?.time ?: continue
                    val endTime = dateTimeFormat.parse("$date ${next.time}")?.time ?: continue
                    val label = if (curr.status == 1) "Active" else "Inactive"

                    if (currentLabel == null) {
                        currentLabel = label
                        groupStart = startTime
                    }

                    if (label == currentLabel) {
                        groupEnd = endTime
                    } else {
                        val duration = (groupEnd ?: endTime) - (groupStart ?: startTime)
                        if (currentLabel == "Active") dailyActive += duration else dailyInactive += duration

                        currentLabel = label
                        groupStart = startTime
                        groupEnd = endTime
                    }
                }

                if (rawLogs.isNotEmpty() && groupStart != null && currentLabel != null) {
                    val last = rawLogs.last()
                    val lastTime = dateTimeFormat.parse("$date ${last.time}")?.time ?: 0L
                    val duration = lastTime - groupStart
                    if (currentLabel == "Active") dailyActive += duration else dailyInactive += duration
                }

                totalActive += dailyActive
                totalInactive += dailyInactive

                val dailyActiveMinutes = dailyActive / 60000f
                barEntries.add(BarEntry(index.toFloat(), dailyActiveMinutes))
                dateLabels.add(date.substring(5))

                fetchedCount++
                if (fetchedCount == datesToFetch.size) {
                    binding.progressOverlay.visibility = View.GONE
                    binding.totalOnTime.text = if (totalActive == 0L && totalInactive == 0L) {
                        "No logs available for selected range"
                    } else {
                        "👓 Wearing Glasses: ${formatDuration(totalActive)}\n👁️ Not Wearing: ${formatDuration(totalInactive)}"
                    }
                    showBarChart(barEntries, dateLabels)
                }
            }.addOnFailureListener {
                fetchedCount++
                if (fetchedCount == datesToFetch.size) {
                    binding.progressOverlay.visibility = View.GONE
                }
            }
        }
    }

    private fun showBarChart(entries: List<BarEntry>, labels: List<String>) {
        val dataSet = BarDataSet(entries, "Wearing Duration (h m)").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextColor = ContextCompat.getColor(this@LogsBriefActivity, R.color.black)
            valueTextSize = 12f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val mins = value.toInt()
                    val hrs = mins / 60
                    val remMins = mins % 60
                    return if (hrs > 0) "${hrs}h ${remMins}m" else "${remMins}m"
                }
            }
        }

        val barData = BarData(dataSet)
        binding.barChart.data = barData

        binding.barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(labels)
            setDrawGridLines(false)
            granularity = 1f
            labelRotationAngle = -45f
        }

        binding.barChart.axisLeft.apply {
            granularity = 1f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val mins = value.toInt()
                    val hrs = mins / 60
                    val remMins = mins % 60
                    return if (hrs > 0) "${hrs}h\n${remMins}m" else "${remMins}m"
                }
            }
        }

        binding.barChart.axisRight.isEnabled = false
        binding.barChart.description = Description().apply { text = "Daily Wearing Time" }
        binding.barChart.invalidate()
    }

    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m ${secs}s"
    }
}

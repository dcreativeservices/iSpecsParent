
package com.ispecs.parent

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.FirebaseDatabase
import com.ispecs.parent.databinding.ActivityDailyLogBinding
import com.ispecs.parent.model.DailyLogEntry
import com.ispecs.parent.model.LogEntry
import com.ispecs.parent.ui.ispecs.LogsTableAdapter
import java.text.SimpleDateFormat
import java.util.*

class DailyLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyLogBinding
    private val logList = mutableListOf<DailyLogEntry>()
    private lateinit var adapter: LogsTableAdapter

    private var startDate: String = ""
    private var endDate: String = ""
    private var selectedFilter: String = "last30"

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private var parentId: String = ""
    private var childMac: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Daily Logs"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        adapter = LogsTableAdapter(logList)
        binding.logsTableRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.logsTableRecyclerView.adapter = adapter

        startDate = intent.getStringExtra("startDate") ?: ""
        endDate = intent.getStringExtra("endDate") ?: ""
        selectedFilter = intent.getStringExtra("filterType") ?: "last30"

        val sharedPrefs = getSharedPreferences("MySharedPrefs", MODE_PRIVATE)
        parentId = sharedPrefs.getString("parentId", null) ?: return
        val childId = sharedPrefs.getString("selectedChildId", null)

        if (childId.isNullOrEmpty()) {
            Toast.makeText(this, "No child selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 🔁 Fetch MAC address from Firebase
        FirebaseDatabase.getInstance().getReference("Children")
            .child(childId)
            .child("mac")
            .get()
            .addOnSuccessListener { snapshot ->
                childMac = snapshot.getValue(String::class.java) ?: ""

                if (childMac.isEmpty()) {
                    Toast.makeText(this, "MAC not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                setupQuickFilters()
                setupRangeClick()

                if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                    updateDateRangeText()
                    loadLogs(startDate, endDate)
                } else {
                    setDefaultRange()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch MAC", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setDefaultRange() {
        val cal = Calendar.getInstance()
        endDate = dateFormatter.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, -30)
        startDate = dateFormatter.format(cal.time)
        updateDateRangeText()
        loadLogs(startDate, endDate)
    }

    private fun setupQuickFilters() {
        val last7 = findViewById<TextView>(R.id.last7)
        val last30 = findViewById<TextView>(R.id.last30)
        val prevYear = findViewById<TextView>(R.id.prevYear)
        val currYear = findViewById<TextView>(R.id.currYear)

        val filters = listOf(last7, last30, prevYear, currYear)

        fun selectChip(selected: TextView) {
            filters.forEach { chip ->
                if (chip == selected) {
                    chip.setBackgroundResource(R.drawable.filter_chip_selected)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.white))
                } else {
                    chip.setBackgroundResource(R.drawable.filter_chip_unselected)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.primary))
                }
            }
        }

        last7.setOnClickListener {
            selectedFilter = "last7"
            selectChip(last7)
            setRangeByDays(7)
        }

        last30.setOnClickListener {
            selectedFilter = "last30"
            selectChip(last30)
            setRangeByDays(30)
        }

        prevYear.setOnClickListener {
            selectedFilter = "prevYear"
            selectChip(prevYear)
            val fyStart = "${Calendar.getInstance().get(Calendar.YEAR) - 1}-04-01"
            val fyEnd = "${Calendar.getInstance().get(Calendar.YEAR)}-03-31"
            startDate = fyStart
            endDate = fyEnd
            updateDateRangeText()
            loadLogs(startDate, endDate)
        }

        currYear.setOnClickListener {
            selectedFilter = "currYear"
            selectChip(currYear)
            val fyStart = "${Calendar.getInstance().get(Calendar.YEAR)}-04-01"
            val fyEnd = dateFormatter.format(Date())
            startDate = fyStart
            endDate = fyEnd
            updateDateRangeText()
            loadLogs(startDate, endDate)
        }

        when (selectedFilter) {
            "last7" -> last7.performClick()
            "last30" -> last30.performClick()
            "prevYear" -> prevYear.performClick()
            "currYear" -> currYear.performClick()
        }
    }

    private fun setRangeByDays(days: Int) {
        val cal = Calendar.getInstance()
        endDate = dateFormatter.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, -days)
        startDate = dateFormatter.format(cal.time)
        updateDateRangeText()
        loadLogs(startDate, endDate)
    }

    private fun setupRangeClick() {
        binding.textViewDateRange.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val fromCal = Calendar.getInstance()
                fromCal.set(year, month, dayOfMonth)
                startDate = dateFormatter.format(fromCal.time)

                DatePickerDialog(this, { _, toYear, toMonth, toDay ->
                    val toCal = Calendar.getInstance()
                    toCal.set(toYear, toMonth, toDay)
                    endDate = dateFormatter.format(toCal.time)

                    selectedFilter = "custom"
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

        val cal = Calendar.getInstance()
        cal.time = startDateObj
        val datesToFetch = mutableListOf<String>()
        while (!cal.time.after(endDateObj)) {
            datesToFetch.add(dateFormatter.format(cal.time))
            cal.add(Calendar.DATE, 1)
        }

        logList.clear()
        var fetchedDays = 0
        var totalActive = 0L
        var totalInactive = 0L

        for (date in datesToFetch) {
            Log.d("FIREBASE_PATH>>>>>>>>>>>>>>>>", "Path: logs/$parentId/$childMac/$date")
            val dbRef = FirebaseDatabase.getInstance().getReference("logs").child(parentId).child(childMac).child(date)

            dbRef.get().addOnSuccessListener { snapshot ->
                val rawLogs = mutableListOf<LogEntry>()
                for (child in snapshot.children) {
                    val time = child.child("uploaded_at").getValue(String::class.java) ?: continue
                    val status = child.child("status").getValue(Int::class.java) ?: 0
                    val battery = child.child("battery").getValue(Int::class.java) ?: -1
                    rawLogs.add(LogEntry(battery, status, time, emptyMap()))
                }

                rawLogs.sortBy { it.time }

                val merged = mutableListOf<DailyLogEntry>()
                var currentLabel: String? = null
                var tempStart: String? = null
                var tempEnd: String? = null
                var groupStartTime: Long? = null
                var groupEndTime: Long? = null

                for (i in 0 until rawLogs.size - 1) {
                    val current = rawLogs[i]
                    val next = rawLogs[i + 1]

                    val startDateTime = dateTimeFormat.parse("$date ${current.time}") ?: continue
                    val endDateTime = dateTimeFormat.parse("$date ${next.time}") ?: continue
                    val label = if (current.status == 1) "Active" else "Inactive"

                    if (currentLabel == null) {
                        currentLabel = label
                        tempStart = current.time
                        groupStartTime = startDateTime.time
                    }

                    if (label == currentLabel) {
                        tempEnd = next.time
                        groupEndTime = endDateTime.time
                    } else {
                        val duration = (groupEndTime ?: endDateTime.time) - (groupStartTime ?: startDateTime.time)
                        merged.add(DailyLogEntry(tempStart ?: "", tempEnd ?: "", formatDuration(duration), currentLabel, date))
                        if (currentLabel == "Active") totalActive += duration else totalInactive += duration

                        currentLabel = label
                        tempStart = current.time
                        tempEnd = next.time
                        groupStartTime = startDateTime.time
                        groupEndTime = endDateTime.time
                    }
                }

                if (tempStart != null && tempEnd != null && groupStartTime != null && groupEndTime != null && currentLabel != null) {
                    val totalDuration = groupEndTime - groupStartTime
                    merged.add(DailyLogEntry(tempStart, tempEnd, formatDuration(totalDuration), currentLabel, date))
                    if (currentLabel == "Active") totalActive += totalDuration else totalInactive += totalDuration
                }

                logList.addAll(merged)
                fetchedDays++

                if (fetchedDays == datesToFetch.size) {
                    adapter.notifyDataSetChanged()
                    binding.textViewSummary.text = "👓 Active: ${formatDuration(totalActive)} | 👁️ Inactive: ${formatDuration(totalInactive)}"
                    binding.progressOverlay.visibility = View.GONE

                    if (logList.isEmpty()) {
                        Toast.makeText(this, "No logs found for selected range", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                fetchedDays++
                if (fetchedDays == datesToFetch.size) {
                    binding.progressOverlay.visibility = View.GONE
                }
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}

package com.ispecs.parent

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = LogsTableAdapter(logList)
        binding.logsTableRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.logsTableRecyclerView.adapter = adapter

        startDate = intent.getStringExtra("startDate") ?: ""
        endDate = intent.getStringExtra("endDate") ?: ""

        if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
            updateDateRangeText()
            loadLogs(startDate, endDate)
        } else {
            setDefaultRange()
        }

        setupQuickFilters()
        setupRangeClick()
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
        findViewById<TextView>(R.id.last7).setOnClickListener { setRangeByDays(7) }
        findViewById<TextView>(R.id.last30).setOnClickListener { setRangeByDays(30) }
        findViewById<TextView>(R.id.prevYear).setOnClickListener {
            val fyStart = "${Calendar.getInstance().get(Calendar.YEAR) - 1}-04-01"
            val fyEnd = "${Calendar.getInstance().get(Calendar.YEAR)}-03-31"
            startDate = fyStart
            endDate = fyEnd
            updateDateRangeText()
            loadLogs(startDate, endDate)
        }
        findViewById<TextView>(R.id.currYear).setOnClickListener {
            val fyStart = "${Calendar.getInstance().get(Calendar.YEAR)}-04-01"
            val fyEnd = dateFormatter.format(Date())
            startDate = fyStart
            endDate = fyEnd
            updateDateRangeText()
            loadLogs(startDate, endDate)
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
            showCustomRangeDialog()
        }
    }

    private fun showCustomRangeDialog() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val fromCal = Calendar.getInstance()
            fromCal.set(year, month, dayOfMonth)
            startDate = dateFormatter.format(fromCal.time)

            DatePickerDialog(this, { _, toYear, toMonth, toDay ->
                val toCal = Calendar.getInstance()
                toCal.set(toYear, toMonth, toDay)
                endDate = dateFormatter.format(toCal.time)

                updateDateRangeText()
                loadLogs(startDate, endDate)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()

        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDateRangeText() {
        binding.textViewDateRange.text = "$startDate — $endDate"
    }

    private fun loadLogs(start: String, end: String) {
        val sharedPrefs = getSharedPreferences("MySharedPrefs", MODE_PRIVATE)
        val parentId = sharedPrefs.getString("parentId", null) ?: return

        binding.progressBar.visibility = View.VISIBLE

        val cal = Calendar.getInstance()
        val startDateObj = dateFormatter.parse(start) ?: return
        val endDateObj = dateFormatter.parse(end) ?: return

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
            val dbRef = FirebaseDatabase.getInstance().getReference("logs").child(parentId).child(date)
            dbRef.get().addOnSuccessListener { snapshot ->
                val rawLogs = mutableListOf<LogEntry>()

                for (child in snapshot.children) {
                    val time = child.child("uploaded_at").getValue(String::class.java) ?: continue
                    val status = child.child("status").getValue(Int::class.java) ?: 0
                    val battery = child.child("battery").getValue(Int::class.java) ?: -1
                    rawLogs.add(LogEntry(battery, status, time, emptyMap()))
                }

                val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                rawLogs.sortBy { it.time?.let { t -> format.parse(t) } }

                val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val merged = mutableListOf<DailyLogEntry>()

                var tempStart: String? = null
                var tempEnd: String? = null
                var currentLabel: String? = null
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
                        val totalDuration = (groupEndTime ?: endDateTime.time) - (groupStartTime ?: startDateTime.time)
                        merged.add(DailyLogEntry(tempStart ?: "", tempEnd ?: current.time ?: "", formatDuration(totalDuration), currentLabel, date))
                        if (currentLabel == "Active") totalActive += totalDuration else totalInactive += totalDuration

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
                    binding.textViewSummary.text = "Active: ${formatDuration(totalActive)} | Inactive: ${formatDuration(totalInactive)}"
                    binding.progressBar.visibility = View.GONE

                    if (logList.isEmpty()) {
                        Toast.makeText(this, "No logs found for selected range", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                fetchedDays++
                if (fetchedDays == datesToFetch.size) {
                    binding.progressBar.visibility = View.GONE
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

package com.ispecs.parent

import android.app.DatePickerDialog
import android.os.Bundle
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

        setDefaultRange()
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
        findViewById<TextView>(R.id.last7).setOnClickListener {
            setRangeByDays(7)
        }
        findViewById<TextView>(R.id.last30).setOnClickListener {
            setRangeByDays(30)
        }
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

                var prevStatus: Int? = null
                var startTime: String? = null
                for (i in rawLogs.indices) {
                    val curr = rawLogs[i]
                    val currStatus = curr.status
                    val currTime = curr.time ?: continue

                    if (startTime == null) {
                        startTime = currTime
                        prevStatus = currStatus
                        continue
                    }

                    val sameGroup = (prevStatus == currStatus) || (prevStatus != 1 && currStatus != 1)

                    if (!sameGroup || i == rawLogs.lastIndex) {
                        val endTime = currTime
                        val df = format
                        val startDate = df.parse(startTime) ?: continue
                        val endDate = df.parse(endTime) ?: startDate
                        val durationMillis = endDate.time - startDate.time

                        val label = if (prevStatus == 1) "Active" else "Inactive"
                        logList.add(DailyLogEntry(startTime, endTime, formatDuration(durationMillis), label))

                        if (label == "Active") totalActive += durationMillis else totalInactive += durationMillis

                        startTime = currTime
                        prevStatus = currStatus
                    }
                }

                fetchedDays++
                if (fetchedDays == datesToFetch.size) {
                    adapter.notifyDataSetChanged()
                    binding.textViewSummary.text = "Active: ${formatDuration(totalActive)} | Inactive: ${formatDuration(totalInactive)}"
                }
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val minutes = millis / 1000 / 60
        val seconds = (millis / 1000) % 60
        return "${minutes}m ${seconds}s"
    }
}

package com.ispecs.parent

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.ispecs.parent.databinding.ActivityLogsBriefBinding
import com.ispecs.parent.model.LogEntry
import com.ispecs.parent.model.StatusDuration
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class LogsBriefActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogsBriefBinding
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var startDate: String = ""
    private var endDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogsBriefBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Logs Summary"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        initDefaultRange()
        setupQuickFilters()
        setupDatePicker()

        binding.viewDetailedLogsBtn.setOnClickListener {
            val intent = Intent(this, DailyLogActivity::class.java).apply {
                putExtra("startDate", startDate)
                putExtra("endDate", endDate)
            }
            startActivity(intent)
        }

        binding.viewBatteryLogsBtn.setOnClickListener {
            val intent = Intent(this, BatteryLogActivity::class.java).apply {
                putExtra("startDate", startDate)
                putExtra("endDate", endDate)
            }
            startActivity(intent)
        }

        binding.viewRawLogsBtn.setOnClickListener {
            val intent = Intent(this, RawLogsActivity::class.java).apply {
                putExtra("startDate", startDate)
                putExtra("endDate", endDate)
            }
            startActivity(intent)
        }

        loadAndSummarizeLogs()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun initDefaultRange() {
        val cal = Calendar.getInstance()
        val today = dateFormatter.format(cal.time)
        startDate = today
        endDate = today
        updateDateRangeText()
    }

    private fun setupQuickFilters() {
        binding.last7.setOnClickListener { setRangeByDays(7) }
        binding.last30.setOnClickListener { setRangeByDays(30) }
        binding.prevYear.setOnClickListener {
            val fyStart = "${Calendar.getInstance().get(Calendar.YEAR) - 1}-04-01"
            val fyEnd = "${Calendar.getInstance().get(Calendar.YEAR)}-03-31"
            startDate = fyStart
            endDate = fyEnd
            updateDateRangeText()
            loadAndSummarizeLogs()
        }
        binding.currYear.setOnClickListener {
            val fyStart = "${Calendar.getInstance().get(Calendar.YEAR)}-04-01"
            val fyEnd = dateFormatter.format(Date())
            startDate = fyStart
            endDate = fyEnd
            updateDateRangeText()
            loadAndSummarizeLogs()
        }
    }

    private fun setRangeByDays(days: Int) {
        val cal = Calendar.getInstance()
        endDate = dateFormatter.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, -days)
        startDate = dateFormatter.format(cal.time)
        updateDateRangeText()
        loadAndSummarizeLogs()
    }

    private fun setupDatePicker() {
        binding.textViewDateRange.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                val fromCal = Calendar.getInstance()
                fromCal.set(year, month, day)
                startDate = dateFormatter.format(fromCal.time)

                DatePickerDialog(this, { _, toYear, toMonth, toDay ->
                    val toCal = Calendar.getInstance()
                    toCal.set(toYear, toMonth, toDay)
                    endDate = dateFormatter.format(toCal.time)
                    updateDateRangeText()
                    loadAndSummarizeLogs()
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun updateDateRangeText() {
        binding.textViewDateRange.text = "$startDate — $endDate"
    }

    private fun loadAndSummarizeLogs() {
        val sharedPrefs = getSharedPreferences("MySharedPrefs", MODE_PRIVATE)
        val parentId = sharedPrefs.getString("parentId", null) ?: return

        val cal = Calendar.getInstance()
        val startDateObj = dateFormatter.parse(startDate) ?: return
        val endDateObj = dateFormatter.parse(endDate) ?: return

        cal.time = startDateObj
        val datesToFetch = mutableListOf<String>()
        while (!cal.time.after(endDateObj)) {
            datesToFetch.add(dateFormatter.format(cal.time))
            cal.add(Calendar.DATE, 1)
        }

        val allLogs = mutableListOf<LogEntry>()
        var fetchedCount = 0

        for (date in datesToFetch) {
            val dbRef = FirebaseDatabase.getInstance().getReference("logs").child(parentId).child(date)
            dbRef.get().addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val time = child.child("uploaded_at").getValue(String::class.java)?.let {
                        if (it.contains(":")) "$date $it" else it
                    } ?: continue
                    val status = child.child("status").getValue(Int::class.java) ?: 0
                    val battery = child.child("battery").getValue(Int::class.java) ?: -1
                    allLogs.add(LogEntry(battery, status, time, emptyMap()))
                }
                fetchedCount++
                if (fetchedCount == datesToFetch.size) updateSummary(allLogs)
            }.addOnFailureListener {
                fetchedCount++
                if (fetchedCount == datesToFetch.size) updateSummary(allLogs)
            }
        }
    }

    private fun updateSummary(logsData: List<LogEntry>) {
        val filteredLogs = logsData.filter { entry ->
            val datePart = entry.time?.split(" ")?.getOrNull(0) ?: return@filter false
            datePart in startDate..endDate
        }.sortedBy { it.time }

        val statusDurations = mutableListOf<StatusDuration>()

        if (filteredLogs.isNotEmpty()) {
            var startTime = filteredLogs.first().time
            var currentState = filteredLogs.first().status

            for (i in 1 until filteredLogs.size) {
                val entry = filteredLogs[i]
                if (entry.status != currentState) {
                    val durationInSeconds = calculateTimeDifferenceInSeconds(startTime, entry.time)
                    statusDurations.add(StatusDuration(
                        state = if (currentState == 1) "On" else "Off",
                        startTime = startTime ?: "",
                        endTime = entry.time ?: "",
                        durationInSeconds = durationInSeconds
                    ))
                    startTime = entry.time
                    currentState = entry.status
                }
            }

            val lastEntry = filteredLogs.last()
            val durationInSeconds = calculateTimeDifferenceInSeconds(startTime, lastEntry.time)
            statusDurations.add(StatusDuration(
                state = if (currentState == 1) "On" else "Off",
                startTime = startTime ?: "",
                endTime = lastEntry.time ?: "",
                durationInSeconds = durationInSeconds
            ))
        }

        val totalOnTime = statusDurations.filter { it.state == "On" }.sumOf { it.durationInSeconds }
        val totalOffTime = statusDurations.filter { it.state == "Off" }.sumOf { it.durationInSeconds }

        binding.totalOnTime.text = buildString {
            if (totalOnTime == 0L && totalOffTime == 0L) {
                append("No logs available for selected range")
            } else {
                if (totalOnTime > 0) {
                    append("Child was wearing glasses for ${formatDuration(totalOnTime)}\n")
                } else {
                    append("Child was not wearing glasses\n")
                }
                if (totalOffTime > 0) {
                    append("Not wearing glasses for ${formatDuration(totalOffTime)}")
                }
            }
        }
    }

    private fun calculateTimeDifferenceInSeconds(startTime: String?, endTime: String?): Long {
        return try {
            val start = timeFormatter.parse(startTime?.split(" ")?.getOrNull(1) ?: "") ?: return 0
            val end = timeFormatter.parse(endTime?.split(" ")?.getOrNull(1) ?: "") ?: return 0
            TimeUnit.MILLISECONDS.toSeconds(end.time - start.time)
        } catch (e: Exception) {
            0
        }
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return listOfNotNull(
            if (hours > 0) "$hours hour${if (hours > 1) "s" else ""}" else null,
            if (minutes > 0) "$minutes minute${if (minutes > 1) "s" else ""}" else null
        ).joinToString(" ")
    }
}

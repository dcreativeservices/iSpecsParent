package com.ispecs.parent

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

        // 🔁 Get MAC using selectedChildId from Firebase
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupIconButtons() {
        findViewById<ImageButton>(R.id.btnDetailedLogs).setOnClickListener {
            val intent = Intent(this, DailyLogActivity::class.java).apply {
                putExtra("startDate", startDate)
                putExtra("endDate", endDate)
                putExtra("filterType", selectedFilter)
                putExtra("childMac", childMac)
            }
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.btnBatteryLogs).setOnClickListener {
            val intent = Intent(this, BatteryLogActivity::class.java).apply {
                putExtra("startDate", startDate)
                putExtra("endDate", endDate)
                putExtra("childMac", childMac)
            }
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.btnRawLogs).setOnClickListener {
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
        fun resetStyles() {
            filterButtons.forEach {
                it.setBackgroundResource(R.drawable.filter_chip_unselected)
                it.setTextColor(ContextCompat.getColor(this, R.color.primary))
            }
        }

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
                val fromCal = Calendar.getInstance()
                fromCal.set(year, month, day)
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

        var fetchedCount = 0
        var totalActive = 0L
        var totalInactive = 0L
        for (date in datesToFetch) {
            Log.d("LOG_PATH**********************", "Firebase path: logs/$parentId/$childMac/$date")
            val dbRef = FirebaseDatabase.getInstance()
                .getReference("logs")
                .child(parentId)
                .child(childMac)
                .child(date)

            dbRef.get().addOnSuccessListener { snapshot ->
                val rawLogs = mutableListOf<LogEntry>()
                for (child in snapshot.children) {
                    val time = child.child("uploaded_at").getValue(String::class.java) ?: continue
                    val status = child.child("status").getValue(Int::class.java) ?: continue
                    rawLogs.add(LogEntry(-1, status, time, emptyMap()))
                }

                rawLogs.sortBy { it.time }

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
                        groupStartTime = startDateTime.time
                    }

                    if (label == currentLabel) {
                        groupEndTime = endDateTime.time
                    } else {
                        val duration = (groupEndTime ?: endDateTime.time) - (groupStartTime ?: startDateTime.time)
                        if (currentLabel == "Active") totalActive += duration else totalInactive += duration

                        currentLabel = label
                        groupStartTime = startDateTime.time
                        groupEndTime = endDateTime.time
                    }
                }

                if (rawLogs.isNotEmpty()) {
                    val last = rawLogs.last()
                    val lastTime = dateTimeFormat.parse("$date ${last.time}")?.time ?: 0L

                    if (groupStartTime != null && currentLabel != null) {
                        val duration = lastTime - groupStartTime
                        if (currentLabel == "Active") totalActive += duration else totalInactive += duration
                    }
                }

                fetchedCount++
                if (fetchedCount == datesToFetch.size) {
                    binding.progressOverlay.visibility = View.GONE
                    binding.totalOnTime.text = if (totalActive == 0L && totalInactive == 0L) {
                        "No logs available for selected range"
                    } else {
                        "👓 Wearing Glasses: ${formatDuration(totalActive)}\n👁️ Not Wearing: ${formatDuration(totalInactive)}"
                    }
                }
            }.addOnFailureListener {
                fetchedCount++
                if (fetchedCount == datesToFetch.size) {
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
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m ${seconds}s"
    }
}

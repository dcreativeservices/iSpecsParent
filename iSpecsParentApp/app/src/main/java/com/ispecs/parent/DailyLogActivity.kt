package com.ispecs.parent

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = LogsTableAdapter(logList)
        binding.logsTableRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.logsTableRecyclerView.adapter = adapter

        val today = Calendar.getInstance()
        val todayString = "%04d-%02d-%02d".format(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.DAY_OF_MONTH)
        )
        binding.textViewSelectedDate.text = todayString
        loadLogsForDate(todayString)

        binding.textViewSelectedDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val date = "%04d-%02d-%02d".format(year, month + 1, day)
            binding.textViewSelectedDate.text = date
            loadLogsForDate(date)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadLogsForDate(date: String) {
        val sharedPrefs = getSharedPreferences("MySharedPrefs", MODE_PRIVATE)
        val parentId = sharedPrefs.getString("parentId", null)

        if (parentId.isNullOrEmpty()) {
            Toast.makeText(this, "Parent ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        val dbRef = FirebaseDatabase.getInstance().getReference("logs").child(parentId).child(date)
        logList.clear()

        dbRef.get().addOnSuccessListener { snapshot ->
            val rawLogs = mutableListOf<LogEntry>()

            for (child in snapshot.children) {
                val time = child.child("uploaded_at").getValue(String::class.java) ?: continue
                val status = child.child("status").getValue(Int::class.java) ?: 0
                val battery = child.child("battery").getValue(Int::class.java) ?: -1
                rawLogs.add(LogEntry(battery, status, time, emptyMap()))
            }

            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            rawLogs.sortBy { log -> dateFormat.parse(log.time ?: "00:00:00") }

            // ✅ Merge consecutive logs
            val merged = mutableListOf<DailyLogEntry>()
            var startTime: String? = null
            var prevStatus: Int? = null

            var totalActiveMillis = 0L
            var totalInactiveMillis = 0L

            for (i in rawLogs.indices) {
                val current = rawLogs[i]
                val currentStatus = if (current.status == 1) "Active" else "Inactive"
                val currentTime = current.time ?: continue

                if (startTime == null) {
                    startTime = currentTime
                    prevStatus = current.status
                    continue
                }

                val isSameGroup = (prevStatus == current.status) ||
                        (prevStatus != 1 && current.status != 1)

                if (!isSameGroup || i == rawLogs.lastIndex) {
                    val endTime = currentTime
                    val df = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val startDate = df.parse(startTime) ?: continue
                    val endDate = df.parse(endTime) ?: startDate
                    val durationMillis = endDate.time - startDate.time
                    val duration = formatDuration(durationMillis)

                    val statusLabel = if (prevStatus == 1) "Active" else "Inactive"
                    merged.add(DailyLogEntry(startTime, endTime, duration, statusLabel))

                    if (statusLabel == "Active") totalActiveMillis += durationMillis
                    else totalInactiveMillis += durationMillis

                    startTime = currentTime
                    prevStatus = current.status
                }
            }

            logList.clear()
            logList.addAll(merged)
            adapter.notifyDataSetChanged()

            val summary = "Active: ${formatDuration(totalActiveMillis)} | Inactive: ${formatDuration(totalInactiveMillis)}"
            binding.textViewSummary.text = summary

        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load logs", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) "${minutes}m ${remainingSeconds}s" else "${remainingSeconds}s"
    }
}

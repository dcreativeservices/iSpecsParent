package com.ispecs.parent

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.ispecs.parent.model.LogEntry
import com.ispecs.parent.model.StatusDuration
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class LogsBriefActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs_brief)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = intent.getStringExtra("date")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val logsData: ArrayList<LogEntry>? = intent.getParcelableArrayListExtra("logs")

        Log.d("re" , logsData?.size.toString())

        val viewDetailedLogsBtn: Button = findViewById(R.id.viewDetailedLogsBtn)
        val viewBatteryLogsBtn: Button = findViewById(R.id.viewBatteryLogsBtn)
        val viewRawLogsBtn: Button = findViewById(R.id.viewRawLogsBtn)
        val totalOnTimeTextView: TextView = findViewById(R.id.totalOnTime)

        viewBatteryLogsBtn.setOnClickListener {
            val intent = Intent(applicationContext, BatteryLogActivity::class.java).apply {
                putParcelableArrayListExtra("logs", logsData)
            }
            startActivity(intent)
        }

        viewRawLogsBtn.setOnClickListener {
            val intent = Intent(applicationContext, RawLogsActivity::class.java).apply {
                putParcelableArrayListExtra("logs", logsData)
            }
            startActivity(intent)
        }

        // Calculate on/off durations
        val statusDurations = logsData?.let { calculateStatusDurations(it) }


        viewDetailedLogsBtn.setOnClickListener {
            val intent = Intent(applicationContext, LogDetailsActivity::class.java).apply {
                putParcelableArrayListExtra("statusDuration", statusDurations)
            }
            startActivity(intent)
        }


        if (statusDurations != null) {
            // Update total on time
            val totalOnTime = statusDurations.filter { it.state == "On" }
                .sumOf { it.durationInSeconds }
            totalOnTimeTextView.text = formatDuration(totalOnTime)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()  // This method is called when the up button is pressed. Just finish the activity
        return true
    }


    private fun calculateStatusDurations(logEntries: ArrayList<LogEntry>): ArrayList<StatusDuration> {
        val durations = ArrayList<StatusDuration>()
        if (logEntries.isEmpty()) return durations

        val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        var startTime = logEntries.first().time
        var currentState = logEntries.first().status

        for (i in 1 until logEntries.size) {
            val entry = logEntries[i]
            if (entry.status != currentState) {
                // Calculate duration
                val durationInSeconds = calculateTimeDifferenceInSeconds(startTime, entry.time, timeFormatter)
                durations.add(
                    StatusDuration(
                        state = if (currentState == 1) "On" else "Off",
                        startTime = startTime,
                        endTime = entry.time,
                        durationInSeconds = durationInSeconds
                    )
                )
                // Update startTime and currentState
                startTime = entry.time
                currentState = entry.status
            }
        }
        // Handle the final duration
        val lastEntry = logEntries.last()
        val durationInSeconds = calculateTimeDifferenceInSeconds(startTime, lastEntry.time, timeFormatter)
        durations.add(
            StatusDuration(
                state = if (currentState == 1) "On" else "Off",
                startTime = startTime,
                endTime = lastEntry.time,
                durationInSeconds = durationInSeconds
            )
        )

        return durations
    }

    private fun calculateTimeDifferenceInSeconds(startTime: String?, endTime: String?, formatter: SimpleDateFormat): Long {
        val startDate = formatter.parse(startTime)
        val endDate = formatter.parse(endTime)
        return TimeUnit.MILLISECONDS.toSeconds(endDate.time - startDate.time)
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        // We ignore remaining seconds for display purposes.

        return if (hours == 0L && minutes == 0L) {
            "Child was not wearing glasses"
        } else {
            val parts = mutableListOf<String>()
            if (hours > 0) {
                parts.add("$hours hour${if (hours > 1) "s" else ""}")
            }
            if (minutes > 0) {
                parts.add("$minutes minute${if (minutes > 1) "s" else ""}")
            }
            "Child was wearing glasses for ${parts.joinToString(" ")}"
        }
    }


}
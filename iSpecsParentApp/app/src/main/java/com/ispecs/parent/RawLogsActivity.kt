package com.ispecs.parent

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ispecs.parent.model.LogEntry

class RawLogsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_raw_logs)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle("Raw Logs")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Retrieve the list of LogEntry objects from the intent
        val logEntries = intent.getParcelableArrayListExtra<LogEntry>("logs") ?: arrayListOf()

        // Keys in the desired fixed order
        val keys = listOf(
            "sensor1", "sensor2", "acl_x", "acl_y", "acl_z",
            "day", "month", "year", "hour", "minute", "second",
            "battery", "status"
        )

        // Prepare a list of rows: the first row will be the header
        val rows = mutableListOf<List<String>>()

        // Add header row with column names
        rows.add(listOf( "s1" , "s1" , "x" , "y", "z", "D" , "M" , "Y", "HH" , "MM", "SS", "Bat", "on/off" ))

        // Add data rows in the same key order
        logEntries.forEach { logEntry ->
            val row = keys.map { key ->
                logEntry.additionalData[key]?.toString() ?: "" // Default to empty string if value is missing
            }
            rows.add(row)
        }

        // Set up RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = RawValuesAdapter(rows)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()  // This method is called when the up button is pressed. Just finish the activity
        return true
    }
}


class RawValuesAdapter(
    private val rows: List<List<String>> // Each row is a list of values
) : RecyclerView.Adapter<RawValuesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rowContainer: LinearLayout = view.findViewById(R.id.rowContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_raw_value, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rowData = rows[position]

        // Clear previous views
        holder.rowContainer.removeAllViews()

        // Dynamically add TextViews for each value in the row
        rowData.forEach { value ->
            val textView = TextView(holder.itemView.context).apply {
                text = value
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 8, 16, 8) // Add padding for better spacing
            }
            holder.rowContainer.addView(textView)
        }
    }

    override fun getItemCount(): Int = rows.size
}
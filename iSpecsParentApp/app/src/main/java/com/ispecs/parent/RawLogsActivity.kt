package com.ispecs.parent

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class RawLogsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val TAG = "RawLogs"
    private lateinit var database: DatabaseReference

    private val keys = listOf(
        "sensor1", "sensor2", "acl_x", "acl_y", "acl_z",
        "day", "month", "year", "hour", "minute", "second",
        "battery", "status"
    )

    private val header = listOf(
        "IR1", "IR2", "X", "Y", "Z", "Day", "Month", "Year",
        "Hour", "Min", "Sec", "Battery", "Specs", "PRX1", "PRX2", "ACL", "Packet"
    )

    private val rows = mutableListOf<List<String>>()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_raw_logs)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Raw Logs"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val parentId = getSharedPreferences("MySharedPrefs", MODE_PRIVATE)
            .getString("parentId", null) ?: return

        val startDate = intent.getStringExtra("startDate") ?: return
        val endDate = intent.getStringExtra("endDate") ?: return
        val macAddress = intent.getStringExtra("childMac") ?: return

        database = FirebaseDatabase.getInstance().reference
        rows.add(header)

        fetchLogsForDateRange(parentId, macAddress, startDate, endDate)
    }

    private fun fetchLogsForDateRange(parentId: String, mac: String, startDate: String, endDate: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val start = sdf.parse(startDate) ?: return
        val end = sdf.parse(endDate) ?: return

        val dateList = mutableListOf<String>()
        val cal = Calendar.getInstance().apply { time = start }
        while (!cal.time.after(end)) {
            dateList.add(sdf.format(cal.time))
            cal.add(Calendar.DATE, 1)
        }

        var completed = 0
        for (date in dateList) {
            val path = "logs/$parentId/$mac/$date"
            Log.d(TAG, "Fetching logs from: $path")

            database.child("logs").child(parentId).child(mac).child(date)
                .get().addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        Log.w(TAG, "No logs for $date")
                    }
                    for (entry in snapshot.children) {
                        val values = keys.map { key ->
                            entry.child(key).getValue()?.toString() ?: ""
                        }

                        if (values.size == 13) {
                            val status = values[12].toIntOrNull() ?: 0
                            val specs = if (status and 0x01 > 0) "On" else "Off"
                            val prx1 = if (status and 0x02 > 0) "Fault" else "OK"
                            val prx2 = if (status and 0x04 > 0) "Fault" else "OK"
                            val acl = if (status and 0x08 > 0) "Fault" else "OK"
                            val packet = if (status and 0x10 > 0) "History" else "Live"

                            val newRow = values.subList(0, 12) + listOf(specs, prx1, prx2, acl, packet)
                            rows.add(newRow)
                        }
                    }

                    completed++
                    if (completed == dateList.size) {
                        updateRecycler()
                    }
                }.addOnFailureListener {
                    Log.e(TAG, "Error reading logs: ${it.message}")
                    completed++
                    if (completed == dateList.size) {
                        updateRecycler()
                    }
                }
        }
    }

    private fun updateRecycler() {
        if (rows.size == 1) {
            rows.add(listOf("No logs found for selected range"))
        }
        recyclerView.adapter = RawValuesAdapter(rows)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    class RawValuesAdapter(
        private val rows: List<List<String>>
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
            holder.rowContainer.removeAllViews()

            rowData.forEach { value ->
                val textView = TextView(holder.itemView.context).apply {
                    text = value
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(12, 8, 12, 8)
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    textSize = 14f
                    setBackgroundResource(R.drawable.cell_border)

                    if (position == 0) {
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }
                }
                holder.rowContainer.addView(textView)
            }
        }


        override fun getItemCount(): Int = rows.size
    }

}

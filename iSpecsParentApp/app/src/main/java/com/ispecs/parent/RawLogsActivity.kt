package com.ispecs.parent

import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.ispecs.parent.databinding.ActivityRawLogsBinding
import java.text.SimpleDateFormat
import java.util.*

class RawLogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRawLogsBinding
    private lateinit var database: DatabaseReference
    private val TAG = "RawLogs"

    private val keys = listOf(
        "sensor1", "sensor2", "acl_x", "acl_y", "acl_z",
        "day", "month", "year", "hour", "minute", "second",
        "battery", "status"
    )

    private val header = listOf(
        "IR1", "IR2", "X", "Y", "Z", "Day", "Month", "Year",
        "Hour", "Min", "Sec", "Battery", "Specs", "PRX1", "PRX2", "ACL", "Packet"
    )

    private val rows = mutableListOf<List<String>>()  // Full data
    private var filteredRows = mutableListOf<List<String>>()  // Displayed data

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRawLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Raw Logs"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Spinner
        val packetOptions = listOf("All", "Live", "History")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, packetOptions)
        binding.spinnerPacketFilter.adapter = adapter
        binding.spinnerPacketFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                filterRows(packetOptions[pos])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        // Draw header row manually
        buildHeaderRow()

        // Firebase
        val parentId = getSharedPreferences("MySharedPrefs", MODE_PRIVATE)
            .getString("parentId", null) ?: return
        val startDate = intent.getStringExtra("startDate") ?: return
        val endDate = intent.getStringExtra("endDate") ?: return
        val macAddress = intent.getStringExtra("childMac") ?: return

        database = FirebaseDatabase.getInstance().reference
        fetchLogsForDateRange(parentId, macAddress, startDate, endDate)
    }

    private fun buildHeaderRow() {
        binding.headerRow.removeAllViews()
        header.forEach { title ->
            val textView = TextView(this).apply {
                text = title
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.BLACK)
                setBackgroundColor(Color.parseColor("#F0F0F0"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(140, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(4, 4, 4, 4)
                }
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setSingleLine(true)
                setPadding(8, 8, 8, 8)
            }
            binding.headerRow.addView(textView)
        }
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
        rows.clear()

        for (date in dateList) {
            val path = "logs/$parentId/$mac/$date"
            Log.d(TAG, "Fetching logs from: $path")

            database.child("logs").child(parentId).child(mac).child(date)
                .get().addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        Log.w(TAG, "No logs for $date")
                    }
                    for (entry in snapshot.children) {
                        val values = keys.map { key -> entry.child(key).getValue()?.toString() ?: "" }
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
                        filterRows(binding.spinnerPacketFilter.selectedItem.toString())
                    }
                }.addOnFailureListener {
                    Log.e(TAG, "Error reading logs: ${it.message}")
                    completed++
                    if (completed == dateList.size) {
                        filterRows(binding.spinnerPacketFilter.selectedItem.toString())
                    }
                }
        }
    }

    private fun filterRows(filter: String) {
        filteredRows = if (filter == "All") {
            rows.toMutableList()
        } else {
            rows.filter { it.lastOrNull() == filter }.toMutableList()
        }

        binding.recyclerView.adapter = RawValuesAdapter(filteredRows)
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
            holder.rowContainer.removeAllViews()
            val rowData = rows[position]

            rowData.forEach { value ->
                val cell = TextView(holder.itemView.context).apply {
                    text = value
                    textSize = 14f
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(140, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(4, 4, 4, 4)
                    }
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setSingleLine(true)
                    setPadding(8, 8, 8, 8)
                }
                holder.rowContainer.addView(cell)
            }
        }

        override fun getItemCount(): Int = rows.size
    }
}

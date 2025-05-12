package com.ispecs.parent

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ispecs.parent.model.DateItem
import com.ispecs.parent.model.LogEntry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogsListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var database: DatabaseReference
    private lateinit var adapter: DatesAdapter
    private lateinit var progressBar: ProgressBar
    private val dateItems = mutableListOf<DateItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle("Logs")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val sharedPreferences = getSharedPreferences("MySharedPrefs", MODE_PRIVATE)
        val parentId = sharedPreferences.getString("parentId", null)
        if (parentId.isNullOrEmpty()) {
            Toast.makeText(this, "Parent ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Setting up RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DatesAdapter(dateItems , parentId, this, progressBar)
        recyclerView.adapter = adapter

        // Firebase reference
        database = FirebaseDatabase.getInstance().getReference("logs").child(parentId)
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dateItems.clear()
                snapshot.children.forEach { child ->
                    val dateStr = child.key ?: "Unknown Date"
                    dateItems.add(DateItem(dateStr))
                }

                // Create a date formatter using the correct pattern.
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                // Sort the dateItems list by descending date (most recent first).
                dateItems.sortByDescending { dateItem ->
                    try {
                        dateFormat.parse(dateItem.date)
                    } catch (e: Exception) {
                        // In case of an invalid date, assign the earliest possible date so it appears last.
                        Date(0)
                    }
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LogsListActivity, "Error fetching data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()  // This method is called when the up button is pressed. Just finish the activity
        return true
    }


    class DatesAdapter(private val datesList: List<DateItem> ,
                       private val parentId: String,
                       private val context: Context,
                       private val progressBar: ProgressBar) : RecyclerView.Adapter<DatesAdapter.DateViewHolder>() {

        class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_date, parent, false)
            return DateViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
            val currentItem = datesList[position]
            holder.textViewDate.text = currentItem.date
            holder.itemView.setOnClickListener {
                progressBar.visibility = View.VISIBLE  //
                fetchLogDataAndStartActivity(currentItem.date)
            }
        }

        override fun getItemCount() = datesList.size

        private fun fetchLogDataAndStartActivity(date: String) {
            val database = FirebaseDatabase.getInstance().getReference("logs").child(parentId).child(date)
            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val entries = ArrayList<LogEntry>()
                    snapshot.children.forEach { child ->
                        val battery = child.child("battery").getValue(Int::class.java) ?: 0
                        val status = child.child("status").getValue(Int::class.java) ?: 0
                        val time = child.child("uploaded_at").getValue(String::class.java) ?: "Unknown"

                        // Extract all additional data into a map
                        val additionalData = mutableMapOf<String, Any?>()
                        child.children.forEach { dataChild ->
                            val key = dataChild.key ?: return@forEach
                            additionalData[key] = dataChild.getValue()

                        }

                        // Create a LogEntry with additional data
                        entries.add(LogEntry(battery, status, time, additionalData))
                    }

                    Log.d("Logs Data", entries.toList().toString())

                    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    entries.sortBy { entry ->
                        try {
                            dateFormat.parse(entry.time)
                        } catch (e: Exception) {
                            null // Handle invalid date formats
                        }
                    }

                    progressBar.visibility = View.GONE
                    startBriefActivity(entries, date)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors
                    progressBar.visibility = View.GONE
                }
            })
        }


        private fun startBriefActivity(logs: ArrayList<LogEntry> , date: String) {
            val intent = Intent(context, LogsBriefActivity::class.java).apply {
                putParcelableArrayListExtra("logs", logs)
                putExtra("date", date)
            }
            context.startActivity(intent)
        }
    }



}
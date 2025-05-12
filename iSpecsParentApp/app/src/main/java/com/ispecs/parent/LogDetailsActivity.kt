package com.ispecs.parent

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.widget.Toolbar
import com.ispecs.parent.model.StatusDuration

class LogDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_details)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle("Log Details")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val statusDurations: ArrayList<StatusDuration>? = intent.getParcelableArrayListExtra("statusDuration")


        val statusListView: ListView = findViewById(R.id.statusListView)


        if (statusDurations != null) {

            // Prepare data for ListView
            val listItems = statusDurations.map {
                "${it.state}: ${it.startTime} - ${it.endTime} (Duration: ${formatDuration(it.durationInSeconds)})"
            }

            // Set data to ListView
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listItems)
            statusListView.adapter = adapter
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()  // This method is called when the up button is pressed. Just finish the activity
        return true
    }


    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

}

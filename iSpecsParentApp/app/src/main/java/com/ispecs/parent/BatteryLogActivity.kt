package com.ispecs.parent

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.firebase.database.FirebaseDatabase
import com.ispecs.parent.databinding.ActivityBatteryLogBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class BatteryLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBatteryLogBinding
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var startDate = ""
    private var endDate = ""
    private var xLabels = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBatteryLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Battery Logs"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        startDate = intent.getStringExtra("startDate") ?: ""
        endDate = intent.getStringExtra("endDate") ?: ""

        val sharedPrefs = getSharedPreferences("MySharedPrefs", MODE_PRIVATE)
        val parentId = sharedPrefs.getString("parentId", null) ?: return
        val childId = sharedPrefs.getString("selectedChildId", null)

        if (childId.isNullOrEmpty()) {
            Toast.makeText(this, "No child selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressOverlay.visibility = View.VISIBLE
        binding.lineChartBattery.clear()

        FirebaseDatabase.getInstance().getReference("Children")
            .child(childId)
            .child("mac")
            .get()
            .addOnSuccessListener { snapshot ->
                val childMac = snapshot.getValue(String::class.java) ?: ""

                if (childMac.isEmpty()) {
                    Toast.makeText(this, "MAC not found", Toast.LENGTH_SHORT).show()
                    binding.progressOverlay.visibility = View.GONE
                    return@addOnSuccessListener
                }

                loadBatteryLogs(parentId, childMac, startDate, endDate)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch MAC", Toast.LENGTH_SHORT).show()
                binding.progressOverlay.visibility = View.GONE
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_battery_log, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save_chart -> {
                saveChartAsImage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveChartAsImage() {
        val bitmap: Bitmap = binding.lineChartBattery.chartBitmap
        val file = File(getExternalFilesDir(null), "battery_chart_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(this, "Chart saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadBatteryLogs(parentId: String, mac: String, start: String, end: String) {
        val startDateObj = dateFormatter.parse(start) ?: return
        val endDateObj = dateFormatter.parse(end) ?: return

        val cal = Calendar.getInstance()
        cal.time = startDateObj
        val datesToFetch = mutableListOf<String>()
        while (!cal.time.after(endDateObj)) {
            datesToFetch.add(dateFormatter.format(cal.time))
            cal.add(Calendar.DATE, 1)
        }

        val batteryEntries = mutableListOf<Entry>()
        val timeLabels = mutableListOf<String>()

        var pointIndex = 0
        var fetchedDays = 0

        for (date in datesToFetch) {
            val path = "logs/$parentId/$mac/$date"
            FirebaseDatabase.getInstance().getReference(path)
                .get()
                .addOnSuccessListener { snapshot ->
                    for (logSnap in snapshot.children) {
                        val time = logSnap.child("uploaded_at").getValue(String::class.java)
                        val battery = logSnap.child("battery").getValue(Int::class.java)

                        if (time != null && battery != null && battery in 0..100) {
                            batteryEntries.add(Entry(pointIndex.toFloat(), battery.toFloat()))
                            timeLabels.add("$date $time")
                            pointIndex++
                        }
                    }

                    fetchedDays++
                    if (fetchedDays == datesToFetch.size) {
                        binding.progressOverlay.visibility = View.GONE
                        if (batteryEntries.isEmpty()) {
                            Toast.makeText(this, "No battery logs available", Toast.LENGTH_SHORT).show()
                        } else {
                            xLabels = timeLabels
                            setupBatteryChart(binding.lineChartBattery, batteryEntries)
                        }
                    }
                }
                .addOnFailureListener {
                    fetchedDays++
                    if (fetchedDays == datesToFetch.size) {
                        binding.progressOverlay.visibility = View.GONE
                        if (batteryEntries.isEmpty()) {
                            Toast.makeText(this, "Failed to load battery logs", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
    }

    private fun setupBatteryChart(chart: LineChart, entries: List<Entry>) {
        val batteryDataSet = LineDataSet(entries, "Battery %").apply {
            valueTextColor = Color.BLACK
            lineWidth = 2.5f
            circleRadius = 5f
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            setDrawFilled(true)
            fillAlpha = 80
            setDrawValues(true)
            setDrawCircles(true)
            setDrawHighlightIndicators(true)
            highLightColor = Color.MAGENTA

            // Color-code line and circles based on value ranges
            setColors(entries.map {
                when (it.y.toInt()) {
                    in 0..10 -> Color.RED
                    in 11..80 -> Color.BLUE
                    else -> Color.GREEN
                }
            })
            setCircleColors(entries.map {
                when (it.y.toInt()) {
                    in 0..10 -> Color.RED
                    in 11..80 -> Color.BLUE
                    else -> Color.GREEN
                }
            })
        }

        chart.data = LineData(batteryDataSet)

        chart.xAxis.apply {
            granularity = 1f
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            textColor = Color.DKGRAY
            textSize = 10f
            labelRotationAngle = -45f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index in xLabels.indices) {
                        xLabels[index].substringAfter(" ")
                    } else ""
                }
            }
        }

        chart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = 100f
            granularity = 10f
            textColor = Color.DKGRAY
            textSize = 12f
        }

        chart.axisRight.isEnabled = false
        chart.description.text = "Battery over Time"
        chart.legend.form = Legend.LegendForm.LINE

        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.setScaleEnabled(true)
        chart.animateX(750)

        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let {
                    val index = e.x.toInt()
                    val time = if (index in xLabels.indices) xLabels[index] else ""
                    Toast.makeText(this@BatteryLogActivity, "Battery: ${e.y.toInt()}% at $time", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onNothingSelected() {}
        })

        chart.invalidate()
    }
}
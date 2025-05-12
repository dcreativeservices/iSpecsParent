package com.ispecs.parent

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.ispecs.parent.model.LogEntry
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

class BatteryLogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battery_log)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle("Battery Logs")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val logsData: ArrayList<LogEntry>? = intent.getParcelableArrayListExtra("logs")

        val batteryChart: LineChart = findViewById(R.id.lineChartBattery)

        val xLabels = logsData?.map { it.time } // Extract time labels

        // Prepare battery data
        val batteryEntries = logsData?.mapIndexed { index, logEntry ->
            Entry(index.toFloat(), logEntry.battery.toFloat())
        }

        // Configure Battery Chart
        setupBatteryChart(batteryChart, batteryEntries, xLabels)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()  // This method is called when the up button is pressed. Just finish the activity
        return true
    }

    private fun setupBatteryChart(chart: LineChart, entries: List<Entry>?, xLabels: List<String?>?) {
        val batteryDataSet = LineDataSet(entries, "Battery Level").apply {
            color = Color.BLUE
            valueTextColor = Color.BLACK
            lineWidth = 2f
            circleRadius = 4f
            mode = LineDataSet.Mode.CUBIC_BEZIER // Enable cubic lines
            setDrawFilled(true) // Optional: Fill the area under the curve
            fillColor = Color.CYAN // Optional: Set fill color
            fillAlpha = 50 // Optional: Set fill transparency
        }

        val lineData = LineData(batteryDataSet)
        chart.data = lineData

        val xAxis = chart.xAxis
        xAxis.granularity = 1f
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                val index = value.toInt()
                return if (xLabels?.indices?.contains(index) == true) xLabels[index] else ""
            }
        }

        val yAxis = chart.axisLeft
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = 100f
        yAxis.granularity = 10f

        chart.axisRight.isEnabled = false // Disable right Y-axis
        chart.invalidate() // Refresh the chart
    }
}
package com.ispecs.parent.ui.ispecs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ispecs.parent.R
import com.ispecs.parent.databinding.ItemLogRowBinding
import com.ispecs.parent.model.DailyLogEntry

class LogsTableAdapter(private val logList: List<DailyLogEntry>) :
    RecyclerView.Adapter<LogsTableAdapter.LogViewHolder>() {

    class LogViewHolder(val binding: ItemLogRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val entry = logList[position]
        with(holder.binding) {
            date.text = entry.date
            startTime.text = entry.startTime
            endTime.text = entry.endTime
            duration.text = entry.duration

            // Set status dot color
            val iconRes = if (entry.status == "Active") R.drawable.ic_green_dot else R.drawable.ic_red_dot
            statusIcon.setImageResource(iconRes)
        }
    }

    override fun getItemCount(): Int = logList.size
}
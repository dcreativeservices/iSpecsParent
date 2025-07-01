package com.ispecs.parent.ui.ispecs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ispecs.parent.R

// Inline data class (still needed to hold the 3 fields)
data class ChildStatus(val name: String, val isActive: Boolean, val mac: String)

class ChildStatusAdapter(private val children: List<ChildStatus>) :
    RecyclerView.Adapter<ChildStatusAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textChildName)
        val statusTextView: TextView = itemView.findViewById(R.id.textChildStatus)
        val macTextView: TextView = itemView.findViewById(R.id.textChildMac)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_child_status, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val child = children[position]
        holder.nameTextView.text = child.name
        holder.statusTextView.text = if (child.isActive) "Active" else "Inactive"
        holder.statusTextView.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (child.isActive) R.color.green else R.color.red
            )
        )
        holder.macTextView.text = "MAC: ${child.mac}"
    }

    override fun getItemCount(): Int = children.size
}

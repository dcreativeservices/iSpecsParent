package com.ispecs.parent.ui.ispecs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.ispecs.parent.R
import com.ispecs.parent.databinding.ActivityChildStatusListBinding

class ChildStatusListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChildStatusListBinding
    private val childrenList = mutableListOf<ChildStatus>()
    private lateinit var adapter: RecyclerView.Adapter<*>

    // Inline data class for child status
    data class ChildStatus(val name: String, val isActive: Boolean, val mac: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildStatusListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Child App Status"
            setDisplayHomeAsUpEnabled(true)
        }

        adapter = object : RecyclerView.Adapter<ChildViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_child_status, parent, false)
                return ChildViewHolder(view)
            }

            override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
                val child = childrenList[position]
                holder.nameTextView.text = child.name
                holder.statusTextView.text = if (child.isActive) "Active" else "Inactive"
                holder.statusTextView.setTextColor(
                    ContextCompat.getColor(
                        this@ChildStatusListActivity,
                        if (child.isActive) R.color.green else R.color.red
                    )
                )
                holder.macTextView.text = "MAC: ${child.mac}"
            }

            override fun getItemCount(): Int = childrenList.size
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadChildren()
    }

    private fun loadChildren() {
        val parentId = getSharedPreferences("MySharedPrefs", MODE_PRIVATE)
            .getString("parentId", null) ?: return

        val dbRef = FirebaseDatabase.getInstance().getReference("Children")
        dbRef.orderByChild("parent_ids/$parentId").equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    childrenList.clear()
                    for (child in snapshot.children) {
                        val name = child.child("name").getValue(String::class.java) ?: "Unnamed"
                        val isActive =
                            child.child("is_child_app_running").getValue(Boolean::class.java)
                                ?: false
                        val mac = child.child("mac").getValue(String::class.java) ?: "N/A"
                        childrenList.add(ChildStatus(name, isActive, mac))
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    inner class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textChildName)
        val statusTextView: TextView = itemView.findViewById(R.id.textChildStatus)
        val macTextView: TextView = itemView.findViewById(R.id.textChildMac)
    }
}

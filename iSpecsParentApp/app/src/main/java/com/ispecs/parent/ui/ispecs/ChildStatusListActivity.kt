package com.ispecs.parent.ui.ispecs

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.ispecs.parent.ui.ispecs.ChildStatus
import com.ispecs.parent.ui.ispecs.ChildStatusAdapter
import com.ispecs.parent.databinding.ActivityChildStatusListBinding

class ChildStatusListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChildStatusListBinding
    private val childrenList = mutableListOf<ChildStatus>()
    private lateinit var adapter: ChildStatusAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildStatusListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up Toolbar with Back button
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Child App Status"
            setDisplayHomeAsUpEnabled(true)
        }

        // Set up RecyclerView
        adapter = ChildStatusAdapter(childrenList)
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
                        val isActive = child.child("is_active").getValue(Boolean::class.java) ?: false
                        childrenList.add(ChildStatus(name, isActive))
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // handle error
                }
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
}

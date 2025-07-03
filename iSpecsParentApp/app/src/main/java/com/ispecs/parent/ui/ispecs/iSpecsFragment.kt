package com.ispecs.parent.ui.ispecs

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import com.ispecs.parent.ChildRegistrationActivity
import com.ispecs.parent.LogsBriefActivity
import com.ispecs.parent.databinding.FragmentIspecsBinding

class iSpecsFragment : Fragment() {
    private var _binding: FragmentIspecsBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIspecsBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        binding.toolbar.title = "iSpecs Parent"

        database = FirebaseDatabase.getInstance().reference

        binding.logsClickableLayout.setOnClickListener { fetchSelectedChildMacAndOpenLogs() }
        binding.childAppStatusLayout.setOnClickListener {
            startActivity(Intent(requireContext(), ChildStatusListActivity::class.java))
        }
        binding.registerChildLayout.setOnClickListener {
            startActivity(Intent(requireContext(), ChildRegistrationActivity::class.java))
        }

        return binding.root
    }

    private fun fetchSelectedChildMacAndOpenLogs() {
        val sharedPrefs = requireContext().getSharedPreferences("MySharedPrefs", AppCompatActivity.MODE_PRIVATE)
        val parentId = sharedPrefs.getString("parentId", null)
        val selectedChildId = sharedPrefs.getString("selectedChildId", null)

        if (parentId.isNullOrEmpty() || selectedChildId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Parent or selected child not found", Toast.LENGTH_SHORT).show()
            return
        }

        val childRef = database.child("Children").child(selectedChildId)
        childRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val mac = snapshot.child("mac").getValue(String::class.java)
                if (!mac.isNullOrEmpty()) {
                    // Save to SharedPreferences in case needed later
                    sharedPrefs.edit().putString("childMac", mac).apply()

                    val intent = Intent(requireContext(), LogsBriefActivity::class.java).apply {
                        putExtra("childMac", mac)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "MAC address missing for selected child", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

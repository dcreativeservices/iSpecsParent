package com.ispecs.parent.ui.settings

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.database.*
import com.ispecs.parent.ChildConfigActivity
import com.ispecs.parent.R
import com.ispecs.parent.databinding.FragmentSettingsBinding
import showUpdateDialog

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val settingsViewModel: SettingsViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
    }

    private var cachedChildrenList: List<Pair<String, String>> = emptyList()
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root = binding.root

        database = FirebaseDatabase.getInstance().reference
        val sharedPrefs = requireContext().getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)

        val firebaseAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        Log.d("SettingsFragment", "Current Firebase UID: $firebaseAuthUid")
        if (firebaseAuthUid == null) {
            binding.textViewChildName.text = "No Child Found"
            return root
        }

        val childrenRef = database.child("Parents").child(firebaseAuthUid).child("children")

        childrenRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    binding.textViewChildName.text = "No Child Found"
                    return
                }

                val childList = snapshot.children.mapNotNull {
                    val childId = it.key ?: return@mapNotNull null
                    val firstName = it.child("childFirstName").getValue(String::class.java) ?: return@mapNotNull null
                    val lastName = it.child("childLastName").getValue(String::class.java) ?: ""
                    val name = "$firstName $lastName".trim()
                    Pair(childId, name)
                }

                cachedChildrenList = childList
                val savedChildId = sharedPrefs.getString("selectedChildId", null)

                val childToLoad = when {
                    savedChildId != null && childList.any { it.first == savedChildId } -> savedChildId
                    childList.isNotEmpty() -> childList[0].first
                    else -> null
                }

                if (childToLoad != null) {
                    settingsViewModel.loadSelectedChild(childToLoad)

                    if (childList.size == 1) {
                        binding.imageViewChildDropdown.visibility = View.GONE
                        binding.layoutChildName.setOnClickListener(null)
                        binding.layoutChildName.isClickable = false
                        binding.layoutChildName.isEnabled = false
                    } else {
                        binding.imageViewChildDropdown.visibility = View.VISIBLE
                        binding.layoutChildName.isClickable = true
                        binding.layoutChildName.setOnClickListener {
                            showChildSelectionDialog(cachedChildrenList)
                        }
                    }

                    settingsViewModel.parentId.observe(viewLifecycleOwner) { parentId ->
                        settingsViewModel.macAddress.observe(viewLifecycleOwner) { mac ->
                            if (!parentId.isNullOrBlank() && !mac.isNullOrBlank()) {
                                fetchChildStatusForGlasses(parentId, mac)
                            }
                        }
                    }
                } else {
                    binding.textViewChildName.text = "No Child Found"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.textViewChildName.text = "No Child Found"
            }
        })

        // ADD CHILD BUTTON → Launch ChildConfigActivity
        binding.btnAddChild.setOnClickListener {
            val intent = Intent(requireContext(), ChildConfigActivity::class.java)
            startActivity(intent)
        }

        // Observers
        settingsViewModel.childName.observe(viewLifecycleOwner) {
            binding.textViewChildName.text = it ?: "No Child Found"
        }

        settingsViewModel.macAddress.observe(viewLifecycleOwner) {
            binding.textViewMacAddress.text = "MAC: ${it ?: "--"}"
        }

        settingsViewModel.iSpecDeviceStatus.observe(viewLifecycleOwner) { status ->
            val label = "iSpec Device Status: "
            val rawValue = status ?: "--"
            val value = when (rawValue.lowercase()) {
                "active" -> "Connected"
                "inactive" -> "Disconnected"
                else -> "--"
            }

            val spannable = SpannableString(label + value)
            val color = when (value) {
                "Connected" -> ContextCompat.getColor(requireContext(), R.color.green)
                "Disconnected" -> ContextCompat.getColor(requireContext(), R.color.red)
                else -> ContextCompat.getColor(requireContext(), R.color.dark_gray)
            }

            spannable.setSpan(
                ForegroundColorSpan(color),
                label.length,
                label.length + value.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.textViewDeviceStatus.text = spannable
        }

        settingsViewModel.blurIntensity.observe(viewLifecycleOwner) {
            binding.textViewBlurIntensity.text = "$it%"
        }

        settingsViewModel.blurDelay.observe(viewLifecycleOwner) {
            binding.textViewBlurDelay.text = "$it s"
        }

        settingsViewModel.fadeIn.observe(viewLifecycleOwner) {
            binding.textViewFadeIn.text = "$it s"
        }

        settingsViewModel.mute.observe(viewLifecycleOwner) {
            binding.muteSwitch.isChecked = it
        }

        settingsViewModel.parentId.observe(viewLifecycleOwner) {
            binding.parentIDTextView.text = it
        }

        settingsViewModel.passcode.observe(viewLifecycleOwner) {
            binding.textViewChildPasscode.text = if (!it.isNullOrBlank() && it.length == 4) "****" else "----"
        }

        // Settings update listeners
        binding.setBlurIntensityLayout.setOnClickListener {
            showUpdateDialog(requireContext(), "Update Blur Intensity",
                settingsViewModel.blurIntensity.value ?: 80, 10, 100
            ) { newValue ->
                settingsViewModel.updateChildSetting("blur_intensity", newValue)
            }
        }

        binding.setBlurDelayLayout.setOnClickListener {
            showUpdateDialog(requireContext(), "Update Blur Delay",
                settingsViewModel.blurDelay.value ?: 2, 1, 20
            ) { newValue ->
                settingsViewModel.updateChildSetting("blur_delay", newValue)
            }
        }

        binding.setFadeInLayout.setOnClickListener {
            showUpdateDialog(requireContext(), "Update Fade In",
                settingsViewModel.fadeIn.value ?: 5, 1, 20
            ) { newValue ->
                settingsViewModel.updateChildSetting("fade_in", newValue)
            }
        }

        binding.muteSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.updateChildSetting("mute", isChecked)
        }

        binding.layoutChildPasscode.setOnClickListener {
            showUpdatePasscodeDialog()
        }

        binding.logoutText.setOnClickListener {
            settingsViewModel.onLogoutClick()
        }

        return root
    }

    private fun fetchChildStatusForGlasses(parentId: String, macAddress: String) {
        val glassesStatusView = binding.textViewGlassesWearingStatus
        val sanitizedMac = macAddress.trim()

        val baseRef = FirebaseDatabase.getInstance()
            .getReference("logs")
            .child(parentId)
            .child(sanitizedMac)

        baseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    glassesStatusView.text = "iSpec Glasses: --"
                    glassesStatusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
                    return
                }

                val latestDate = snapshot.children.mapNotNull { it.key }.maxOrNull()
                if (latestDate == null) {
                    glassesStatusView.text = "iSpec Glasses: --"
                    glassesStatusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
                    return
                }

                val logRef = baseRef.child(latestDate)
                logRef.limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(logSnapshot: DataSnapshot) {
                        var found = false
                        for (entry in logSnapshot.children) {
                            val status = entry.child("status").getValue(Int::class.java)
                            if (status == 1) found = true
                        }

                        val label = "iSpec Glasses: "
                        val statusText = if (found) "Wearing" else "Not Wearing"
                        val fullText = label + statusText

                        val spannable = SpannableString(fullText)
                        val color = ContextCompat.getColor(requireContext(), if (found) R.color.green else R.color.red)
                        spannable.setSpan(
                            ForegroundColorSpan(color),
                            label.length,
                            fullText.length,
                            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        glassesStatusView.text = spannable
                    }

                    override fun onCancelled(error: DatabaseError) {
                        glassesStatusView.text = "iSpec Glasses: --"
                        glassesStatusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                glassesStatusView.text = "iSpec Glasses: --"
                glassesStatusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
            }
        })
    }

    private fun showChildSelectionDialog(children: List<Pair<String, String>>) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_select_child, null)
        val listView = dialogView.findViewById<ListView>(R.id.childListView)

        val displayList = children.map { it.second } + "➕ Add Another Child"

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displayList)
        listView.adapter = adapter

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogStyle)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            if (position == children.size) {
                startActivity(Intent(requireContext(), ChildConfigActivity::class.java))
            } else {
                val selectedChildId = children[position].first
                val sharedPrefs = requireContext().getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().putString("selectedChildId", selectedChildId).apply()
                settingsViewModel.loadSelectedChild(selectedChildId)
                settingsViewModel.parentId.value?.let { parentId ->
                    settingsViewModel.macAddress.value?.let { mac ->
                        fetchChildStatusForGlasses(parentId, mac)
                    }
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showUpdatePasscodeDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update_passcode, null)
        val editText = dialogView.findViewById<EditText>(R.id.editTextPasscode)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Update Password")
            .setView(dialogView)
            .setPositiveButton("Update", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val newPasscode = editText.text.toString()
                if (newPasscode.length == 4 && newPasscode.all { it.isDigit() }) {
                    settingsViewModel.updateChildSetting("passcode", newPasscode)
                    dialog.dismiss()
                } else {
                    editText.error = "Password must be exactly 4 digits"
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

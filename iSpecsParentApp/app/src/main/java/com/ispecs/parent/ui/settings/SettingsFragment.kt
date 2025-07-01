package com.ispecs.parent.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.ispecs.parent.R
import com.ispecs.parent.databinding.FragmentSettingsBinding
import showUpdateDialog

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val settingsViewModel: SettingsViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        settingsViewModel.loadChildrenList { childList ->
            when {
                childList.size == 1 -> settingsViewModel.loadSelectedChild(childList[0].first)
                childList.isNotEmpty() -> showChildSelectionDialog(childList)
                else -> binding.textViewChildName.text = "No Child Found"
            }
        }

        settingsViewModel.childName.observe(viewLifecycleOwner) {
            binding.textViewChildName.text = it
        }

        settingsViewModel.macAddress.observe(viewLifecycleOwner) {
            binding.textViewMacAddress.text = "MAC: ${it ?: "--"}"
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

        settingsViewModel.passcode.observe(viewLifecycleOwner) { passcode ->
            binding.textViewChildPasscode.text = if (!passcode.isNullOrEmpty() && passcode.length == 4) {
                "****"
            } else {
                "----"
            }
        }

        // Click listeners
        binding.setBlurIntensityLayout.setOnClickListener {
            showUpdateDialog(
                requireContext(), "Update Blur Intensity",
                settingsViewModel.blurIntensity.value ?: 80, 10, 100
            ) { newValue ->
                settingsViewModel.updateChildSetting("blur_intensity", newValue)
            }
        }

        binding.setBlurDelayLayout.setOnClickListener {
            showUpdateDialog(
                requireContext(), "Update Blur Delay",
                settingsViewModel.blurDelay.value ?: 2, 1, 20
            ) { newValue ->
                settingsViewModel.updateChildSetting("blur_delay", newValue)
            }
        }

        binding.setFadeInLayout.setOnClickListener {
            showUpdateDialog(
                requireContext(), "Update Fade In",
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

    private fun showChildSelectionDialog(children: List<Pair<String, String>>) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_select_child, null)

        val listView = dialogView.findViewById<ListView>(R.id.childListView)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            children.map { it.second }
        )
        listView.adapter = adapter

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogStyle)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedChildId = children[position].first
            settingsViewModel.loadSelectedChild(selectedChildId)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showUpdatePasscodeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_update_passcode, null)

        val editText = dialogView.findViewById<EditText>(R.id.editTextPasscode)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Update Passcode")
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
                    editText.error = "Passcode must be exactly 4 digits"
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

// Updated SettingsFragment.kt to support child selection and settings management from Children node

package com.ispecs.parent.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
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
            if (childList.isNotEmpty()) {
                showChildSelectionDialog(childList)
            }
        }

        settingsViewModel.childName.observe(viewLifecycleOwner) {
            binding.textViewChildName.text = it
        }

        binding.setBlurIntensityLayout.setOnClickListener {
            showUpdateDialog(
                requireContext(),
                "Update Blur Intensity",
                settingsViewModel.blurIntensity.value ?: 80,
                10,
                100
            ) { newValue ->
                settingsViewModel.updateChildSetting("blur_intensity", newValue)
            }
        }

        binding.setBlurDelayLayout.setOnClickListener {
            showUpdateDialog(
                requireContext(),
                "Update Blur Delay",
                settingsViewModel.blurDelay.value ?: 2,
                1,
                20
            ) { newValue ->
                settingsViewModel.updateChildSetting("blur_delay", newValue)
            }
        }

        binding.setFadeInLayout.setOnClickListener {
            showUpdateDialog(
                requireContext(),
                "Update Fade In",
                settingsViewModel.fadeIn.value ?: 5,
                1,
                20
            ) { newValue ->
                settingsViewModel.updateChildSetting("fade_in", newValue)
            }
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

        binding.muteSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.updateChildSetting("mute", isChecked)
        }

        settingsViewModel.parentId.observe(viewLifecycleOwner) {
            binding.parentIDTextView.text = it
        }

        settingsViewModel.passcode.observe(viewLifecycleOwner) {
            binding.textViewChildPasscode.text = it
        }

        binding.logoutText.setOnClickListener {
            settingsViewModel.onLogoutClick()
        }

        binding.layoutChildPasscode.setOnClickListener {
            showUpdatePasscodeDialog()
        }

        return root
    }

    private fun showChildSelectionDialog(children: List<Pair<String, String>>) {
        val childNames = children.map { it.second }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, childNames)
        AlertDialog.Builder(requireContext())
            .setTitle("Select Child")
            .setAdapter(adapter) { _, which ->
                val selectedChildId = children[which].first
                settingsViewModel.loadSelectedChild(selectedChildId)
            }
            .setCancelable(false)
            .show()
    }

    private fun showUpdatePasscodeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_update_passcode, null)
        val editText = dialogView.findViewById<EditText>(R.id.editTextPasscode)
        AlertDialog.Builder(requireContext())
            .setTitle("Update Passcode")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newPasscode = editText.text.toString()
                settingsViewModel.updateChildSetting("passcode", newPasscode)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

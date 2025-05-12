package com.ispecs.parent.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.ispecs.parent.R
import com.ispecs.parent.databinding.FragmentSettingsBinding
import showUpdateDialog

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val settingsViewModel: SettingsViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the merged layout which includes both ISpecs and settings UI
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // --- ISpecs UI Section ---

        binding.setBlurIntensityLayout.setOnClickListener {
            showUpdateDialog(
                requireContext(),
                "Update Blur Intensity",
                settingsViewModel.blurIntensity.value ?: 80,
                10,
                100
            ) { newValue ->
                settingsViewModel.updateBlurIntensity(newValue)
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
                settingsViewModel.updateBlurDelay(newValue)
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
                settingsViewModel.updateFadeIn(newValue)
            }
        }

        // Observe ISpecs-related LiveData
        settingsViewModel.blurIntensity.observe(viewLifecycleOwner) { intensity ->
            binding.textViewBlurIntensity.text = "$intensity%"
        }
        settingsViewModel.blurDelay.observe(viewLifecycleOwner) { delay ->
            binding.textViewBlurDelay.text = "$delay s"
        }
        settingsViewModel.fadeIn.observe(viewLifecycleOwner) { fade ->
            binding.textViewFadeIn.text = "$fade s"
        }
        settingsViewModel.mute.observe(viewLifecycleOwner) { mute ->
            binding.muteSwitch.isChecked = mute
        }
        binding.muteSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.setMute(isChecked)
        }

        // --- Settings UI Section ---

        // Display the parent ID (loaded from SharedPreferences)
        settingsViewModel.parentId.observe(viewLifecycleOwner) { id ->
            binding.parentIDTextView.text = id
        }
        // Display the child passcode
        settingsViewModel.passcode.observe(viewLifecycleOwner) { code ->
            binding.textViewChildPasscode.text = code
        }

        // Logout click listener
        binding.logoutText.setOnClickListener {
            settingsViewModel.onLogoutClick()
        }

        // Update passcode dialog
        binding.layoutChildPasscode.setOnClickListener {
            showUpdatePasscodeDialog()
        }

        return root
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
                settingsViewModel.updatePasscode(newPasscode)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

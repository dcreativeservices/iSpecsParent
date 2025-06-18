package com.ispecs.parent.ui.ispecs

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ispecs.parent.ChildRegistrationActivity
import com.ispecs.parent.LogsListActivity
import com.ispecs.parent.R
import com.ispecs.parent.databinding.FragmentIspecsBinding

class iSpecsFragment : Fragment() {

    private var _binding: FragmentIspecsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIspecsBinding.inflate(inflater, container, false)

        // ✅ Set toolbar title programmatically
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "iSpecs Parent"

        // 👉 Logs click
        binding.logsClickableLayout.setOnClickListener {
            startActivity(Intent(requireContext(), LogsListActivity::class.java))
        }

        // 👉 Child App Status click
        binding.childAppStatusLayout.setOnClickListener {
            startActivity(Intent(requireContext(), ChildStatusListActivity::class.java))
        }

        // 👉 Register Child click
        binding.registerChildLayout.setOnClickListener {
            startActivity(Intent(requireContext(), ChildRegistrationActivity::class.java))
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.ispecs.parent.ui.ispecs

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ispecs.parent.LogsListActivity
import com.ispecs.parent.R
import com.ispecs.parent.databinding.FragmentIspecsBinding

class iSpecsFragment : Fragment() {

    private var _binding: FragmentIspecsBinding? = null

    private val activityViewModel: iSpecsViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory(
            requireActivity().application
        )
    }


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        _binding = FragmentIspecsBinding.inflate(inflater, container, false)
        val root: View = binding.root

       /* val textView: TextView = binding.textNotifications
        activityViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }\*/

        binding.screenTimeLayout.setOnClickListener {
            showScreenTimeDialog()
        }

        binding.logsClickableLayout.setOnClickListener{
            startActivity(Intent(activity, LogsListActivity::class.java))
        }

        activityViewModel.childAppStatus.observe(viewLifecycleOwner, Observer { childAppStatus ->
            if(childAppStatus){
                binding.textViewChildAppStatus.text = "Active"
                binding.textViewChildAppStatus.setTextColor(resources.getColor(R.color.green))

            } else {
                binding.textViewChildAppStatus.text = "Inactive"
                binding.textViewChildAppStatus.setTextColor(resources.getColor(R.color.red))
            }
        })

        return root
    }

    private fun showScreenTimeDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_screen_time, null)

        val editTextScreenTime = dialogView.findViewById<EditText>(R.id.editTextScreenTime)
        val buttonSave = dialogView.findViewById<Button>(R.id.buttonSave)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Screen time")
            .setView(dialogView)
            .create()

        buttonSave.setOnClickListener {
            val screenTime = editTextScreenTime.text.toString().trim()
            if (screenTime.isNotEmpty()) {
                activityViewModel.saveScreenTime(screenTime.toInt())
                dialog.dismiss()
            }
        }


        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
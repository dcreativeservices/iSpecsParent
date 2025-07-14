package com.ispecs.parent

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class ChildConfigActivity : AppCompatActivity() {

    private lateinit var editTextChildFirstName: EditText
    private lateinit var editTextChildLastName: EditText
    private lateinit var editTextViewTime: EditText
    private lateinit var editTextChildPasscode: EditText
    private lateinit var spinnerBlurType: Spinner
    private lateinit var saveButton: Button

    private lateinit var parentId: String
    private lateinit var parentName: String
    private lateinit var parentEmail: String

    private val database = FirebaseDatabase.getInstance()
    private val TAG = "ChildConfigActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_config)

        // Setup Toolbar with back button
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Child Configuration"

        parentId = intent.getStringExtra("parentId") ?: ""
        parentName = intent.getStringExtra("parentName") ?: ""
        parentEmail = intent.getStringExtra("parentEmail") ?: ""

        editTextChildFirstName = findViewById(R.id.editTextChildFirstName)
        editTextChildLastName = findViewById(R.id.editTextChildLastName)
        editTextViewTime = findViewById(R.id.editTextViewTime)
        editTextChildPasscode = findViewById(R.id.editTextChildPasscode)
        spinnerBlurType = findViewById(R.id.spinnerBlurType)
        saveButton = findViewById(R.id.saveConfigButton)

        val blurOptions = listOf("popup", "blur")
        spinnerBlurType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, blurOptions)

        saveButton.setOnClickListener {
            val childFirstName = editTextChildFirstName.text.toString().trim()
            val childLastName = editTextChildLastName.text.toString().trim()
            val viewTime = editTextViewTime.text.toString().toIntOrNull()
            val blurType = spinnerBlurType.selectedItem.toString()
            val childPasscode = editTextChildPasscode.text.toString().trim()

            if (childFirstName.length !in 3..30) {
                editTextChildFirstName.error = "First name must be 3–30 characters"
                return@setOnClickListener
            }

            if (childLastName.length !in 3..30) {
                editTextChildLastName.error = "Last name must be 3–30 characters"
                return@setOnClickListener
            }

            if (viewTime == null || viewTime !in 10..240) {
                editTextViewTime.error = "Screen time must be 10 to 240 minutes"
                return@setOnClickListener
            }

            if (childPasscode.length != 4 || !childPasscode.all { it.isDigit() }) {
                editTextChildPasscode.error = "Passcode must be exactly 4 digits"
                return@setOnClickListener
            }

            val childId = UUID.randomUUID().toString()
            val macAddress = getPairedMacAddress()
            val parentUid = FirebaseAuth.getInstance().currentUser?.uid

            if (parentUid == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val childMap = mapOf(
                "childId" to childId,
                "parentId" to parentId,
                "childFirstName" to childFirstName,
                "childLastName" to childLastName,
                "childPasscode" to childPasscode,
                "mac" to macAddress,
                "screen_time" to viewTime,
                "blur_type" to blurType,
                "blur_delay" to 2,
                "blur_intensity" to 80,
                "fade_in" to 5,
                "mute" to true,
                "is_child_app_running" to false,
                "ispec_device_status" to "inactive"
            )

            val ref = database.getReference("Parents")
                .child(parentUid)
                .child("children")
                .child(childId)

            ref.setValue(childMap).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Child config saved!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to save config", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Firebase write failed: ${task.exception?.message}")
                }
            }
        }
    }

    // Handle toolbar back button press
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getPairedMacAddress(): String {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        return pairedDevices?.firstOrNull()?.address ?: "00:00:00:00:00:00"
    }
}

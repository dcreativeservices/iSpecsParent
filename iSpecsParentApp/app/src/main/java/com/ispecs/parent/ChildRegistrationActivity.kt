package com.ispecs.parent

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// Import the standard Toolbar, EditText, and Button
import androidx.appcompat.widget.Toolbar
import android.widget.EditText
import android.widget.Button
import com.google.firebase.database.FirebaseDatabase

class ChildRegistrationActivity : AppCompatActivity() {

    // Correct types: EditText and Button
    private lateinit var etChildName: EditText
    private lateinit var etChildPasscode: EditText
    private lateinit var etMacAddress: EditText
    private lateinit var btnRegister: Button

    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This should be the XML layout that uses standard EditText and Button
        setContentView(R.layout.activity_child_registration)

        // Ensure this ID matches the Toolbar ID in your XML (e.g., R.id.toolbar)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Register Child"
            setDisplayHomeAsUpEnabled(true) // Show back arrow
        }
        // It's good practice to null-check if there's any doubt,
        // but if the ID is correct, it shouldn't be null.
        toolbar?.setNavigationOnClickListener { finish() }


        // These IDs must match the IDs in your activity_child_registration.xml
        etChildName = findViewById(R.id.etChildName)
        etChildPasscode = findViewById(R.id.etChildPasscode)
        etMacAddress = findViewById(R.id.etMacAddress)
        btnRegister = findViewById(R.id.btnRegisterChild)

        btnRegister.setOnClickListener {
            registerChild()
        }
    }

    private fun registerChild() {
        val name = etChildName.text.toString().trim()
        val passcode = etChildPasscode.text.toString().trim()
        val mac = etMacAddress.text.toString().trim()

        if (name.isEmpty() || passcode.isEmpty() || mac.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (name.length < 3 || name.length > 30) {
            Toast.makeText(this, "Child name must be between 3 and 30 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (passcode.length != 4 || !passcode.all { it.isDigit() }) {
            Toast.makeText(this, "Passcode must be exactly 4 numeric digits", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidMacAddress(mac)) {
            Toast.makeText(this, "Invalid MAC Address format. Use XX:XX:XX:XX:XX:XX", Toast.LENGTH_LONG).show()
            return
        }

        val sharedPref = getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        val parentId = sharedPref.getString("parentId", null)

        if (parentId == null) {
            Toast.makeText(this, "Parent ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
            // Optionally, navigate the user back to a login screen or take other appropriate action.
            return
        }

        val childrenRef = database.getReference("Children")
        childrenRef.orderByChild("mac").equalTo(mac)
            .get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    Toast.makeText(this, "A child with this MAC address is already registered.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val childRef = childrenRef.push()
                val childId = childRef.key ?: run {
                    Toast.makeText(this, "Error generating child ID", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val childMap = mapOf(
                    "name" to name,
                    "passcode" to passcode,
                    "mac" to mac,
                    "parent_ids" to mapOf(parentId to true), // Storing parentId as a key in a map
                    "screen_time" to 10, // Default value
                    "blur_delay" to 10,  // Default value
                    "blur_intensity" to 90, // Default value
                    "fade_in" to 10, // Default value
                    "is_child_app_running" to false, // Default value
                    "mute" to true // Default value
                )

                childRef.setValue(childMap).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Child registered successfully!", Toast.LENGTH_SHORT).show()
                        finish() // Close the activity
                    } else {
                        Toast.makeText(this, "Failed to register child: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Database error during MAC check: ${exception.message}", Toast.LENGTH_LONG).show()
                exception.printStackTrace() // Log the full stack trace for debugging
            }
    }

    private fun isValidMacAddress(mac: String): Boolean {
        // Regex for MAC address format XX:XX:XX:XX:XX:XX or XX-XX-XX-XX-XX-XX
        // Allows both uppercase and lowercase hex digits.
        val regex = Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
        return regex.matches(mac)
    }
}
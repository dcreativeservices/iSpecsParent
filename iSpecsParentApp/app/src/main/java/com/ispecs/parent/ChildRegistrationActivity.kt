package com.ispecs.parent

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase

class ChildRegistrationActivity : AppCompatActivity() {

    private lateinit var etChildName: TextInputEditText
    private lateinit var etChildPasscode: TextInputEditText
    private lateinit var etMacAddress: TextInputEditText
    private lateinit var btnRegister: MaterialButton

    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_registration)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Register Child" // ✅ Set your custom title here
            setDisplayHomeAsUpEnabled(true) // Show back arrow
        }
        toolbar.setNavigationOnClickListener { finish() }



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

        // 🚫 Basic validation
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
            Toast.makeText(this, "Invalid MAC Address format", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        val parentId = sharedPref.getString("parentId", null)

        if (parentId == null) {
            Toast.makeText(this, "Parent ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ MAC uniqueness check
        val childrenRef = database.getReference("Children")
        childrenRef.orderByChild("mac").equalTo(mac)
            .get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    Toast.makeText(this, "A child with this MAC address is already registered.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // ✅ Add new child if MAC is unique
                val childRef = childrenRef.push()
                val childId = childRef.key ?: run {
                    Toast.makeText(this, "Error generating child ID", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val childMap = mapOf(
                    "name" to name,
                    "passcode" to passcode,
                    "mac" to mac,
                    "parent_ids" to mapOf(parentId to true),
                    "screen_time" to 10,
                    "blur_delay" to 10,
                    "blur_intensity" to 90,
                    "fade_in" to 10,
                    "is_child_app_running" to false,
                    "mute" to true
                )


                childRef.setValue(childMap).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Child registered successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to register child", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "MAC uniqueness check failed: ${it.message}", Toast.LENGTH_SHORT).show()
                it.printStackTrace() //
            }
    }

    private fun isValidMacAddress(mac: String): Boolean {
        val regex = Regex("^([0-9A-Fa-f]{2}:){5}([0-9A-Fa-f]{2})$")
        return regex.matches(mac)
    }
}

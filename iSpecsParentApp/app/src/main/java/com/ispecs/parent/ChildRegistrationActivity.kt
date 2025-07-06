package com.ispecs.parent

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.FirebaseDatabase

class ChildRegistrationActivity : AppCompatActivity() {

    private lateinit var etChildId: EditText
    private lateinit var etChildName: EditText
    private lateinit var etChildPasscode: EditText
    private lateinit var etMacAddress: EditText
    private lateinit var btnRegister: Button

    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_registration)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Register Child"
            setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener { finish() }

        etChildId = findViewById(R.id.etChildId)
        etChildName = findViewById(R.id.etChildName)
        etChildPasscode = findViewById(R.id.etChildPasscode)
        etMacAddress = findViewById(R.id.etMacAddress)
        btnRegister = findViewById(R.id.btnRegisterChild)

        btnRegister.setOnClickListener {
            registerChild()
        }
    }

    private fun registerChild() {
        val childIdInput = etChildId.text.toString().trim()
        val name = etChildName.text.toString().trim()
        val passcode = etChildPasscode.text.toString().trim()
        val mac = etMacAddress.text.toString().trim()

        if (childIdInput.isEmpty() || name.isEmpty() || passcode.isEmpty() || mac.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!childIdInput.matches(Regex("^[a-zA-Z0-9_-]{4,20}$"))) {
            Toast.makeText(this, "Child ID must be 4–20 alphanumeric characters", Toast.LENGTH_LONG).show()
            return
        }

        if (name.length !in 3..30) {
            Toast.makeText(this, "Child name must be between 3 and 30 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (passcode.length != 4 || !passcode.all { it.isDigit() }) {
            Toast.makeText(this, "Passcode must be exactly 4 digits", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidMacAddress(mac)) {
            Toast.makeText(this, "Invalid MAC Address format", Toast.LENGTH_LONG).show()
            return
        }

        val sharedPref = getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        val parentId = sharedPref.getString("parentId", null)

        if (parentId == null) {
            Toast.makeText(this, "Parent ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        val childrenRef = database.getReference("Children")

        // ✅ Check for uniqueness using child_id field
        childrenRef.orderByChild("child_id").equalTo(childIdInput)
            .get().addOnSuccessListener { snapshot ->
                var exists = false
                for (child in snapshot.children) {
                    val existingMac = child.child("mac").getValue(String::class.java)
                    if (existingMac == mac) {
                        exists = true
                        break
                    }
                }

                if (exists) {
                    Toast.makeText(this, "Child ID and MAC already registered", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // ✅ Firebase auto-generated key
                val childRef = childrenRef.push()
                val generatedId = childRef.key ?: return@addOnSuccessListener

                // ✅ Store using "child_id" instead of "childId"
                val childMap = mapOf(
                    "firebaseKey" to generatedId,
                    "child_id" to childIdInput,
                    "name" to name,
                    "passcode" to passcode,
                    "mac" to mac,
                    "parent_ids" to mapOf(parentId to true),
                    "screen_time" to 10,
                    "blur_delay" to 10,
                    "blur_intensity" to 90,
                    "fade_in" to 10,
                    "is_child_app_running" to false,
                    "mute" to true,
                    "ispec_device_status" to "inactive"
                )

                childRef.setValue(childMap).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Child registered successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error checking child ID: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun isValidMacAddress(mac: String): Boolean {
        val regex = Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
        return regex.matches(mac)
    }
}

package com.ispecs.parent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var textViewVersion: TextView // Added for clarity

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Check if user is already logged in
        if (isUserLoggedIn()) {
            navigateToMainActivity()
            return // Skip the rest of onCreate if already logged in
        }

        val registerBtn = findViewById<Button>(R.id.register_btn)
        registerBtn.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.login_btn)
        textViewVersion = findViewById(R.id.version_txt) // Assuming R.id.version_txt exists

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (email.isEmpty()) {
                editTextEmail.error = "Email is required"
                editTextEmail.requestFocus()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                editTextEmail.error = "Enter a valid email"
                editTextEmail.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                editTextPassword.error = "Password is required"
                editTextPassword.requestFocus()
                return@setOnClickListener
            }
            // Add password length check if desired, e.g., password.length < 6

            loginUser(email, password)
        }

        displayAppVersion()
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isLoggedIn", false) && auth.currentUser != null
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }


    private fun displayAppVersion() {
        try {
            val packageInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            textViewVersion.text = "Version: $versionName"
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Could not get package info", e)
            textViewVersion.text = "Version: N/A"
        }
    }

    private fun loginUser(email: String, password: String) {
        buttonLogin.isEnabled = false // Disable button to prevent multiple clicks
        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    firebaseUser?.let {
                        val firebaseAuthUid = it.uid // This is the ID from Firebase Authentication
                        // Now fetch the custom parent_id from the /Parents node
                        fetchCustomParentId(firebaseAuthUid, email)
                    } ?: run {
                        Log.e(TAG, "Firebase user is null after successful auth task.")
                        Toast.makeText(baseContext, "Login failed: Could not get user details.", Toast.LENGTH_SHORT).show()
                        buttonLogin.isEnabled = true
                    }
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    buttonLogin.isEnabled = true
                }
            }
    }

    private fun fetchCustomParentId(firebaseAuthUid: String, email: String) {
        // Path to the parent's data in the /Parents collection
        val parentNodeRef = database.getReference("Parents").child(firebaseAuthUid)

        parentNodeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Retrieve the custom parent_id
                    val customParentId = dataSnapshot.child("parent_id").getValue(String::class.java)
                    val parentName = dataSnapshot.child("name").getValue(String::class.java) // Optional: retrieve name

                    if (customParentId != null) {
                        Log.d(TAG, "Successfully fetched customParentId: $customParentId")
                        saveLoginInfoToPrefs(email, firebaseAuthUid, customParentId, parentName ?: "N/A")
                        navigateToMainActivity()
                    } else {
                        Log.e(TAG, "Custom parent_id is null in the database for user: $firebaseAuthUid")
                        Toast.makeText(this@LoginActivity, "Login failed: Parent account data incomplete. Please try registering again or contact support.", Toast.LENGTH_LONG).show()
                        auth.signOut() // Sign out the user as essential data is missing
                        buttonLogin.isEnabled = true
                    }
                } else {
                    Log.e(TAG, "No data found in /Parents/$firebaseAuthUid. This might mean the registration process was incomplete or data was deleted.")
                    Toast.makeText(this@LoginActivity, "Login failed: Parent account not fully set up. Please try registering again or contact support.", Toast.LENGTH_LONG).show()
                    auth.signOut() // Sign out the user
                    buttonLogin.isEnabled = true
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "Database error while fetching parent_id: ${databaseError.message}", databaseError.toException())
                Toast.makeText(this@LoginActivity, "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                buttonLogin.isEnabled = true
            }
        })
    }

    private fun saveLoginInfoToPrefs(email: String, firebaseAuthUid: String, customParentId: String, name: String) {
        val sharedPreferences = getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", true)
        editor.putString("email", email)
        editor.putString("firebaseAuthUid", firebaseAuthUid) // Store Firebase Auth UID
        editor.putString("parentId", customParentId)      // Store the custom parent_id
        editor.putString("parentName", name)              // Store parent name
        editor.apply()
        Log.d(TAG, "Login info saved to SharedPreferences. parentId: $customParentId")
    }
}
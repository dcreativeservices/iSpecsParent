package com.ispecs.parent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.*
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonRegister: Button
    private lateinit var toolbar: Toolbar

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    companion object {
        private const val TAG = "RegisterActivity"
        private const val MAX_ID_GENERATION_ATTEMPTS = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // UI Initialization
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        editTextName = findViewById(R.id.editTextName)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonRegister = findViewById(R.id.register_btn)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        buttonRegister.setOnClickListener {
            val name = editTextName.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            when {
                name.isEmpty() -> {
                    editTextName.error = "Name is required"
                    editTextName.requestFocus()
                }
                email.isEmpty() -> {
                    editTextEmail.error = "Email is required"
                    editTextEmail.requestFocus()
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    editTextEmail.error = "Enter a valid email"
                    editTextEmail.requestFocus()
                }
                password.isEmpty() -> {
                    editTextPassword.error = "Password is required"
                    editTextPassword.requestFocus()
                }
                password.length < 6 -> {
                    editTextPassword.error = "Password should be at least 6 characters long"
                    editTextPassword.requestFocus()
                }
                else -> {
                    registerUser(name, email, password)
                }
            }
        }
    }

    private fun generateSixDigitRandomString(): String {
        val number = Random().nextInt(900000) + 100000
        return number.toString()
    }

    private fun registerUser(name: String, email: String, password: String) {
        buttonRegister.isEnabled = false
        Toast.makeText(this, "Registering...", Toast.LENGTH_SHORT).show()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    auth.currentUser?.let { firebaseUser ->
                        attemptToClaimParentId(firebaseUser.uid, name, email, 0)
                    } ?: run {
                        Log.e(TAG, "Firebase user is null after successful auth.")
                        handleRegistrationFailure("Could not get user details.", true)
                    }
                } else {
                    val message = if (authTask.exception is FirebaseAuthUserCollisionException) {
                        "The email address is already in use by another account."
                    } else {
                        authTask.exception?.message ?: "Authentication failed."
                    }
                    Log.e(TAG, "Firebase Authentication failed: $message")
                    handleRegistrationFailure(message, true)
                }
            }
    }

    private fun attemptToClaimParentId(firebaseAuthUid: String, name: String, email: String, attemptCount: Int) {
        if (attemptCount >= MAX_ID_GENERATION_ATTEMPTS) {
            auth.currentUser?.delete()
            handleRegistrationFailure("Could not generate a unique Parent ID. Please try again later.", true)
            return
        }

        val potentialId = generateSixDigitRandomString()
        val claimedIdRef = database.getReference("claimedParentIDs").child(potentialId)

        claimedIdRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                return if (currentData.value == null) {
                    currentData.value = true
                    Transaction.success(currentData)
                } else {
                    Transaction.abort()
                }
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                when {
                    error != null -> {
                        Log.e(TAG, "Error claiming ID: ${error.message}")
                        attemptToClaimParentId(firebaseAuthUid, name, email, attemptCount + 1)
                    }
                    committed -> {
                        Log.d(TAG, "Parent ID $potentialId claimed.")
                        saveParentData(firebaseAuthUid, potentialId, name, email)
                    }
                    else -> {
                        Log.d(TAG, "Parent ID $potentialId already claimed. Retrying...")
                        attemptToClaimParentId(firebaseAuthUid, name, email, attemptCount + 1)
                    }
                }
            }
        })
    }

    private fun saveParentData(uid: String, parentId: String, name: String, email: String) {
        val parentRef = database.getReference("Parents").child(uid)
        val data = mapOf(
            "parent_id" to parentId,
            "name" to name,
            "email" to email
        )

        parentRef.setValue(data).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                saveRegistrationInfoToPrefs(email, uid, parentId, name)
                Toast.makeText(this, "Registration successful! Your Parent ID is $parentId", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            } else {
                unclaimParentIdAndDeleteAuthUser(parentId, "Failed to save parent data.")
            }
        }
    }

    private fun unclaimParentIdAndDeleteAuthUser(parentId: String, failureMessage: String) {
        database.getReference("claimedParentIDs").child(parentId).removeValue().addOnCompleteListener {
            auth.currentUser?.delete()?.addOnCompleteListener {
                handleRegistrationFailure("$failureMessage Please try again.", true)
            }
        }
    }

    private fun handleRegistrationFailure(message: String, enableButton: Boolean) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        if (enableButton) buttonRegister.isEnabled = true
    }

    private fun saveRegistrationInfoToPrefs(email: String, uid: String, parentId: String, name: String) {
        val prefs = getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("email_last_registered", email)
            putString("firebaseAuthUid_last_registered", uid)
            putString("customParentId_last_registered", parentId)
            putString("parentName_last_registered", name)
            apply()
        }
        Log.d(TAG, "Saved temporary parent registration info: $parentId")
    }
}

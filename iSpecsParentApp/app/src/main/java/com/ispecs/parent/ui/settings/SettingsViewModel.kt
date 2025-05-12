package com.ispecs.parent.ui.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ispecs.parent.SplashActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // Firebase references
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    // ISpecs-related LiveData
    private val _blurIntensity = MutableLiveData<Int>()
    val blurIntensity: LiveData<Int> = _blurIntensity

    private val _blurDelay = MutableLiveData<Int>()
    val blurDelay: LiveData<Int> = _blurDelay

    private val _fadeIn = MutableLiveData<Int>()
    val fadeIn: LiveData<Int> = _fadeIn

    private val _mute = MutableLiveData<Boolean>()
    val mute: LiveData<Boolean> = _mute

    // Settings-related LiveData
    private val _parentId = MutableLiveData<String>()
    val parentId: LiveData<String> = _parentId

    private val _passcode = MutableLiveData<String>()
    val passcode: LiveData<String> = _passcode

    init {
        // Load parent ID from SharedPreferences
        val sharedPreferences = getApplication<Application>()
            .getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        _parentId.value = sharedPreferences.getString("parentId", "") ?: ""

        // Fetch all user data (ISpecs settings & child passcode)
        fetchUserData()
    }

    private fun fetchUserData() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            database.child("Users").child(userId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        _blurIntensity.value =
                            snapshot.child("blur_intensity").getValue(Int::class.java) ?: 80
                        _blurDelay.value =
                            snapshot.child("blur_delay").getValue(Int::class.java) ?: 2
                        _fadeIn.value =
                            snapshot.child("fade_in").getValue(Int::class.java) ?: 5
                        _mute.value =
                            snapshot.child("mute").getValue(Boolean::class.java) ?: true
                        _passcode.value =
                            snapshot.child("child_passcode").getValue(String::class.java) ?: ""
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Log or handle error as needed
                        Log.e("SettingsViewModel", "Failed to fetch user data", error.toException())
                    }
                })
        }
    }

    // --- ISpecs settings update methods ---

    fun updateBlurIntensity(value: Int) {
        updateValueInFirebase("blur_intensity", value)
    }

    fun updateBlurDelay(value: Int) {
        updateValueInFirebase("blur_delay", value)
    }

    fun updateFadeIn(value: Int) {
        updateValueInFirebase("fade_in", value)
    }

    fun setMute(isMuted: Boolean) {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            database.child("Users").child(userId).child("mute").setValue(isMuted)
                .addOnSuccessListener {
                    _mute.value = isMuted
                }
                .addOnFailureListener {
                    // Handle error if needed
                    Log.e("SettingsViewModel", "Failed to update mute", it)
                }
        }
    }

    private fun updateValueInFirebase(key: String, value: Int) {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            database.child("Users").child(userId).child(key).setValue(value)
                .addOnSuccessListener {
                    when (key) {
                        "blur_intensity" -> _blurIntensity.value = value
                        "blur_delay" -> _blurDelay.value = value
                        "fade_in" -> _fadeIn.value = value
                    }
                }
                .addOnFailureListener {
                    Log.e("SettingsViewModel", "Failed to update $key", it)
                }
        }
    }

    // --- Settings (child passcode & logout) methods ---

    fun updatePasscode(newPasscode: String) {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            database.child("Users").child(userId).child("child_passcode")
                .setValue(newPasscode)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _passcode.value = newPasscode
                    } else {
                        Log.e("SettingsViewModel", "Failed to update passcode", task.exception)
                    }
                }
        }
    }

    fun onLogoutClick() {
        // Clear SharedPreferences
        val sharedPreferences = getApplication<Application>()
            .getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        // Start SplashActivity
        val intent = Intent(getApplication(), SplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        getApplication<Application>().startActivity(intent)
    }
}

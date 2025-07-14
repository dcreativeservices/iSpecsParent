package com.ispecs.parent.ui.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.ispecs.parent.SplashActivity

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    private val _blurIntensity = MutableLiveData<Int>()
    val blurIntensity: LiveData<Int> = _blurIntensity

    private val _blurDelay = MutableLiveData<Int>()
    val blurDelay: LiveData<Int> = _blurDelay

    private val _fadeIn = MutableLiveData<Int>()
    val fadeIn: LiveData<Int> = _fadeIn

    private val _mute = MutableLiveData<Boolean>()
    val mute: LiveData<Boolean> = _mute

    private val _parentId = MutableLiveData<String>()
    val parentId: LiveData<String> = _parentId

    private val _passcode = MutableLiveData<String>()
    val passcode: LiveData<String> = _passcode

    private val _childName = MutableLiveData<String>()
    val childName: LiveData<String> = _childName

    private val _macAddress = MutableLiveData<String>()
    val macAddress: LiveData<String> = _macAddress

    private val _iSpecDeviceStatus = MutableLiveData<String>()
    val iSpecDeviceStatus: LiveData<String> = _iSpecDeviceStatus

    private var selectedChildId: String? = null

    init {
        val sharedPreferences = getApplication<Application>()
            .getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        _parentId.value = sharedPreferences.getString("parentId", "") ?: ""
    }

    fun loadChildrenList(callback: (List<Pair<String, String>>) -> Unit) {
        val parent = _parentId.value ?: return
        val childrenRef = database
            .child("Parent")
            .child(parent)
            .child("children")

        childrenRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val children = mutableListOf<Pair<String, String>>()

                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val firstName = child.child("childFirstName").getValue(String::class.java) ?: ""
                    val lastName = child.child("childLastName").getValue(String::class.java) ?: ""
                    val displayName = "$firstName $lastName".trim()

                    children.add(id to displayName)
                }

                callback(children)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SettingsViewModel", "Error loading children", error.toException())
                callback(emptyList())
            }
        })
    }



    private var childSettingsListener: ValueEventListener? = null

    fun loadSelectedChild(childId: String) {
        selectedChildId = childId

        val sharedPrefs = getApplication<Application>()
            .getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("selectedChildId", childId).apply()

        val firebaseAuthUid = FirebaseAuth.getInstance().currentUser?.uid
        if (firebaseAuthUid.isNullOrEmpty()) {
            Log.e("SettingsViewModel", "FirebaseAuth UID is null")
            return
        }

        val childRef = database
            .child("Parents")
            .child(firebaseAuthUid)
            .child("children")
            .child(childId)

        Log.d("SettingsViewModel", "Listening to child updates at: ${childRef.path}")

        // Remove previous listener if any
        childSettingsListener?.let { childRef.removeEventListener(it) }

        childSettingsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.e("SettingsViewModel", "Child not found at ${childRef.path}")
                    return
                }

                val firstName = snapshot.child("childFirstName").getValue(String::class.java) ?: ""
                val lastName = snapshot.child("childLastName").getValue(String::class.java) ?: ""
                val fullName = "$firstName $lastName".trim()
                _childName.value = if (fullName.isNotBlank()) fullName else "---"

                _blurIntensity.value = snapshot.child("blur_intensity").getValue(Int::class.java) ?: 80
                _blurDelay.value = snapshot.child("blur_delay").getValue(Int::class.java) ?: 2
                _fadeIn.value = snapshot.child("fade_in").getValue(Int::class.java) ?: 5
                _mute.value = snapshot.child("mute").getValue(Boolean::class.java) ?: true
                _passcode.value = snapshot.child("passcode").getValue(String::class.java) ?: ""
                _macAddress.value = snapshot.child("mac").getValue(String::class.java) ?: "--"
                _iSpecDeviceStatus.value = snapshot.child("ispec_device_status").getValue(String::class.java) ?: "--"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SettingsViewModel", "Failed to listen to child settings", error.toException())
            }
        }

        childRef.addValueEventListener(childSettingsListener as ValueEventListener)
    }

    override fun onCleared() {
        super.onCleared()
        selectedChildId?.let { childId ->
            val firebaseAuthUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val childRef = database.child("Parents").child(firebaseAuthUid).child("children").child(childId)
            childSettingsListener?.let { childRef.removeEventListener(it) }
        }
    }




    fun updateChildSetting(key: String, value: Any) {
        val childId = selectedChildId ?: return
        val firebaseAuthUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val updates = hashMapOf<String, Any>(
            "/Parents/$firebaseAuthUid/children/$childId/$key" to value,
            "/Children/$childId/$key" to value
        )

        database.updateChildren(updates)
            .addOnFailureListener {
                Log.e("SettingsViewModel", "Failed to update $key", it)
            }
    }



    fun onLogoutClick() {
        val sharedPreferences = getApplication<Application>()
            .getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        val intent = Intent(getApplication(), SplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        getApplication<Application>().startActivity(intent)
    }
}

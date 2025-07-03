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

    private var selectedChildId: String? = null

    init {
        val sharedPreferences = getApplication<Application>()
            .getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        _parentId.value = sharedPreferences.getString("parentId", "") ?: ""
    }

    fun loadChildrenList(callback: (List<Pair<String, String>>) -> Unit) {
        val parent = _parentId.value ?: return
        val childrenRef = database.child("Children")

        childrenRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val children = mutableListOf<Pair<String, String>>()

                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: "Unnamed"
                    val parentIds = child.child("parent_ids")

                    val isLinkedToParent = parentIds.hasChild(parent) &&
                            parentIds.child(parent).getValue(Boolean::class.java) == true

                    if (isLinkedToParent) {
                        children.add(id to name)
                    }
                }

                callback(children)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SettingsViewModel", "Error loading children", error.toException())
                callback(emptyList())
            }
        })
    }

    fun loadSelectedChild(childId: String) {
        selectedChildId = childId

        val sharedPrefs = getApplication<Application>()
            .getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("selectedChildId", childId).apply()

        val childRef = database.child("Children").child(childId)
        childRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _childName.value = snapshot.child("name").getValue(String::class.java) ?: ""
                _blurIntensity.value = snapshot.child("blur_intensity").getValue(Int::class.java) ?: 80
                _blurDelay.value = snapshot.child("blur_delay").getValue(Int::class.java) ?: 2
                _fadeIn.value = snapshot.child("fade_in").getValue(Int::class.java) ?: 5
                _mute.value = snapshot.child("mute").getValue(Boolean::class.java) ?: true
                _passcode.value = snapshot.child("passcode").getValue(String::class.java) ?: ""
                _macAddress.value = snapshot.child("mac").getValue(String::class.java) ?: "--"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SettingsViewModel", "Failed to load child settings", error.toException())
            }
        })
    }


    fun updateChildSetting(key: String, value: Any) {
        val childId = selectedChildId ?: return
        database.child("Children").child(childId).child(key).setValue(value)
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

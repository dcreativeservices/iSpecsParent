package com.ispecs.parent.ui.ispecs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class iSpecsViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private val _childAppStatus = MutableLiveData<Boolean>()
    val childAppStatus: LiveData<Boolean> get() = _childAppStatus

    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text

    init {
        fetchDataFromFirebase()
    }

    fun saveScreenTime(screenTime: Int) {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val updates: MutableMap<String, Any> = HashMap()
            updates["screen_time"] = screenTime
            val screenTimeRef = database.getReference("Users").child(userId)
            screenTimeRef.updateChildren(updates)
        }
    }

    private fun fetchDataFromFirebase() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val database: DatabaseReference = FirebaseDatabase.getInstance().reference
            database.child("Users").child(userId).addValueEventListener(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    _childAppStatus.value = snapshot.child("is_child_app_running").getValue(Boolean::class.java) ?: false
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }
}
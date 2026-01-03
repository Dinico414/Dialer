package com.xenonware.phone.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.xenonware.phone.data.SharedPreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PhoneViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = SharedPreferenceManager(application.applicationContext)
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _recentCalls = mutableListOf<CallLogEntry>()
    val recentCalls: List<CallLogEntry> get() = _recentCalls

    private val _favorites = mutableListOf<Contact>()
    val favorites: List<Contact> get() = _favorites

    private val _contacts = mutableListOf<Contact>()
    val contacts: List<Contact> get() = _contacts

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val syncingCallIds = mutableSetOf<String>()
    private val offlineCallIds = mutableSetOf<String>()

    init {
        loadLocalData()
        auth.currentUser?.uid?.let { uid ->
            startRealtimeSync(uid)
        }
    }

    private fun loadLocalData() {
        // Load from SharedPreferences or Room if needed
        // For now, we use empty lists – can be expanded
    }

    private fun startRealtimeSync(userId: String) {
        firestore.collection("phone").document(userId).collection("calls")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                snapshot.documentChanges.forEach { change ->
                    val call = change.document.toObject(CallLogEntry::class.java)
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            if (!offlineCallIds.contains(call.id)) {
                                if (_recentCalls.none { it.id == call.id }) {
                                    _recentCalls.add(0, call.copy(isOffline = false))
                                    saveLocalCalls()
                                }
                            }
                        }
                        DocumentChange.Type.MODIFIED -> {
                            if (!offlineCallIds.contains(call.id)) {
                                val index = _recentCalls.indexOfFirst { it.id == call.id }
                                if (index != -1) {
                                    _recentCalls[index] = call.copy(isOffline = false)
                                    saveLocalCalls()
                                }
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            if (!offlineCallIds.contains(call.id)) {
                                _recentCalls.removeAll { it.id == call.id }
                                saveLocalCalls()
                            }
                        }
                    }
                }
            }
    }

    private fun saveLocalCalls() {
        // Save to SharedPreferences or Room
        // For now, just keep in memory
    }

    fun onSignedIn() {
        val uid = auth.currentUser?.uid ?: return
        startRealtimeSync(uid)
        viewModelScope.launch {
            // Upload local-only calls if needed
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

data class CallLogEntry(
    val id: String,
    val number: String,
    val type: Int,
    val timestamp: Long,
    val duration: Long = 0,
    val isOffline: Boolean = false
)

data class Contact(
    val id: String,
    val name: String,
    val phone: String,
    val isFavorite: Boolean = false
)
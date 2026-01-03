package com.xenonware.phone.viewmodel

import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.telecom.TelecomManager
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.xenonware.phone.data.SharedPreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PhoneViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val prefsManager = SharedPreferenceManager(context)
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

    // Overlay state
    private val _showSetDefaultOverlay = MutableStateFlow(false)
    val showSetDefaultOverlay: StateFlow<Boolean> = _showSetDefaultOverlay.asStateFlow()

    private val syncingCallIds = mutableSetOf<String>()
    private val offlineCallIds = mutableSetOf<String>()

    init {
        loadLocalData()
        checkDefaultDialerStatus()

        auth.currentUser?.uid?.let { uid ->
            startRealtimeSync(uid)
        }
    }

    private fun loadLocalData() {}

    private fun startRealtimeSync(userId: String) {
        firestore.collection("phone").document(userId).collection("calls")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                snapshot.documentChanges.forEach { change ->
                    val call = change.document.toObject(CallLogEntry::class.java)
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            if (!offlineCallIds.contains(call.id) && _recentCalls.none { it.id == call.id }) {
                                _recentCalls.add(0, call.copy(isOffline = false))
                                saveLocalCalls()
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

    private fun saveLocalCalls() {}

    fun onSignedIn() {
        val uid = auth.currentUser?.uid ?: return
        startRealtimeSync(uid)
        checkDefaultDialerStatus()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun checkDefaultDialerStatus() {
        val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) && roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
            @Suppress("DEPRECATION")
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecomManager.defaultDialerPackage == context.packageName
        }
        _showSetDefaultOverlay.value = !isDefault
    }

    fun dismissSetDefaultOverlay() {
        _showSetDefaultOverlay.value = false
    }
}

data class CallLogEntry(
    val id: String = "",
    val number: String = "",
    val type: Int = 0,
    val timestamp: Long = 0L,
    val duration: Long = 0L,
    val isOffline: Boolean = false
)

data class Contact(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val isFavorite: Boolean = false
)
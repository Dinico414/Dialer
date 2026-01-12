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

    private val _recentCalls = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val recentCalls: StateFlow<List<CallLogEntry>> = _recentCalls.asStateFlow()

    private val _favorites = MutableStateFlow<List<Contact>>(emptyList())
    val favorites: StateFlow<List<Contact>> = _favorites.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showSetDefaultOverlay = MutableStateFlow(false)
    val showSetDefaultOverlay: StateFlow<Boolean> = _showSetDefaultOverlay.asStateFlow()

    private val syncingCallIds = mutableSetOf<String>()
    private val offlineCallIds = mutableSetOf<String>()

    init {
        _recentCalls.value = listOf(
            CallLogEntry(
                id = "call1",
                number = "+491701234567",
                type = 2, // incoming
                timestamp = System.currentTimeMillis() - 3_600_000,
                duration = 120,
                isOffline = false
            ),
            CallLogEntry(
                id = "call2",
                number = "089123456",
                type = 1, // outgoing
                timestamp = System.currentTimeMillis() - 86_400_000 * 2,
                duration = 45,
                isOffline = false
            ),
            CallLogEntry(
                id = "call3",
                number = "+49891234567",
                type = 3, // missed
                timestamp = System.currentTimeMillis() - 86_400_000 * 5,
                duration = 0,
                isOffline = false
            ),
            CallLogEntry(
                id = "call4",
                number = "+491609876543",
                type = 2,
                timestamp = System.currentTimeMillis() - 86_400_000 * 7,
                duration = 180,
                isOffline = false
            )
        )

        _favorites.value = listOf(
            Contact(id = "f1", name = "Mama", phone = "+491609876543", isFavorite = true),
            Contact(id = "f2", name = "Thomas Müller", phone = "017612345678", isFavorite = true),
            Contact(id = "f3", name = "Pizza Mario", phone = "0897654321", isFavorite = true),
            Contact(id = "f4", name = "Anna", phone = "+491701234567", isFavorite = true),
            Contact(id = "f5", name = "Chef", phone = "+4915123456789", isFavorite = true)
        )
        // ─────────────────────────────────────────────────────────────────────

        loadLocalData()
        checkDefaultDialerStatus()

        auth.currentUser?.uid?.let { uid ->
            startRealtimeSync(uid)
        }
    }

    private fun loadLocalData() {
        // TODO: Implement real local loading later (Room, SharedPrefs, etc.)
    }

    private fun startRealtimeSync(userId: String) {
        firestore.collection("phone").document(userId).collection("calls")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                snapshot.documentChanges.forEach { change ->
                    val call = change.document.toObject(CallLogEntry::class.java)
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            if (!offlineCallIds.contains(call.id) && _recentCalls.value.none { it.id == call.id }) {
                                _recentCalls.value = listOf(call.copy(isOffline = false)) + _recentCalls.value
                                saveLocalCalls()
                            }
                        }
                        DocumentChange.Type.MODIFIED -> {
                            if (!offlineCallIds.contains(call.id)) {
                                val current = _recentCalls.value.toMutableList()
                                val index = current.indexOfFirst { it.id == call.id }
                                if (index != -1) {
                                    current[index] = call.copy(isOffline = false)
                                    _recentCalls.value = current
                                    saveLocalCalls()
                                }
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            if (!offlineCallIds.contains(call.id)) {
                                _recentCalls.value = _recentCalls.value.filterNot { it.id == call.id }
                                saveLocalCalls()
                            }
                        }
                    }
                }
            }
    }

    private fun saveLocalCalls() {
        // TODO: Implement persistence if needed
    }

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
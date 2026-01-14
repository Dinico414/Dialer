package com.xenonware.phone.viewmodel

import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.xenonware.phone.data.Contact
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

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> =
        _contacts.asStateFlow()  

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showSetDefaultOverlay = MutableStateFlow(false)
    val showSetDefaultOverlay: StateFlow<Boolean> = _showSetDefaultOverlay.asStateFlow()

    private val offlineCallIds = mutableSetOf<String>()

    init {
        loadLocalData()
        checkDefaultDialerStatus()

        auth.currentUser?.uid?.let { uid ->
            startRealtimeSync(uid)
        }
    }

    private fun matchesNumber(phone: String, query: String): Boolean {
        if (query.isEmpty()) return true

        if (query.none { it.isDigit() || it in setOf('+', '*', '#', '-') }) {
            return false
        }

        val cleanPhone = phone.replace(Regex("[^+0-9*#-]"), "")
        val cleanQuery = query.replace(Regex("[^+0-9*#-]"), "")

        if (cleanQuery.isEmpty()) return false

        return cleanPhone.startsWith(cleanQuery) ||
                cleanPhone.contains(cleanQuery) ||
                cleanPhone.endsWith(cleanQuery)
    }

    private fun loadLocalData() {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            _recentCalls.value = loadCallLogs()
        }

        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val loaded = loadContacts()
            val withNumber = loaded.filter { it.phone.isNotBlank() }

            _contacts.value = withNumber
            _favorites.value = withNumber.filter { it.isFavorite }
        }
    }

    private fun loadCallLogs(): List<CallLogEntry> {
        val list = mutableListOf<CallLogEntry>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI, projection, null, null, "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                list += CallLogEntry(
                    id = cursor.getString(0) ?: "",
                    number = cursor.getString(1) ?: "",
                    type = cursor.getInt(2),
                    timestamp = cursor.getLong(3),
                    duration = cursor.getLong(4),
                    isOffline = false
                )
            }
        }
        return list
    }

    private fun loadContacts(): List<Contact> {
        val list = mutableListOf<Contact>()

        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.STARRED,
                ContactsContract.Contacts.HAS_PHONE_NUMBER
            ),
            "${ContactsContract.Contacts.HAS_PHONE_NUMBER} > 0",
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: ""
                val starred = cursor.getInt(2) == 1

                var phone = ""
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id),
                    "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC"
                )?.use { pCursor ->
                    if (pCursor.moveToFirst()) {
                        phone = pCursor.getString(0)?.trim() ?: ""
                    }
                }

                if (phone.isNotBlank()) {
                    list += Contact(id, name, phone, starred)
                }
            }
        }
        return list
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
                                _recentCalls.value =
                                    listOf(call.copy(isOffline = false)) + _recentCalls.value
                            }
                        }

                        DocumentChange.Type.MODIFIED -> {
                            if (!offlineCallIds.contains(call.id)) {
                                val current = _recentCalls.value.toMutableList()
                                val index = current.indexOfFirst { it.id == call.id }
                                if (index != -1) {
                                    current[index] = call.copy(isOffline = false)
                                    _recentCalls.value = current
                                }
                            }
                        }

                        DocumentChange.Type.REMOVED -> {
                            if (!offlineCallIds.contains(call.id)) {
                                _recentCalls.value =
                                    _recentCalls.value.filterNot { it.id == call.id }
                            }
                        }
                    }
                }
            }
    }

    private val _filteredContacts = MutableStateFlow<List<Contact>>(emptyList())
    val filteredContacts: StateFlow<List<Contact>> = _filteredContacts.asStateFlow()

    fun updateSearchQuery(query: String) {
        val trimmed = query.trim()
        _searchQuery.value = trimmed

        if (trimmed.isEmpty()) {
            _filteredContacts.value = _contacts.value
            return
        }

        val queryNorm = trimmed.lowercase()

        val filtered = _contacts.value.filter { contact ->
            val name = contact.name?.trim()?.lowercase() ?: ""

            val matchesName =
                name.contains(queryNorm) ||
                        name.startsWith(queryNorm) ||
                        name.split(Regex("\\s+")).any { it.startsWith(queryNorm) }

            matchesName || matchesNumber(contact.phone, trimmed)
        }

        _filteredContacts.value = filtered
    }

    fun checkDefaultDialerStatus() {
        val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            rm.isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
            @Suppress("DEPRECATION") val tm =
                context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            tm.defaultDialerPackage == context.packageName
        }
        _showSetDefaultOverlay.value = !isDefault
    }

    fun onSignedIn() {
        val uid = auth.currentUser?.uid ?: return
        startRealtimeSync(uid)
        checkDefaultDialerStatus()
    }
}

data class CallLogEntry(
    val id: String = "",
    val number: String = "",
    val type: Int = 0,
    val timestamp: Long = 0L,
    val duration: Long = 0L,
    val isOffline: Boolean = false,
)
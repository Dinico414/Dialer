package com.xenonware.phone.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.xenonware.phone.data.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IndexedContact(
    val contact: Contact,
    val normalizedPhone: String,
    val t9NormalizedName: String,
    val t9Keys: String,
)

data class CallLogEntry(
    val id: String = "",
    val number: String = "",
    val type: Int = 0,
    val timestamp: Long = 0L,
    val duration: Long = 0L,
    val isOffline: Boolean = false,
)

class PhoneViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _showSetDefaultOverlay = MutableStateFlow(false)
    val showSetDefaultOverlay: StateFlow<Boolean> = _showSetDefaultOverlay.asStateFlow()

    private val _recentCalls = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val recentCalls: StateFlow<List<CallLogEntry>> = _recentCalls.asStateFlow()

    private val _favorites = MutableStateFlow<List<Contact>>(emptyList())
    val favorites: StateFlow<List<Contact>> = _favorites.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _incomingPhoneNumber = MutableStateFlow<String?>(null)
    val incomingPhoneNumber: StateFlow<String?> = _incomingPhoneNumber.asStateFlow()

    private val _filteredContacts = MutableStateFlow<List<Contact>>(emptyList())
    val filteredContacts: StateFlow<List<Contact>> = _filteredContacts.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val offlineCallIds = mutableSetOf<String>()

    val indexedContacts = MutableStateFlow<List<IndexedContact>>(emptyList())

    init {
        loadLocalData()
        checkDefaultDialerStatus()

        auth.currentUser?.uid?.let { uid ->
            startRealtimeSync(uid)
        }

        viewModelScope.launch {
            contacts.collect { contactsList ->
                indexedContacts.value =
                    contactsList.filter { it.phone.isNotBlank() }.map { contact ->
                            IndexedContact(
                                contact = contact,
                                normalizedPhone = normalizePhone(contact.phone),
                                t9NormalizedName = normalizeName(contact.name),
                                t9Keys = nameToT9Keys(normalizeName(contact.name))
                            )
                        }.sortedBy { it.contact.name.lowercase() }
            }
        }
    }

    fun checkDefaultDialerStatus() {
        val rm = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        val isDefault = rm.isRoleHeld(RoleManager.ROLE_DIALER)
        _showSetDefaultOverlay.value = !isDefault
    }

    fun onSignedIn() {
        val uid = auth.currentUser?.uid ?: return
        startRealtimeSync(uid)
        checkDefaultDialerStatus()
    }

    fun setIncomingPhoneNumber(number: String) {
        _incomingPhoneNumber.value = number
    }

    fun updateSearchQuery(query: String) {
        val trimmed = query.trim()
        _searchQuery.value = trimmed

        if (trimmed.isEmpty()) {
            _filteredContacts.value = _contacts.value
            return
        }

        val queryNorm = trimmed.lowercase()

        val filtered = _contacts.value.filter { contact ->
            val name = contact.name.trim().lowercase()

            val matchesName =
                name.contains(queryNorm) || name.startsWith(queryNorm) || name.split(Regex("\\s+"))
                    .any { it.startsWith(queryNorm) }

            matchesName || matchesNumber(contact.phone, trimmed)
        }

        _filteredContacts.value = filtered
    }

    fun startCall(number: String) {
        if (number.isBlank()) return

        val uri = "tel:${Uri.encode(number)}".toUri()

        if (isDefaultDialer()) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val extras = Bundle().apply {
                putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
            }
            try {
                telecomManager.placeCall(uri, extras)
            } catch (_: SecurityException) {
                fallbackToIntent(uri)
            }
        } else {
            fallbackToIntent(uri)
        }
    }

    private fun isDefaultDialer(): Boolean {
        val rm = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        return rm.isRoleHeld(RoleManager.ROLE_DIALER)
    }

    private fun fallbackToIntent(uri: Uri) {
        val intent = Intent(Intent.ACTION_CALL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Call failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun startVoicemailCall() {
        // Most common carrier voicemail shortcuts in many countries (Germany/DE included):
        // "*86", "123", "500", "5500", "888", "999", or just your own number
        //
        // → Try "*86" first — works for many (including some Vodafone, Telekom variants)
        // → Fallback: dial your own number — many carriers send to voicemail when busy

        val voicemailCodes = listOf("*86", "123", "5500") // add more if you know your carrier

        val intent = Intent(Intent.ACTION_CALL).apply {
            // Try first code that might work
            data = "tel:${voicemailCodes.first()}".toUri()
        }

        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                context.startActivity(intent)
            } else {
                // fallback to dialer
                context.startActivity(Intent(Intent.ACTION_DIAL, intent.data))
            }
        } catch (_: Exception) {
            // Fallback: try dialing own number (common voicemail trigger when line busy)
            val ownNumber = getOwnPhoneNumber() // implement below or hardcode for testing
            if (!ownNumber.isNullOrBlank()) {
                startCall(ownNumber)
            } else {
                Toast.makeText(context, "Voicemail not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("HardwareIds")
    private fun getOwnPhoneNumber(): String? {
        val requiredPermission = Manifest.permission.READ_PHONE_STATE

        if (ContextCompat.checkSelfPermission(
                context,
                requiredPermission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return try {
            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            @Suppress("DEPRECATION")
            telephony.line1Number?.takeIf { it.isNotBlank() }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
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
                context, Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            _recentCalls.value = loadCallLogs()
        }

        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CONTACTS
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

    private fun nameToT9Keys(name: String): String = buildString {
        name.forEach { c ->
            t9Map.entries.find { it.value.contains(c) }?.key?.let { append(it) }
        }
    }

    companion object {
        private val t9Map = mapOf(
            '2' to "abc",
            '3' to "def",
            '4' to "ghi",
            '5' to "jkl",
            '6' to "mno",
            '7' to "pqrs",
            '8' to "tuv",
            '9' to "wxyz"
        )

        fun normalizeName(name: String): String {
            return name.lowercase().replace(Regex("[^a-z]"), "")
        }

        fun normalizePhone(number: String): String {
            if (number.isBlank()) return ""
            return number.replace(Regex("[^+0-9]"), "").removePrefix("00").removePrefix("+")
        }
    }
}
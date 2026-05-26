package com.xenonware.phone.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.phone.ui.layouts.call_history.CallLogEntry
import com.xenonware.phone.ui.layouts.call_history.loadCallLogEntries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CallHistoryViewModel : ViewModel() {

    private val _callLogs = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val callLogs: StateFlow<List<CallLogEntry>> = _callLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredCallLogs = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val filteredCallLogs: StateFlow<List<CallLogEntry>> = _filteredCallLogs.asStateFlow()

    private var indexedLogs: List<IndexedCallLog> = emptyList()

    fun loadCallLogs(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val entries = loadCallLogEntries(context)
                _callLogs.value = entries
                indexedLogs = entries.map { entry ->
                    IndexedCallLog(
                        entry = entry,
                        normalizedPhone = PhoneViewModel.normalizePhone(entry.phoneNumber),
                        t9Keys = PhoneViewModel.nameToT9Keys(PhoneViewModel.normalizeName(entry.nameOrNumber))
                    )
                }
                updateFilteredLogs()
                _hasPermission.value = entries.isNotEmpty() || hasReadCallLogPermission(context)
            } catch (e: SecurityException) {
                _hasPermission.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        updateFilteredLogs()
    }

    private fun updateFilteredLogs() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) {
            _filteredCallLogs.value = _callLogs.value
            return
        }

        val queryNorm = query.lowercase()
        val isDigitInput = query.all { it.isDigit() || it in "+*#-" }
        val cleanQuery = query.replace(Regex("[^+0-9*#-]"), "")
        val multiTapQuery = if (isDigitInput) PhoneViewModel.multiTapToT9(cleanQuery) else ""

        _filteredCallLogs.value = indexedLogs.filter { indexed ->
            val name = indexed.entry.nameOrNumber.lowercase()

            val matchesName = name.contains(queryNorm) || name.startsWith(queryNorm)

            val matchesT9 = isDigitInput && (
                PhoneViewModel.matchesT9(indexed.t9Keys, cleanQuery) ||
                (multiTapQuery.isNotEmpty() && PhoneViewModel.matchesT9(indexed.t9Keys, multiTapQuery))
            )

            val cleanPhone = indexed.entry.phoneNumber.replace(Regex("[^+0-9*#-]"), "")
            val matchesPhone = cleanPhone.contains(cleanQuery)

            matchesName || matchesT9 || matchesPhone
        }.map { it.entry }
    }

    private fun hasReadCallLogPermission(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

data class IndexedCallLog(
    val entry: CallLogEntry,
    val normalizedPhone: String,
    val t9Keys: String
)

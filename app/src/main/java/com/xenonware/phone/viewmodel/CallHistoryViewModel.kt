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

    fun loadCallLogs(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val entries = loadCallLogEntries(context)
                _callLogs.value = entries
                _hasPermission.value = entries.isNotEmpty() || hasReadCallLogPermission(context)
            } catch (e: SecurityException) {
                _hasPermission.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun hasReadCallLogPermission(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
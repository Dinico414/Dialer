package com.xenonware.phone.viewmodel

import android.content.Context
import android.telecom.Call
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CallScreenViewModelFactory(
    private val context: Context,
    private val call: Call?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CallScreenViewModel::class.java)) {
            return CallScreenViewModel(context.applicationContext, call) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
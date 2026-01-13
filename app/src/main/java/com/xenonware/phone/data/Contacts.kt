package com.xenonware.phone.data

data class Contact(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val isFavorite: Boolean = false,
)
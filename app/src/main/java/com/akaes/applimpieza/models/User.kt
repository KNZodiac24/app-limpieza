package com.akaes.applimpieza.models

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "", // "cliente" o "proveedor"
    val photoUrl: String = "",
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)
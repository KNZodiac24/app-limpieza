package com.akaes.applimpieza.models

data class Client(
    val id: String = "",
    val userId: String = "",
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)

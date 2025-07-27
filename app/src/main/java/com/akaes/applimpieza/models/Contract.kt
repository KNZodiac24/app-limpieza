package com.akaes.applimpieza.models

data class Contract(
    val id: String = "",
    val clientId: String = "",
    val providerId: String = "",
    val serviceType: String = "",
    val status: String = "", // "pendiente", "en_proceso", "completado", "cancelado"
    val description: String = "",
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val updatedAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val completedAt: com.google.firebase.Timestamp? = null
)
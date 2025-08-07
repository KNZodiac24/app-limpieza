package com.akaes.applimpieza.models

data class Provider(
    val id: String = "",
    val userId: String = "",
    val phone: String = "",
    val description: String = "",
    val serviceTypes: List<String> = emptyList(), // ["Reparacion", "Limpieza", "Decoracion", "Instalacion"]
    val completedJobs: Int = 0,
    val rating: Double = 0.0,
    val totalReviews: Int = 0,
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)

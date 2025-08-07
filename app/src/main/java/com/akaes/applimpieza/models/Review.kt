package com.akaes.applimpieza.models

data class Review(
    val id: String = "",
    val contractId: String = "",
    val clientId: String = "",
    val providerId: String = "",
    val rating: Int = 0, // 1-5
    val comment: String = "",
    val photos: List<String> = emptyList(), // URLs de fotos
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)

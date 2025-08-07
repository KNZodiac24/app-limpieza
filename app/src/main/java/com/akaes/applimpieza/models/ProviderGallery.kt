package com.akaes.applimpieza.models

data class ProviderGallery(
    val id: String = "",
    val providerId: String = "",
    val photoUrl: String = "",
    val description: String = "",
    val serviceType: String = "",
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)

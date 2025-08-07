package com.akaes.applimpieza

data class Mensaje(val texto: String = "",
                   val senderId: String = "",
                   val receiverId: String = "",
                   val tiempo: com.google.firebase.Timestamp? = null)

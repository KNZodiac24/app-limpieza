package com.akaes.applimpieza

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {
    private lateinit var txtFieldMensaje: EditText
    private lateinit var btnEnviar : Button
    private var listaMensajesChat = mutableListOf<Mensaje>()
    private lateinit var db : FirebaseFirestore
    private lateinit var adapter: MensajeAdapter
    private lateinit var btnBackToProfile : Button
    private lateinit var txtNombreDeChat: TextView
    // VARIABLES CON VALORES TEMPORALES DE PRUEBA, CAMBIAR VALORES CUANDO SE TERMINE EL LOGIN
    private var currentUserId: String = intent.getStringExtra("userId").toString()
    private var receiverId: String = intent.getStringExtra("providerid").toString()

    //@SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicio vars
        txtFieldMensaje = findViewById(R.id.textFieldMensaje)
        btnEnviar = findViewById(R.id.btnEnviar)
        btnBackToProfile = findViewById(R.id.btnBackToProfile)
        txtNombreDeChat = findViewById(R.id.txtNombreDeChat)

        /////////// CAMBIAR ESTA DEFINICIÓN
        txtNombreDeChat.text = receiverId // Tomar el nombre de la bd

        // Inicio Firestore
        db = FirebaseFirestore.getInstance()

        // Enviar mensaje
        btnEnviar.setOnClickListener {
            val texto = txtFieldMensaje.text.trim().toString()
            if(texto.isNotEmpty()){
                enviarMensaje(texto, currentUserId, receiverId)
                txtFieldMensaje.text.clear()
            }
        }

        // Configurar lista de mensajes
        adapter = MensajeAdapter(listaMensajesChat, currentUserId)
        val recyclerView = findViewById<RecyclerView>(R.id.listaMensajes)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Escuchar nuevos mensajes
        escucharMensajes (currentUserId, receiverId) { mensaje ->
            Log.d("Chat", "escucharMensajes activado")
            adapter.agregarMensaje(mensaje)
            recyclerView.scrollToPosition(listaMensajesChat.size - 1)
        }

        btnBackToProfile.setOnClickListener {
            var intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun enviarMensaje(texto: String, senderId: String, receiverId: String){
        val mensajeAEnviar = Mensaje(texto, senderId, receiverId, com.google.firebase.Timestamp.now())

        db.collection("chats")
            .add(mensajeAEnviar)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "Documento agregado con ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error al agregar documento", e)
            }
    }

    private fun escucharMensajes(senderId: String, receiverId: String, onNewMessage: (Mensaje) -> Unit) {
        db.collection("chats")
            .orderBy("tiempo")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Chat", "Error al recibir mensajes", e)
                    return@addSnapshotListener
                }

                Log.d("Chat", "Snapshot listener activado")
                for (doc in snapshots!!.documentChanges) {
                    if (doc.type == DocumentChange.Type.ADDED) {
                        val mensaje = doc.document.toObject(Mensaje::class.java)

                        val esConversacionEntreAmbos =
                            (mensaje.senderId == senderId && mensaje.receiverId == receiverId) ||
                                    (mensaje.senderId == receiverId && mensaje.receiverId == senderId)

                        if (esConversacionEntreAmbos) {
                            onNewMessage(mensaje)
                        }
                    }
                }
            }
    }
}
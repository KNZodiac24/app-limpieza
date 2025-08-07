package com.akaes.applimpieza

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akaes.applimpieza.models.Contract
import com.akaes.applimpieza.models.Provider
import com.akaes.applimpieza.models.Review
import com.bumptech.glide.Glide
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var toolbar: Toolbar
    private lateinit var btnContactar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // Inicializar variables
        toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val providerId = intent.getStringExtra("providerId") ?: return
        val userId = intent.getStringExtra("userId") ?: return

        db.collection("providers").document(providerId).get()
            .addOnSuccessListener { provDoc ->
                val provider = provDoc.toObject(Provider::class.java) ?: return@addOnSuccessListener
                mostrarProveedor(provider, userId)
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se pudo cargar el proveedor", Toast.LENGTH_SHORT).show()
            }

        setupGallery(providerId)
        setupReviews(providerId)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnContactar = findViewById(R.id.btnContactar)

        btnContactar.setOnClickListener {
            val clientId = auth.currentUser?.uid
            if (clientId == null) {
                Toast.makeText(this, "Debes iniciar sesión como cliente", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            crearContrato(clientId, providerId)
            var intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("providerId", intent.getStringExtra("providerId"))
            intent.putExtra("userId", intent.getStringExtra("userId")) // Pasarle el currentUserId y el id de la otra persona con la que chatea
            startActivity(intent)
        }
    }

    private fun mostrarProveedor(provider: Provider, userId: String) {
        // 1) Consulta el usuario para nombre y foto
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val name = userDoc.getString("name") ?: "Proveedor"
                val photoUrl = userDoc.getString("photoUrl").orEmpty()

                findViewById<TextView>(R.id.txtProfileName).text = name
                Glide.with(this)
                    .load(photoUrl.ifEmpty { R.drawable.ic_launcher_foreground })
                    .circleCrop()
                    .into(findViewById(R.id.imageView11))
            }

        // 2) Estadísticas
        findViewById<TextView>(R.id.textView8).text =
            "${provider.completedJobs} trabajos completados"
        findViewById<TextView>(R.id.textView9).text =
            "${provider.rating} ⭐"

        // 3) Descripción
        findViewById<TextView>(R.id.textView10).text = provider.description

        // 4) Chips dinámicos de serviceTypes
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupProfile)
        chipGroup.removeAllViews()
        provider.serviceTypes.forEach { tipo ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = tipo
                isClickable = false
                isCheckable = false
            }
            chipGroup.addView(chip)
        }
    }

    private fun setupGallery(providerId: String) {
        val recycler = findViewById<RecyclerView>(R.id.recyclerGallery)
        recycler.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        db.collection("provider_gallery")
            .whereEqualTo("providerId", providerId)
            .get()
            .addOnSuccessListener { snap ->
                // Extraemos todos los arrays 'photoUrls' de cada doc
                val allUrls = snap.documents
                    .flatMap { doc ->
                        @Suppress("UNCHECKED_CAST")
                        (doc.get("photoUrls") as? List<String>) ?: emptyList()
                    }

                // Si no hay nada, mostrar un mensaje
                if (allUrls.isEmpty()) {
//                    findViewById<TextView>(R.id.emptyGalleryText).apply {
//                        text = "No hay fotos en la galería"
//                        visibility = android.view.View.VISIBLE
//                    }
                }

                recycler.adapter = GalleryAdapter(allUrls)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar galería", Toast.LENGTH_SHORT).show()
                Log.e("ProfileActivity", "Galería fallo", e)
            }
    }

    private fun setupReviews(providerId: String) {
        val recycler = findViewById<RecyclerView>(R.id.recyclerReviews)
        recycler.layoutManager = LinearLayoutManager(this)

        db.collection("reviews")
            .whereEqualTo("providerId", providerId)
            .get()
            .addOnSuccessListener { snap ->
                val lista = snap.toObjects(Review::class.java)
                recycler.adapter = ReviewAdapter(lista)
            }
    }

    private fun crearContrato(clientId: String, providerId: String) {
        // Generamos un ID único
        val ref = db.collection("contracts").document()
        val nuevoContrato = Contract(
            id = ref.id,
            clientId = clientId,
            providerId = providerId,
            serviceType = "",
            status = "pendiente",
            description = ""
        )

        ref.set(nuevoContrato)
            .addOnSuccessListener {
                Toast.makeText(this, "Contrato creado correctamente", Toast.LENGTH_SHORT).show()
                // Abrir chat
                val intent = Intent(this, ChatActivity::class.java)
                // Si quieres pasar datos al chat, añádelos aquí
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error al crear contrato", e)
                Toast.makeText(this, "No se pudo crear el contrato", Toast.LENGTH_SHORT).show()
            }
    }
}
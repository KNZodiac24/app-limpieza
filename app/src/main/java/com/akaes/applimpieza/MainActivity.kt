package com.akaes.applimpieza

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akaes.applimpieza.models.Provider
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private val database: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private lateinit var recyclerView: RecyclerView
    private lateinit var providerAdapter: ProviderAdapter
    private val providerList = mutableListOf<Provider>()
    private lateinit var btnReparacion: View
    private lateinit var btnLimpieza: View
    private lateinit var btnDecoracion: View
    private lateinit var btnInstalacion: View
    private lateinit var chips: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // RecyclerView y Adapter
        recyclerView = findViewById(R.id.recyclerProveedores)
        providerAdapter = ProviderAdapter(providerList) { provider ->
            val intent = Intent(this, ProfileActivity::class.java).apply {
                putExtra("providerId", provider.id)
                putExtra("userId", provider.userId)
            }
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = providerAdapter

        // Inicializar variables
        chips = findViewById(R.id.chipGroupServices)

        obtenerProveedoresPorCategoria(null)

        chips.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipAll -> obtenerProveedoresPorCategoria(null)
                R.id.chipReparacion -> obtenerProveedoresPorCategoria("Reparacion")
                R.id.chipLimpieza -> obtenerProveedoresPorCategoria("Limpieza")
                R.id.chipDecoracion -> obtenerProveedoresPorCategoria("Decoracion")
                R.id.chipInstalacion -> obtenerProveedoresPorCategoria("Instalacion")
            }
        }
    }

    private fun obtenerProveedoresPorCategoria(categoria: String?) {
        val query = if (categoria.isNullOrEmpty()) {
            database.collection("providers")
        } else {
            database.collection("providers")
                .whereArrayContains("serviceTypes", categoria)
        }

        query.get()
            .addOnSuccessListener { provSnap ->
                // 1) Pobla la lista de providers
                providerList.clear()
                val nuevos = provSnap.toObjects(Provider::class.java)
                providerList.addAll(nuevos)
                providerAdapter.notifyDataSetChanged()

                // 2) Extrae los userIds únicos
                val userIds = nuevos.map { it.userId }.distinct()
                if (userIds.isEmpty()) return@addOnSuccessListener

                // 3) Trae todos los usuarios en un whereIn
                database.collection("users")
                    .whereIn("id", userIds)
                    .get()
                    .addOnSuccessListener { userSnap ->
                        for (doc in userSnap.documents) {
                            val id = doc.getString("id") ?: continue
                            val name = doc.getString("name") ?: "Proveedor"
                            val photo = doc.getString("photoUrl") ?: ""
                            providerAdapter.userNameMap[id] = name
                            providerAdapter.photoUrlMap[id] = photo
                        }
                        // 4) Refresca el RecyclerView con nombres y fotos cargados
                        providerAdapter.notifyDataSetChanged()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar proveedores", Toast.LENGTH_SHORT).show()
                Log.e("Firestore", "Error al obtener proveedores", e)
            }
    }
}
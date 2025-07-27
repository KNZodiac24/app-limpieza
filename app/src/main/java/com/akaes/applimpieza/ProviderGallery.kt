package com.akaes.applimpieza

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager

import com.akaes.applimpieza.databinding.ActivityProviderGalleryBinding
import com.akaes.applimpieza.repository.FirebaseRepository
import kotlinx.coroutines.launch

class ProviderGalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProviderGalleryBinding
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var galleryAdapter: ProviderGalleryAdapter
    private var currentProviderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        initializeRepository()
        setupRecyclerView()
        setupListeners()
        loadProviderGallery()
    }

    override fun onResume() {
        super.onResume()
        loadProviderGallery()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun initializeRepository() {
        firebaseRepository = FirebaseRepository()
        firebaseRepository.initializeCloudinary(this)
    }

    private fun setupRecyclerView() {
        galleryAdapter = ProviderGalleryAdapter(emptyList())
        binding.recyclerViewGallery.apply {
            layoutManager = LinearLayoutManager(this@ProviderGalleryActivity)
            adapter = galleryAdapter
        }
    }

    private fun setupListeners() {
        binding.btnAddWorkPhoto.setOnClickListener {
            showAddPhotoDialog()
        }
    }

    private fun loadProviderGallery() {
        val userId = firebaseRepository.getCurrentUserId()
        if (userId.isNullOrEmpty()) {
            showError("Error: Usuario no autenticado")
            showEmptyState()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                // Obtener providerId
                val providerIdResult = firebaseRepository.getProviderIdByUserId(userId)
                providerIdResult.fold(
                    onSuccess = { providerId ->
                        if (providerId != null) {
                            currentProviderId = providerId
                            loadGalleryData(providerId)
                        } else {
                            showError("No se encontró un perfil de proveedor para este usuario.")
                            showEmptyState()
                        }
                    },
                    onFailure = { e ->
                        showError("Error al obtener ID de proveedor: ${e.message}")
                        showEmptyState()
                    }
                )
            } catch (e: Exception) {
                showError("Error inesperado: ${e.message}")
                showEmptyState()
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun loadGalleryData(providerId: String) {
        try {
            println("DEBUG: Cargando galería para providerId: '$providerId'")

            // Usar el método sin orderBy que ya funciona
            val galleryResult = firebaseRepository.getProviderGallery(providerId)
            galleryResult.fold(
                onSuccess = { items ->
                    println("DEBUG: ✅ Galería cargada exitosamente: ${items.size} items")
                    if (items.isNotEmpty()) {
                        galleryAdapter.updateData(items)
                        showGalleryContent()
                    } else {
                        showEmptyState()
                    }
                },
                onFailure = { e ->
                    println("DEBUG: ❌ Error cargando galería: ${e.message}")
                    showError("Error al cargar galería: ${e.message}")
                    showEmptyState()
                }
            )
        } catch (e: Exception) {
            println("DEBUG: ❌ Exception en loadGalleryData: ${e.message}")
            showError("Error inesperado: ${e.message}")
            showEmptyState()
        }
    }

    private fun showAddPhotoDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_gallery_item, null)
        val etServiceType = dialogView.findViewById<EditText>(R.id.etServiceTypeDialog)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescriptionDialog)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Agregar Fotos de Trabajo")
            .setView(dialogView)
            .setPositiveButton("Siguiente", null)
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val serviceType = etServiceType.text.toString().trim()
                val description = etDescription.text.toString().trim()

                if (validateDialogInput(serviceType, description)) {
                    navigateToUploadImageActivity(serviceType, description)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun validateDialogInput(serviceType: String, description: String): Boolean {
        return when {
            serviceType.isEmpty() -> {
                Toast.makeText(this, "Por favor, ingresa el tipo de servicio.", Toast.LENGTH_SHORT).show()
                false
            }
            description.isEmpty() -> {
                Toast.makeText(this, "Por favor, ingresa una descripción.", Toast.LENGTH_SHORT).show()
                false
            }
            serviceType.length < 3 -> {
                Toast.makeText(this, "El tipo de servicio debe tener al menos 3 caracteres.", Toast.LENGTH_SHORT).show()
                false
            }
            description.length < 10 -> {
                Toast.makeText(this, "La descripción debe tener al menos 10 caracteres.", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun navigateToUploadImageActivity(serviceType: String, description: String) {
        val intent = Intent(this, UploadImageActivity::class.java).apply {
            putExtra(UploadImageActivity.EXTRA_UPLOAD_CONTEXT, "provider_gallery")
            putExtra(UploadImageActivity.EXTRA_SERVICE_TYPE, serviceType)
            putExtra(UploadImageActivity.EXTRA_DESCRIPTION, description)
        }
        startActivity(intent)
    }

    // Métodos de utilidad para UI
    private fun showLoading(show: Boolean) {
        // Implementar si tienes un ProgressBar
        println("DEBUG: Loading: $show")
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        println("DEBUG: Error: $message")
    }

    private fun showEmptyState() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.recyclerViewGallery.visibility = View.GONE
    }

    private fun showGalleryContent() {
        binding.layoutEmptyState.visibility = View.GONE
        binding.recyclerViewGallery.visibility = View.VISIBLE
    }
}
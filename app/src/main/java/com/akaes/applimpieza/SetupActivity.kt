package com.akaes.applimpieza
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import java.util.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.akaes.applimpieza.databinding.ActivitySetupBinding
import com.akaes.applimpieza.repository.FirebaseRepository
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat

class SetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupBinding
    private val repository = FirebaseRepository()
    private val etDescription: String = ""
    private var userId: String = ""
    private var selectedImageUri: Uri? = null
    private var selectedServices: MutableSet<String> = mutableSetOf()
    private var currentPhotoPath: String = ""

    // Launcher para seleccionar imagen de galería
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            displaySelectedImage(it)
        }
    }

    // Launcher para tomar foto con cámara
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            selectedImageUri = Uri.fromFile(File(currentPhotoPath))
            displaySelectedImage(selectedImageUri!!)
        }
    }

    // Launcher para permisos de cámara
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            showError("Se necesita permiso de cámara para tomar fotos")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ✨ Inicializar Cloudinary ANTES de todo
        Log.d("","DEBUG: 🚀 Inicializando Cloudinary en SetupActivity...")
        repository.initializeCloudinary(this)

        // Obtener userId del intent
        userId = intent.getStringExtra("userId") ?: repository.getCurrentUserId() ?: ""

        if (userId.isEmpty()) {
            showError("Error: Usuario no encontrado")
            finish()
            return
        }

        setupUI()
        setupListeners()
        loadUserInfo()
        loadProviderInfo()
    }

    private fun setupUI() {
        // Los chips empiezan sin seleccionar (según tu XML, "Instalación" viene seleccionado)
        selectedServices.add("Instalacion") // Instalación viene seleccionado por defecto
        updateChipAppearance()
    }

    private fun setupListeners() {
        // Seleccionar foto de perfil - mostrar opciones
        binding.frameProfileImage.setOnClickListener {
            showImagePickerDialog()
        }

        // Chips de especialización
        binding.chipInstalacion.setOnClickListener { toggleChip("Instalacion", binding.chipInstalacion) }
        binding.chipLimpieza.setOnClickListener { toggleChip("Limpieza", binding.chipLimpieza) }
        binding.chipDecoracion.setOnClickListener { toggleChip("Decoracion", binding.chipDecoracion) }
        binding.chipReparacion.setOnClickListener { toggleChip("Reparacion", binding.chipReparacion) }

        // Botón continuar
        binding.btnContinuar.setOnClickListener {
            completeProviderProfile()
        }
    }
    private fun loadProviderInfo() {
        lifecycleScope.launch {
            repository.getProviderByUserId(userId).onSuccess { provider ->
                provider?.let {
                    binding.etCelular.setText(it.phone)
                    binding.etDescripcion.setText(it.description)
                    selectedServices.clear()
                    selectedServices.addAll(it.serviceTypes)
                    updateChipAppearance()
                }
            }.onFailure {
                showError("Error al cargar datos de proveedor: ${it.message}")
            }
        }
    }


    private fun showImagePickerDialog() {
        val options = arrayOf("Tomar foto", "Seleccionar de galería", "Cancelar")

        AlertDialog.Builder(this)
            .setTitle("Seleccionar foto de perfil")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> openGallery()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            selectedImageUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )
            cameraLauncher.launch(selectedImageUri)
        } catch (e: Exception) {
            showError("Error al abrir la cámara: ${e.message}")
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun cleanupTempFile() {
        if (currentPhotoPath.isNotEmpty()) {
            try {
                val file = File(currentPhotoPath)
                if (file.exists()) {
                    file.delete()
                    println("DEBUG: Archivo temporal eliminado: $currentPhotoPath")
                }
            } catch (e: Exception) {
                println("DEBUG: Error eliminando archivo temporal: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupTempFile()
    }

    private fun displaySelectedImage(imageUri: Uri) {
        Glide.with(this@SetupActivity)
            .load(imageUri)
            .circleCrop()
            .placeholder(R.drawable.ic_image_placeholder) // Asegúrate de tener este drawable
            .error(R.drawable.ic_image_error)
            .into(binding.ivProfileImage)
    }

    private fun loadUserInfo() {
        lifecycleScope.launch {
            repository.getUser(userId)
                .onSuccess { user ->
                    user?.let {
                        binding.tvSaludo.text = "Hola, ${it.name}"

                        // Cargar foto de perfil si existe
                        if (it.photoUrl.isNotEmpty()) {
                            Glide.with(this@SetupActivity)
                                .load(it.photoUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_error)
                                .into(binding.ivProfileImage)
                        }
                    }
                }
                .onFailure { exception ->
                    showError("Error al cargar información del usuario: ${exception.message}")
                }
        }
    }

    private fun toggleChip(service: String, chipView: TextView) {
        if (selectedServices.contains(service)) {
            selectedServices.remove(service)
        } else {
            selectedServices.add(service)
        }
        updateChipAppearance()
    }

    private fun updateChipAppearance() {
        // Actualizar apariencia de cada chip
        updateSingleChip(binding.chipInstalacion, "Instalacion")
        updateSingleChip(binding.chipLimpieza, "Limpieza")
        updateSingleChip(binding.chipDecoracion, "Decoracion")
        updateSingleChip(binding.chipReparacion, "Reparacion")
    }

    private fun updateSingleChip(chipView: TextView, service: String) {
        if (selectedServices.contains(service)) {
            // Chip seleccionado
            chipView.setBackgroundResource(R.drawable.chip_background_selected)
            chipView.setTextColor(resources.getColor(android.R.color.white, null))
        } else {
            // Chip no seleccionado
            chipView.setBackgroundResource(R.drawable.chip_background_unselected)
            chipView.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }
    }

    private fun completeProviderProfile() {
        val phone = binding.etCelular.text.toString().trim()
        val description = binding.etDescripcion.text.toString().trim()

        // Validaciones
        if (!validateInputs(phone, description)) {
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                var photoUploadSuccess = true
                var newPhotoUrl = ""

                // 1. Subir nueva foto de perfil si se seleccionó una
                if (selectedImageUri != null) {
                    // Verificar que la URI sea válida
                    if (!isValidImageUri(selectedImageUri!!)) {
                        showLoading(false)
                        showError("Error: La imagen seleccionada no es válida")
                        return@launch
                    }

                    Log.d("DEBUG:" ,"Intentando subir imagen con URI: $selectedImageUri")

                    repository.uploadProfilePhoto(userId, selectedImageUri!!)
                        .onSuccess { url ->
                            newPhotoUrl = url
                            Log.d("DEBUG:", "Imagen subida exitosamente. URL: $url")

                            // Actualizar URL de foto en el documento de usuario
                            repository.updateUserPhoto(userId, url)
                                .onFailure { exception ->
                                    Log.d("DEBUG:","Error actualizando foto en usuario: ${exception.message}")
                                    showError("Advertencia: Error al actualizar foto en perfil de usuario")
                                    // No detener el proceso por esto
                                }
                        }
                        .onFailure { exception ->
                            Log.d("DEBUG:" ,"Error subiendo imagen: ${exception.message}")
                            photoUploadSuccess = false

                            // Mostrar error específico pero continuar sin imagen
                            showError("No se pudo subir la imagen, continuando sin foto de perfil")

                            // Opcional: Continuar sin imagen o detener el proceso
                            // Descomenta la siguiente línea si quieres detener el proceso
                            // showLoading(false)
                            // return@launch
                        }
                }

                // 2. Actualizar información del proveedor (siempre continuar)
                Log.d("DEBUG:", "Actualizando información del proveedor...")

                repository.updateProviderInfo(
                    userId = userId,
                    phone = phone,
                    description = description,
                    serviceTypes = selectedServices.toList()
                )
                    .onSuccess {
                        showLoading(false)
                        val message = if (photoUploadSuccess && selectedImageUri != null) {
                            "¡Perfil completado exitosamente!"
                        } else if (selectedImageUri != null) {
                            "Perfil completado, pero no se pudo subir la imagen"
                        } else {
                            "¡Perfil completado exitosamente!"
                        }
                        showSuccess(message)
                        navigateToProviderHome()
                    }
                    .onFailure { exception ->
                        showLoading(false)
                        showError("Error al completar perfil: ${exception.message}")
                        println("DEBUG: Error actualizando proveedor: ${exception.message}")
                    }

            } catch (e: Exception) {
                showLoading(false)
                showError("Error inesperado: ${e.message}")
                println("DEBUG: Error inesperado: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun isValidImageUri(uri: Uri): Boolean {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            inputStream?.use {
                it.available() > 0
            } ?: false
        } catch (e: Exception) {
            println("DEBUG: Error validando URI: ${e.message}")
            false
        }
    }

    private fun validateInputs(phone: String, description: String): Boolean {
        // Limpiar errores previos
        binding.etCelular.error = null
        binding.etDescripcion.error = null

        // Validar teléfono
        if (phone.isEmpty()) {
            binding.etCelular.error = "El número de celular es requerido"
            binding.etCelular.requestFocus()
            return false
        }

        if (phone.length < 8) {
            binding.etCelular.error = "Número de teléfono inválido"
            binding.etCelular.requestFocus()
            return false
        }

        // Validar que solo contenga números
        if (!phone.matches(Regex("^[0-9]+$"))) {
            binding.etCelular.error = "El teléfono solo debe contener números"
            binding.etCelular.requestFocus()
            return false
        }

        // Validar descripción
        if (description.isEmpty()) {
            binding.etDescripcion.error = "La descripción es requerida"
            binding.etDescripcion.requestFocus()
            return false
        }

        if (description.length < 20) {
            binding.etDescripcion.error = "La descripción debe tener al menos 20 caracteres"
            binding.etDescripcion.requestFocus()
            return false
        }

        if (description.length > 500) {
            binding.etDescripcion.error = "La descripción no puede exceder 500 caracteres"
            binding.etDescripcion.requestFocus()
            return false
        }

        // Validar especialización
        if (selectedServices.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos una especialización", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    private fun showLoading(show: Boolean) {
        binding.btnContinuar.isEnabled = !show
        binding.frameProfileImage.isEnabled = !show
        binding.etCelular.isEnabled = !show
        binding.etDescripcion.isEnabled = !show

        // Deshabilitar chips durante carga
        binding.chipInstalacion.isEnabled = !show
        binding.chipLimpieza.isEnabled = !show
        binding.chipDecoracion.isEnabled = !show
        binding.chipReparacion.isEnabled = !show

        if (show) {
            binding.btnContinuar.text = "Completando perfil..."
        } else {
            binding.btnContinuar.text = getString(R.string.txtContinuar)
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToProviderHome() {
        val intent = Intent(this, UploadImageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
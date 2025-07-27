package com.akaes.applimpieza

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.akaes.applimpieza.databinding.ActivityUploadImageBinding
import com.akaes.applimpieza.repository.FirebaseRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class UploadImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadImageBinding
    private lateinit var firebaseRepository: FirebaseRepository
    private var currentPhotoPath: String = ""
    private val selectedImages = mutableListOf<Uri>() // Cambiar a Uri para mejor manejo
    private val uploadedImageUrls = mutableListOf<String>()

    // Contexto de uso: "profile", "review", "provider_gallery"
    private var uploadContext: String = "provider_gallery" // Por defecto para galería del proveedor

    // Datos adicionales según el contexto
    private var serviceType: String = ""
    private var description: String = ""
    private var contractId: String = ""

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val STORAGE_PERMISSION_REQUEST_CODE = 101

        // Extras para el Intent
        const val EXTRA_UPLOAD_CONTEXT = "upload_context"
        const val EXTRA_SERVICE_TYPE = "service_type"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_CONTRACT_ID = "contract_id"
    }

    // ActivityResultLauncher para la cámara
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoPath.isNotEmpty()) {
            val imageUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                File(currentPhotoPath)
            )
            selectedImages.add(imageUri)
            displaySelectedImages()
            Toast.makeText(this, "Foto tomada exitosamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error al tomar la foto", Toast.LENGTH_SHORT).show()
        }
    }

    // ActivityResultLauncher para seleccionar desde galería
    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImages.add(it)
            displaySelectedImages()
            Toast.makeText(this, "Imagen seleccionada exitosamente", Toast.LENGTH_SHORT).show()
        }
    }

    // ActivityResultLauncher para múltiples imágenes de la galería
    private val selectMultipleImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris?.forEach { uri ->
            selectedImages.add(uri)
        }
        if (uris?.isNotEmpty() == true) {
            displaySelectedImages()
            Toast.makeText(this, "${uris.size} imágenes seleccionadas", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inflar el layout usando View Binding
        binding = ActivityUploadImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar Firebase Repository y Cloudinary
        firebaseRepository = FirebaseRepository()
        firebaseRepository.initializeCloudinary(this)

        // Obtener contexto del intent
        getIntentData()

        setupClickListeners()
        updateUIForContext()
    }

    private fun getIntentData() {
        uploadContext = intent.getStringExtra(EXTRA_UPLOAD_CONTEXT) ?: "provider_gallery"
        serviceType = intent.getStringExtra(EXTRA_SERVICE_TYPE) ?: ""
        description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""
        contractId = intent.getStringExtra(EXTRA_CONTRACT_ID) ?: ""
    }

    private fun updateUIForContext() {
        when (uploadContext) {
            "profile" -> {
                title = "Foto de Perfil"
                binding.btnContinuar.text = "Guardar Foto de Perfil"
            }
            "review" -> {
                title = "Fotos de Reseña"
                binding.btnContinuar.text = "Agregar a Reseña"
            }
            "provider_gallery" -> {
                title = "Fotos de Trabajos"
                binding.btnContinuar.text = "Agregar a Galería"
            }
        }
    }

    private fun setupClickListeners() {
        binding.layoutImageHolder.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnContinuar.setOnClickListener {
            if (selectedImages.isNotEmpty()) {
                uploadImagesToCloudinary()
            } else {
                if (uploadContext == "profile") {
                    Toast.makeText(this, "Selecciona al menos una foto de perfil", Toast.LENGTH_SHORT).show()
                } else {
                    // Para provider_gallery, permitir continuar sin imágenes
                    navigateToNextScreen()
                }
            }
        }
    }

    private fun uploadImagesToCloudinary() {
        if (selectedImages.isEmpty()) {
            navigateToNextScreen()
            return
        }

        // Mostrar progreso
        binding.progressBar.visibility = View.VISIBLE
        binding.btnContinuar.isEnabled = false
        binding.btnContinuar.text = "Subiendo imágenes..."

        val userId = firebaseRepository.getCurrentUserId()
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            resetUploadState()
            return
        }

        lifecycleScope.launch {
            try {
                when (uploadContext) {
                    "profile" -> uploadProfilePhoto(userId)
                    "review" -> uploadReviewPhotos(userId)
                    "provider_gallery" -> uploadProviderGalleryPhotos(userId)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@UploadImageActivity, "Error inesperado: ${e.message}", Toast.LENGTH_SHORT).show()
                    resetUploadState()
                }
            }
        }
    }

    private suspend fun uploadProfilePhoto(userId: String) {
        try {
            if (selectedImages.isEmpty()) {
                runOnUiThread {
                    Toast.makeText(this@UploadImageActivity, "No hay imagen seleccionada para el perfil.", Toast.LENGTH_SHORT).show()
                    resetUploadState()
                }
                return
            }

            val imageUri = selectedImages.first()
            val result = firebaseRepository.uploadProfilePhoto(userId, imageUri)

            result.fold(
                onSuccess = { photoUrl ->
                    // Actualizar el campo photoUrl en users
                    val updateResult = firebaseRepository.updateUserPhoto(userId, photoUrl)
                    updateResult.fold(
                        onSuccess = {
                            runOnUiThread {
                                Toast.makeText(this@UploadImageActivity, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                                navigateToNextScreen()
                            }
                        },
                        onFailure = { exception ->
                            runOnUiThread {
                                Toast.makeText(this@UploadImageActivity, "Error al actualizar perfil: ${exception.message}", Toast.LENGTH_SHORT).show()
                                resetUploadState()
                            }
                        }
                    )
                },
                onFailure = { exception ->
                    runOnUiThread {
                        Toast.makeText(this@UploadImageActivity, "Error al subir foto: ${exception.message}", Toast.LENGTH_SHORT).show()
                        resetUploadState()
                    }
                }
            )
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this@UploadImageActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                resetUploadState()
            }
        }
    }

    private suspend fun uploadReviewPhotos(userId: String) {
        try {
            if (selectedImages.isEmpty()) {
                runOnUiThread {
                    Toast.makeText(this@UploadImageActivity, "No hay imágenes seleccionadas para la reseña.", Toast.LENGTH_SHORT).show()
                    resetUploadState()
                }
                return
            }

            if (contractId.isEmpty()) {
                runOnUiThread {
                    Toast.makeText(this@UploadImageActivity, "Error: ID de contrato no proporcionado para la reseña.", Toast.LENGTH_SHORT).show()
                    resetUploadState()
                }
                return
            }

            val result = firebaseRepository.uploadReviewPhotos(contractId, selectedImages)

            result.fold(
                onSuccess = { photoUrls ->
                    uploadedImageUrls.clear()
                    uploadedImageUrls.addAll(photoUrls)
                    runOnUiThread {
                        Toast.makeText(this@UploadImageActivity, "Fotos de reseña subidas exitosamente", Toast.LENGTH_SHORT).show()
                        navigateToNextScreen()
                    }
                },
                onFailure = { exception ->
                    runOnUiThread {
                        Toast.makeText(this@UploadImageActivity, "Error al subir fotos de reseña: ${exception.message}", Toast.LENGTH_SHORT).show()
                        resetUploadState()
                    }
                }
            )
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this@UploadImageActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                resetUploadState()
            }
        }
    }

    private suspend fun uploadProviderGalleryPhotos(userId: String) {
        try {
            if (selectedImages.isEmpty()) {
                runOnUiThread {
                    Toast.makeText(this@UploadImageActivity, "No hay imágenes seleccionadas para la galería.", Toast.LENGTH_SHORT).show()
                    resetUploadState()
                }
                return
            }

            // Obtener providerId del userId
            val providerResult = firebaseRepository.getProviderIdByUserId(userId)
            providerResult.fold(
                onSuccess = { providerId ->
                    if (providerId != null) {
                        // Iniciar la subida de TODAS las imágenes a Cloudinary
                        val uploadResult = firebaseRepository.uploadGalleryPhotos(providerId, selectedImages)
                        uploadResult.fold(
                            onSuccess = { photoUrls ->
                                // Una vez que todas las imágenes están subidas, crea UN SOLO ítem de galería en Firestore
                                val galleryResult = firebaseRepository.addPhotosToProviderGallery(
                                    providerId,
                                    photoUrls, // Pasa la lista COMPLETA de URLs
                                    description,
                                    serviceType
                                )
                                galleryResult.fold(
                                    onSuccess = {
                                        uploadedImageUrls.clear()
                                        uploadedImageUrls.addAll(photoUrls) // Guarda las URLs para pasar al siguiente Intent
                                        runOnUiThread {
                                            Toast.makeText(this@UploadImageActivity, "Fotos agregadas a la galería exitosamente", Toast.LENGTH_SHORT).show()
                                            navigateToNextScreen()
                                        }
                                    },
                                    onFailure = { exception ->
                                        runOnUiThread {
                                            Toast.makeText(this@UploadImageActivity, "Error al agregar a galería en Firestore: ${exception.message}", Toast.LENGTH_SHORT).show()
                                            resetUploadState()
                                        }
                                    }
                                )
                            },
                            onFailure = { exception ->
                                runOnUiThread {
                                    Toast.makeText(this@UploadImageActivity, "Error al subir fotos a Cloudinary para la galería: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    resetUploadState()
                                }
                            }
                        )
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@UploadImageActivity, "Error: No se encontró el proveedor para subir a la galería.", Toast.LENGTH_SHORT).show()
                            resetUploadState()
                        }
                    }
                },
                onFailure = { exception ->
                    runOnUiThread {
                        Toast.makeText(this@UploadImageActivity, "Error al obtener proveedor para galería: ${exception.message}", Toast.LENGTH_SHORT).show()
                        resetUploadState()
                    }
                }
            )
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this@UploadImageActivity, "Error inesperado al procesar galería: ${e.message}", Toast.LENGTH_SHORT).show()
                resetUploadState()
            }
        }
    }

    private suspend fun uploadPhotosToGallery(providerId: String, userId: String) {
        var uploadedCount = 0
        val totalImages = selectedImages.size

        selectedImages.forEach { imageUri ->
            try {
                val fileName = "${serviceType.replace(" ", "_").lowercase()}_${System.currentTimeMillis()}_${UUID.randomUUID()}"

                // Subir imagen a Cloudinary usando el método correcto
                val uploadResult = firebaseRepository.uploadImageWithCustomName(userId, imageUri, fileName)
                uploadResult.fold(
                    onSuccess = { photoUrl ->
                        // Agregar a provider_gallery
                        val galleryResult = firebaseRepository.addPhotosToProviderGallery(
                            providerId, photoUrls= listOf(photoUrl), serviceType, description,
                        )
                        galleryResult.fold(
                            onSuccess = {
                                uploadedImageUrls.add(photoUrl)
                                uploadedCount++

                                runOnUiThread {
                                    binding.btnContinuar.text = "Subiendo... $uploadedCount/$totalImages"
                                }

                                if (uploadedCount == totalImages) {
                                    runOnUiThread {
                                        Toast.makeText(this@UploadImageActivity, "Fotos agregadas a la galería exitosamente", Toast.LENGTH_SHORT).show()
                                        navigateToNextScreen()
                                    }
                                }
                            },
                            onFailure = { exception ->
                                runOnUiThread {
                                    Toast.makeText(this@UploadImageActivity, "Error al agregar a galería: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    resetUploadState()
                                }
                                return@forEach
                            }
                        )
                    },
                    onFailure = { exception ->
                        runOnUiThread {
                            Toast.makeText(this@UploadImageActivity, "Error al subir imagen: ${exception.message}", Toast.LENGTH_SHORT).show()
                            resetUploadState()
                        }
                        return@forEach
                    }
                )
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@UploadImageActivity, "Error con imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                    resetUploadState()
                }
                return@forEach
            }
        }
    }

    private fun resetUploadState() {
        binding.progressBar.visibility = View.GONE
        binding.btnContinuar.isEnabled = true
        updateUIForContext() // Restaurar texto original del botón
    }

    private fun navigateToNextScreen() {
        // Según tu contexto, redirigir a la galería de trabajos del proveedor
        val intent = when (uploadContext) {
            "provider_gallery" -> {
                // Aquí debes crear un Intent hacia la actividad que muestra la galería del proveedor
                // Por ejemplo: Intent(this, ProviderGalleryActivity::class.java)
                Intent(this, ProviderGalleryActivity::class.java).apply {
                    putStringArrayListExtra("uploaded_image_urls", ArrayList(uploadedImageUrls))
                    putExtra("upload_context", uploadContext)
                    putExtra(EXTRA_SERVICE_TYPE, serviceType)
                    putExtra(EXTRA_DESCRIPTION, description)
                }
            }
            else -> {
                Intent(this, ProviderGalleryActivity::class.java).apply {
                    putStringArrayListExtra("uploaded_image_urls", ArrayList(uploadedImageUrls))
                    putExtra("upload_context", uploadContext)
                    if (uploadContext == "review") {
                        putExtra(EXTRA_CONTRACT_ID, contractId)
                    }
                }
            }
        }

        startActivity(intent)
        resetUploadState()
        finish() // Opcional: cerrar esta actividad
    }

    private fun showImagePickerDialog() {
        val options = if (uploadContext == "profile") {
            // Para perfil solo una imagen
            arrayOf(
                "Tomar foto con cámara",
                "Seleccionar imagen de galería",
                "Cancelar"
            )
        } else {
            // Para review y gallery múltiples imágenes
            arrayOf(
                "Tomar foto con cámara",
                "Seleccionar una imagen de galería",
                "Seleccionar múltiples imágenes",
                "Cancelar"
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Seleccionar imagen")
            .setItems(options) { dialog, which ->
                when {
                    uploadContext == "profile" -> {
                        when (which) {
                            0 -> checkCameraPermissionAndTakePhoto()
                            1 -> checkStoragePermissionAndSelectImage()
                            2 -> dialog.dismiss()
                        }
                    }
                    else -> {
                        when (which) {
                            0 -> checkCameraPermissionAndTakePhoto()
                            1 -> checkStoragePermissionAndSelectImage()
                            2 -> checkStoragePermissionAndSelectMultipleImages()
                            3 -> dialog.dismiss()
                        }
                    }
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                takePhoto()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
                showPermissionExplanationDialog(
                    "Permiso de cámara necesario",
                    "Esta aplicación necesita acceso a la cámara para tomar fotos."
                ) {
                    requestCameraPermission()
                }
            }
            else -> {
                requestCameraPermission()
            }
        }
    }

    private fun checkStoragePermissionAndSelectImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            selectImageLauncher.launch("image/*")
        } else {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                    selectImageLauncher.launch("image/*")
                }
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    showPermissionExplanationDialog(
                        "Permiso de almacenamiento necesario",
                        "Esta aplicación necesita acceso al almacenamiento para seleccionar imágenes."
                    ) {
                        requestStoragePermission()
                    }
                }
                else -> {
                    requestStoragePermission()
                }
            }
        }
    }

    private fun checkStoragePermissionAndSelectMultipleImages() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            selectMultipleImagesLauncher.launch("image/*")
        } else {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                    selectMultipleImagesLauncher.launch("image/*")
                }
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    showPermissionExplanationDialog(
                        "Permiso de almacenamiento necesario",
                        "Esta aplicación necesita acceso al almacenamiento para seleccionar múltiples imágenes."
                    ) {
                        requestStoragePermission()
                    }
                }
                else -> {
                    requestStoragePermission()
                }
            }
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    private fun requestStoragePermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(
            this,
            permissions,
            STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    private fun showPermissionExplanationDialog(title: String, message: String, onPositive: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Permitir") { _, _ -> onPositive() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto()
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
            }
            STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showImagePickerDialog()
                } else {
                    Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun takePhoto() {
        val photoFile = createImageFile()
        if (photoFile != null) {
            val photoURI = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            currentPhotoPath = photoFile.absolutePath
            takePictureLauncher.launch(photoURI)
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        } catch (ex: IOException) {
            Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun displaySelectedImages() {
        if (selectedImages.isNotEmpty()) {
            runOnUiThread {
                Toast.makeText(this, "${selectedImages.size} imagen(es) seleccionada(s)", Toast.LENGTH_SHORT).show()
                // Cambiar el ícono para mostrar que hay imágenes seleccionadas
                binding.ivAddImage.setImageResource(R.drawable.ic_image_gallery)
            }
        } else {
            binding.ivAddImage.setImageResource(R.drawable.ic_camera_small)
        }
    }

    // Métodos utilitarios
    fun getSelectedImages(): List<Uri> {
        return selectedImages.toList()
    }

    fun getUploadedImageUrls(): List<String> {
        return uploadedImageUrls.toList()
    }

    fun clearSelectedImages() {
        selectedImages.clear()
        uploadedImageUrls.clear()
        binding.ivAddImage.setImageResource(R.drawable.ic_camera_small)
    }
}
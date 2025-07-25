package com.akaes.applimpieza

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class UploadImageActivity : AppCompatActivity() {
    private lateinit var btnContinuar: Button
    private lateinit var layoutImageHolder: LinearLayout
    private lateinit var ivAddImage: ImageView

    private var currentPhotoPath: String = ""
    private val selectedImages = mutableListOf<String>()

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val STORAGE_PERMISSION_REQUEST_CODE = 101
    }

    // ActivityResultLauncher para la cámara
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoPath.isNotEmpty()) {
            // Foto tomada exitosamente
            selectedImages.add(currentPhotoPath)
            displaySelectedImages()
            Toast.makeText(this, "Foto guardada exitosamente", Toast.LENGTH_SHORT).show()
            navigateToNextScreen()
        } else {
            Toast.makeText(this, "Error al tomar la foto", Toast.LENGTH_SHORT).show()
        }
    }

    // ActivityResultLauncher para seleccionar desde galería
    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val imagePath = saveImageFromUri(it)
            if (imagePath != null) {
                selectedImages.add(imagePath)
                displaySelectedImages()
                Toast.makeText(this, "Imagen seleccionada exitosamente", Toast.LENGTH_SHORT).show()
                navigateToNextScreen()
            }
        }
    }

    // ActivityResultLauncher para múltiples imágenes de la galería
    private val selectMultipleImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris?.forEach { uri ->
            val imagePath = saveImageFromUri(uri)
            if (imagePath != null) {
                selectedImages.add(imagePath)
            }
        }
        if (uris?.isNotEmpty() == true) {
            displaySelectedImages()
            Toast.makeText(this, "${uris.size} imágenes seleccionadas", Toast.LENGTH_SHORT).show()

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_upload_image)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
        setupClickListeners()
      /*  btnContinuar = findViewById(R.id.btnContinuar)
        btnContinuar.setOnClickListener {
            Toast.makeText(this, "Imagen subida correctamente", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, DetalleActivity::class.java)
            startActivity(intent)
        }*/
    }
    private fun initViews() {
        btnContinuar = findViewById(R.id.btnContinuar)
        layoutImageHolder = findViewById(R.id.layout_image_holder)
        ivAddImage = findViewById(R.id.iv_add_image)
    }
    private fun setupClickListeners() {
        layoutImageHolder.setOnClickListener {
            showImagePickerDialog()
        }

        btnContinuar.setOnClickListener {
            if (selectedImages.isNotEmpty()) {
                // Pasar las imágenes seleccionadas a la siguiente actividad
                val intent = Intent(this, DetalleActivity::class.java).apply {
                    putStringArrayListExtra("selected_images", ArrayList(selectedImages))
                }
                startActivity(intent)
                Toast.makeText(this, "${selectedImages.size} imagen(es) guardada(s)", Toast.LENGTH_SHORT).show()
            } else{
                navigateToNextScreen()
            }
        }
    }
    // MÉTODO NUEVO: Función centralizada para navegar a la siguiente pantalla
    private fun navigateToNextScreen() {
        val intent = Intent(this, DetalleActivity::class.java).apply {
            putStringArrayListExtra("selected_images", ArrayList(selectedImages))
        }
        startActivity(intent)

        // Opcional: finalizar esta actividad para que no quede en el stack
        // finish()
    }
    private fun showImagePickerDialog() {
        val options = arrayOf(
            "Tomar foto con cámara",
            "Seleccionar una imagen de galería",
            "Seleccionar múltiples imágenes",
            "Cancelar"
        )

        AlertDialog.Builder(this)
            .setTitle("Seleccionar imagen")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermissionAndTakePhoto()
                    1 -> checkStoragePermissionAndSelectImage()
                    2 -> checkStoragePermissionAndSelectMultipleImages()
                    3 -> dialog.dismiss()
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
            // Para Android 13+ no necesitamos permiso de almacenamiento para leer
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

    private fun saveImageFromUri(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFile = File(storageDir, "IMG_${timeStamp}.jpg")

            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.close()

            imageFile.absolutePath
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun displaySelectedImages() {
        if (selectedImages.isNotEmpty()) {
            // Opcional: mostrar un contador de imágenes
            Toast.makeText(this, "${selectedImages.size} imagen(es) seleccionada(s)", Toast.LENGTH_SHORT).show()
            navigateToNextScreen()
        }
    }

    // Método para obtener las imágenes seleccionadas (para usar en otras partes de la app)
    fun getSelectedImages(): List<String> {
        return selectedImages.toList()
    }

    // Método para limpiar las imágenes seleccionadas
    fun clearSelectedImages() {
        selectedImages.clear()
        ivAddImage.setImageResource(R.drawable.ic_camera_small)
    }
}
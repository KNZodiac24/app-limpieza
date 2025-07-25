package com.akaes.applimpieza

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.widget.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SetupActivity : AppCompatActivity() {
    private lateinit var btnContinuar: Button
    // Views
    private lateinit var frameProfileImage: FrameLayout
    private lateinit var ivProfileImage: ImageView
    private lateinit var etCelular: EditText
    private lateinit var etDescripcion: EditText

    // Chips
    private lateinit var chipInstalacion: TextView
    private lateinit var chipLimpieza: TextView
    private lateinit var chipDecoracion: TextView
    private lateinit var chipReparacion: TextView

    // Variables para la imagen
    private var currentPhotoPath: String = ""
    private var imageUri: Uri? = null

    // Request codes
    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
        private const val STORAGE_PERMISSION_REQUEST = 101
    }

    // ActivityResultLaunchers
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // La imagen se guardó en currentPhotoPath
            android.util.Log.d("SetupActivity", "Foto tomada exitosamente")
            loadImageFromFile()
        }else {
            android.util.Log.d("SetupActivity", "Foto cancelada o falló")
            Toast.makeText(this, "Foto cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    bitmap?.let { originalBitmap ->
                        // Crear imagen circular ajustada (sin rotación EXIF para galería)
                        val circularBitmap = createCircularImage(originalBitmap, 200)
                        ivProfileImage.setImageBitmap(circularBitmap)
                        imageUri = uri
                        Toast.makeText(this, "¡Imagen seleccionada correctamente!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al cargar imagen de galería", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("SetupActivity", "Error al cargar de galería", e)
                }
            }
        }else {
            Toast.makeText(this, "Selección de imagen cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setup)
        initViews()
        setupClickListeners()
        setupChips()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnContinuar = findViewById(R.id.btnContinuar)
        btnContinuar.setOnClickListener {
            val intent = Intent(this, UploadImageActivity::class.java)
            startActivity(intent)
        }
    }
    private fun initViews() {
        frameProfileImage = findViewById(R.id.frameProfileImage)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        etCelular = findViewById(R.id.etCelular)
        etDescripcion = findViewById(R.id.etDescripcion)
        btnContinuar = findViewById(R.id.btnContinuar)

        // Chips
        chipInstalacion = findViewById(R.id.chipInstalacion)
        chipLimpieza = findViewById(R.id.chipLimpieza)
        chipDecoracion = findViewById(R.id.chipDecoracion)
        chipReparacion = findViewById(R.id.chipReparacion)

        // Establecer imagen por defecto
        ivProfileImage.setImageResource(R.drawable.ic_camera_small)

    }

    private fun setupClickListeners() {
        frameProfileImage.setOnClickListener {
            showImagePickerDialog()
        }

        btnContinuar.setOnClickListener {
            if (validateForm()) {
                saveUserData()
            }
        }
        // Click largo en la imagen para mostrar info de debugging
        frameProfileImage.setOnLongClickListener {
            showImageInfo()
            true
        }
    }

    private fun setupChips() {
        val chips = listOf(chipInstalacion, chipLimpieza, chipDecoracion, chipReparacion)

        chips.forEach { chip ->
            chip.setOnClickListener {
                selectChip(chip, chips)
            }
        }
    }

    private fun selectChip(selectedChip: TextView, allChips: List<TextView>) {
        allChips.forEach { chip ->
            if (chip == selectedChip) {
                chip.setBackgroundResource(R.drawable.chip_background_selected)
                chip.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.chip_background_unselected)
                chip.setTextColor(ContextCompat.getColor(this, R.color.primario))
            }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Tomar foto", "Seleccionar de galería", "Cancelar")

        AlertDialog.Builder(this)
            .setTitle("Seleccionar imagen de perfil")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> checkStoragePermission()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        } else {
            openCamera()
        }
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_REQUEST
            )
        } else {
            openGallery()
        }
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            imageUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            }

            takePictureLauncher.launch(takePictureIntent)

        } catch (ex: IOException) {
            Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir("Pictures")

        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun loadImageFromFile() {
        if (currentPhotoPath.isNotEmpty()) {
            try {
                // Cargar la imagen
                val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                bitmap?.let { originalBitmap ->
                    // Corregir orientación usando EXIF
                    val correctedBitmap = rotateImageIfRequired(originalBitmap, currentPhotoPath)

                    // Crear imagen circular ajustada
                    val circularBitmap = createCircularImage(correctedBitmap, 200) // Tamaño fijo

                    // Mostrar en ImageView
                    ivProfileImage.setImageBitmap(circularBitmap)

                    Toast.makeText(this, "¡Imagen cargada correctamente!", Toast.LENGTH_SHORT).show()
                    android.util.Log.d("SetupActivity", "Imagen cargada y procesada desde: $currentPhotoPath")
                } ?: run {
                    Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("SetupActivity", "No se pudo decodificar la imagen")
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error al procesar la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("SetupActivity", "Error al cargar imagen", e)
            }
        }
    }
    // Método para corregir la rotación de la imagen según EXIF
    private fun rotateImageIfRequired(bitmap: Bitmap, imagePath: String): Bitmap {
        try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            android.util.Log.e("SetupActivity", "Error al leer EXIF", e)
            return bitmap
        }
    }
    // Método mejorado para crear imagen circular perfectamente ajustada
    private fun createCircularImage(bitmap: Bitmap, targetSize: Int): Bitmap {
        // Crear bitmap cuadrado escalado
        val squareBitmap = createSquareBitmap(bitmap, targetSize)

        // Crear bitmap circular
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        val rect = android.graphics.Rect(0, 0, targetSize, targetSize)
        val rectF = android.graphics.RectF(rect)
        val radius = targetSize / 2f

        // Dibujar círculo
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(radius, radius, radius, paint)

        // Aplicar máscara circular
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(squareBitmap, rect, rect, paint)

        return output
    }
    // Método para rotar imagen
    private fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    // Crear bitmap cuadrado centrado y escalado
    private fun createSquareBitmap(bitmap: Bitmap, targetSize: Int): Bitmap {
        val size = Math.min(bitmap.width, bitmap.height)

        // Recortar al centro para hacer cuadrado
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        val squareBitmap = Bitmap.createBitmap(bitmap, x, y, size, size)

        // Escalar al tamaño objetivo
        return Bitmap.createScaledBitmap(squareBitmap, targetSize, targetSize, true)
    }


    private fun validateForm(): Boolean {
        val celular = etCelular.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()

        if (celular.isEmpty()) {
            etCelular.error = "El número de celular es requerido"
            return false
        }

        if (descripcion.isEmpty()) {
            etDescripcion.error = "La descripción es requerida"
            return false
        }

        if (imageUri == null && currentPhotoPath.isEmpty()) {
            Toast.makeText(this, "Por favor selecciona una imagen de perfil", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveUserData() {
        // Aquí puedes guardar los datos del usuario
        val celular = etCelular.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val especialización = getSelectedSpecialization()

        // Guardar en SharedPreferences o base de datos
        val sharedPrefs = getSharedPreferences("user_profile", MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString("celular", celular)
            putString("descripcion", descripcion)
            putString("especializacion", especialización)
            putString("profile_image_path", currentPhotoPath)
            putString("profile_image_uri", imageUri?.toString())
            apply()
        }

        Toast.makeText(this, "Perfil guardado exitosamente", Toast.LENGTH_SHORT).show()

        // Continuar a la siguiente actividad
        // startActivity(Intent(this, NextActivity::class.java))
        // finish()
    }

    private fun getSelectedSpecialization(): String {
        return when {
            chipInstalacion.currentTextColor == ContextCompat.getColor(this, android.R.color.white) -> "Instalación"
            chipLimpieza.currentTextColor == ContextCompat.getColor(this, android.R.color.white) -> "Limpieza"
            chipDecoracion.currentTextColor == ContextCompat.getColor(this, android.R.color.white) -> "Decoración"
            chipReparacion.currentTextColor == ContextCompat.getColor(this, android.R.color.white) -> "Reparación"
            else -> "Instalación" // Por defecto
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
            }
            STORAGE_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Método para recuperar la imagen guardada (para usar en otras actividades)
    fun getProfileImage(): Bitmap? {
        val sharedPrefs = getSharedPreferences("user_profile", MODE_PRIVATE)
        val imagePath = sharedPrefs.getString("profile_image_path", "")

        return if (!imagePath.isNullOrEmpty()) {
            BitmapFactory.decodeFile(imagePath)
        } else {
            val imageUriString = sharedPrefs.getString("profile_image_uri", "")
            if (!imageUriString.isNullOrEmpty()) {
                try {
                    val inputStream = contentResolver.openInputStream(Uri.parse(imageUriString))
                    BitmapFactory.decodeStream(inputStream)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }
    // Función para mostrar información de debugging
    private fun showImageInfo() {
        val info = buildString {
            appendLine("=== INFO DE IMAGEN ===")
            appendLine("Ruta: $currentPhotoPath")
            appendLine("URI: $imageUri")
            appendLine("Existe: ${if (currentPhotoPath.isNotEmpty()) File(currentPhotoPath).exists() else "N/A"}")
            appendLine("Tamaño: ${if (currentPhotoPath.isNotEmpty()) File(currentPhotoPath).length() else "N/A"} bytes")

            // Mostrar ruta del directorio de Pictures
            val picturesDir = getExternalFilesDir("Pictures")
            appendLine("Dir Pictures: ${picturesDir?.absolutePath}")
            appendLine("Dir existe: ${picturesDir?.exists()}")
        }

        AlertDialog.Builder(this)
            .setTitle("Info de Imagen")
            .setMessage(info)
            .setPositiveButton("Copiar ruta") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Ruta imagen", currentPhotoPath)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Ruta copiada al portapapeles", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }
}
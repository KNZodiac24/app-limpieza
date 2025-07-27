package com.akaes.applimpieza
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.akaes.applimpieza.databinding.ActivityRegistroBinding
import com.akaes.applimpieza.models.User
import com.akaes.applimpieza.models.Provider
import com.akaes.applimpieza.repository.FirebaseRepository
import kotlinx.coroutines.launch

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private val repository = FirebaseRepository()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configurar ViewBinding
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupListeners()
    }

    private fun setupListeners() {
        // Navegar a login
        binding.txtLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }



        // Botón de registro
        binding.btnRegistrar.setOnClickListener {
            registerProvider()
        }
    }

    private fun registerProvider() {
        val name = binding.txtNombre.text.toString().trim()
        val email = binding.txtCorreo.text.toString().trim()
        val password = binding.txtContrasena.text.toString().trim()
        val confirmPassword = binding.txtConfirmarContrasena.text.toString().trim()
        val acceptedTerms = binding.chbxCondiciones.isChecked

        // Validaciones
        if (!validateInputs(name, email, password, confirmPassword, acceptedTerms)) {
            return
        }

        // Mostrar loading
        showLoading(true)

        lifecycleScope.launch {
            try {
                // 1. Registrar en Firebase Auth
                repository.registerUser(email, password)
                    .onSuccess { userId ->
                        // 2. Crear usuario como proveedor
                        createProviderUser(userId, name, email)
                    }
                    .onFailure { exception ->
                        showLoading(false)
                        showError("Error en registro: ${getFirebaseErrorMessage(exception.message)}")
                    }

            } catch (e: Exception) {
                showLoading(false)
                showError("Error inesperado: ${e.message}")
            }
        }
    }

    private suspend fun createProviderUser(userId: String, name: String, email: String) {


        // 2. Crear documento de usuario
        val user = User(
            id = userId,
            name = name,
            email = email,
            role = "proveedor", // Directamente como proveedor
            photoUrl = ""
        )

        repository.createUser(user)
            .onSuccess {
                // 3. Crear documento de proveedor con datos básicos
                createProviderDocument(userId)
            }
            .onFailure { exception ->
                showLoading(false)
                showError("Error al crear usuario: ${exception.message}")
            }
    }

    private suspend fun createProviderDocument(userId: String) {
        // Crear proveedor con datos básicos (se completarán después en el perfil)
        val provider = Provider(
            userId = userId,
            phone = "", // Se completará después
            description = "", // Se completará después
            serviceTypes = emptyList(), // Se completará después
            completedJobs = 0,
            rating = 0.0,
            totalReviews = 0
        )

        repository.createProvider(provider)
            .onSuccess {
                showLoading(false)
                showSuccess("Proveedor registrado exitosamente")

                // Navegar al home del proveedor o a completar perfil
                navigateToProviderHome()
            }
            .onFailure { exception ->
                showLoading(false)
                showError("Error al crear perfil de proveedor: ${exception.message}")
            }
    }

    private fun validateInputs(name: String, email: String, password: String, confirmPassword: String, acceptedTerms: Boolean): Boolean {
        // Limpiar errores previos
        binding.txtNombreLayout.error = null
        binding.txtCorreoLayout.error = null
        binding.txtContrasenaLayout.error = null
        binding.txtConfirmarContrasenaLayout.error = null

        // Validar nombre
        if (name.isEmpty()) {
            binding.txtNombreLayout.error = "El nombre es requerido"
            binding.txtNombre.requestFocus()
            return false
        }

        if (name.length < 2) {
            binding.txtNombreLayout.error = "El nombre debe tener al menos 2 caracteres"
            binding.txtNombre.requestFocus()
            return false
        }

        // Validar email
        if (email.isEmpty()) {
            binding.txtCorreoLayout.error = "El email es requerido"
            binding.txtCorreo.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.txtCorreoLayout.error = "Email inválido"
            binding.txtCorreo.requestFocus()
            return false
        }

        // Validar contraseña
        if (password.isEmpty()) {
            binding.txtContrasenaLayout.error = "La contraseña es requerida"
            binding.txtContrasena.requestFocus()
            return false
        }

        if (password.length < 6) {
            binding.txtContrasenaLayout.error = "La contraseña debe tener al menos 6 caracteres"
            binding.txtContrasena.requestFocus()
            return false
        }

        // Confirmar contraseña
        if (confirmPassword.isEmpty()) {
            binding.txtConfirmarContrasenaLayout.error = "Confirma tu contraseña"
            binding.txtConfirmarContrasena.requestFocus()
            return false
        }

        if (password != confirmPassword) {
            binding.txtConfirmarContrasenaLayout.error = "Las contraseñas no coinciden"
            binding.txtConfirmarContrasena.requestFocus()
            return false
        }

        // Validar términos y condiciones
        if (!acceptedTerms) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            binding.btnRegistrar.isEnabled = false
            binding.btnRegistrar.text = "Registrando..."
            // Deshabilitar otros campos
            binding.txtNombre.isEnabled = false
            binding.txtCorreo.isEnabled = false
            binding.txtContrasena.isEnabled = false
            binding.txtConfirmarContrasena.isEnabled = false
            binding.chbxCondiciones.isEnabled = false
        } else {
            binding.btnRegistrar.isEnabled = true
            binding.btnRegistrar.text = getString(R.string.btnRegistrar)
            // Habilitar campos
            binding.txtNombre.isEnabled = true
            binding.txtCorreo.isEnabled = true
            binding.txtContrasena.isEnabled = true
            binding.txtConfirmarContrasena.isEnabled = true
            binding.chbxCondiciones.isEnabled = true
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getFirebaseErrorMessage(error: String?): String {
        return when {
            error?.contains("email address is already in use") == true ->
                "Este correo ya está registrado. Intenta con otro email."
            error?.contains("weak password") == true ->
                "La contraseña es muy débil. Usa al menos 6 caracteres."
            error?.contains("invalid email") == true ->
                "El formato del email no es válido."
            error?.contains("network error") == true ->
                "Error de conexión. Verifica tu internet."
            else -> error ?: "Error desconocido"
        }
    }

    private fun navigateToProviderHome() {
        // Navegar a completar perfil de proveedor
        val intent = Intent(this, SetupActivity::class.java)
        intent.putExtra("userId", repository.getCurrentUserId())
        startActivity(intent)
        finish()
    }
}
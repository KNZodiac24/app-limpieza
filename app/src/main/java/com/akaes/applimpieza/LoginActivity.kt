package com.akaes.applimpieza

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.akaes.applimpieza.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    companion object {
        const val EXTRA_LOGIN = "EXTRA_LOGIN"
        const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar ViewBinding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase Auth
        auth = Firebase.auth

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Evento: Olvidé mi contraseña
        binding.txtForgotPsw.setOnClickListener {
            mostrarDialogoRecuperarContrasena()
        }

        // Evento: Iniciar sesión
        binding.btnLogin.setOnClickListener {
            val email = binding.txtEmail.text.toString().trim()
            val clave = binding.txtContrasena.text.toString().trim()

            if (validarCampos(email, clave)) {
                autenticarUsuario(email, clave)
            }
        }

        // Evento: Registrarse
        binding.txtRegistrate.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validarCampos(email: String, password: String): Boolean {
        var isValid = true

        // Limpiar errores previos
        binding.txtEmail.error = null
        binding.txtContrasena.error = null

        // Validar email vacío
        if (email.isEmpty()) {
            binding.txtEmail.error = "El email es obligatorio"
            binding.txtEmail.requestFocus()
            isValid = false
        }
        // Validar formato de email
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.txtEmail.error = "Ingresa un email válido"
            binding.txtEmail.requestFocus()
            isValid = false
        }

        // Validar contraseña vacía
        if (password.isEmpty()) {
            binding.txtContrasena.error = "La contraseña es obligatoria"
            if (isValid) binding.txtContrasena.requestFocus()
            isValid = false
        }
        // Validar longitud mínima de contraseña
        else if (password.length < 6) {
            binding.txtContrasena.error = "La contraseña debe tener al menos 6 caracteres"
            if (isValid) binding.txtContrasena.requestFocus()
            isValid = false
        }

        return isValid
    }

    private fun mostrarDialogoRecuperarContrasena() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Recuperar contraseña")
        builder.setMessage("Ingresa tu email para recibir un enlace de recuperación:")

        // Crear EditText para el email
        val emailEditText = android.widget.EditText(this)
        emailEditText.hint = "tu@email.com"
        emailEditText.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        // Agregar padding al EditText
        emailEditText.setPadding(40, 20, 40, 20)
        builder.setView(emailEditText)

        builder.setPositiveButton("Enviar") { dialog, _ ->
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                enviarEmailRecuperacion(email)
            } else {
                Toast.makeText(this, "Por favor ingresa un email válido", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun enviarEmailRecuperacion(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Se ha enviado un enlace de recuperación a tu email",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.d(TAG, "Password reset email sent to: $email")
                } else {
                    val errorMessage = when (task.exception?.message) {
                        "There is no user record corresponding to this identifier. The user may have been deleted." ->
                            "No existe una cuenta con este email"
                        "The email address is badly formatted." ->
                            "El formato del email no es válido"
                        else -> "Error al enviar el email: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    Log.w(TAG, "Password reset failed", task.exception)
                }
            }
    }

    private fun autenticarUsuario(email: String, password: String) {
        // Deshabilitar el botón para evitar múltiples clics
        binding.btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // Rehabilitar el botón
                binding.btnLogin.isEnabled = true

                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()

                    // Si pasa validación de datos requeridos, ir a pantalla principal
                    val intencion = Intent(this, DetalleActivity::class.java)
                    intencion.putExtra(EXTRA_LOGIN, auth.currentUser!!.email)
                    startActivity(intencion)
                    finish() // Finalizar esta actividad para que no regrese con el botón atrás
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)

                    val errorMessage = when (task.exception?.message) {
                        "There is no user record corresponding to this identifier. The user may have been deleted." ->
                            "Este email no está registrado"
                        "The password is invalid or the user does not have a password." ->
                            "Contraseña incorrecta"
                        "The email address is badly formatted." ->
                            "Formato de email inválido"
                        "A network error (such as timeout, interrupted connection or unreachable host) has occurred." ->
                            "Error de conexión. Verifica tu internet"
                        "We have blocked all requests from this device due to unusual activity. Try again later." ->
                            "Demasiados intentos fallidos. Intenta más tarde"
                        else -> "Error de autenticación: ${task.exception?.message}"
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        // Verificar si el usuario ya está autenticado
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Si ya está logueado, ir directamente a la pantalla principal
            val intent = Intent(this, DetalleActivity::class.java)
            intent.putExtra(EXTRA_LOGIN, currentUser.email)
            startActivity(intent)
            finish()
        }
    }
}
package com.akaes.applimpieza

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth

class RegistroActivity : AppCompatActivity() {
    private lateinit var txtNombre: TextInputEditText
    private lateinit var txtEmail: TextInputEditText
    private lateinit var txtContrasena: TextInputEditText
    private lateinit var txtConfirmarContrasena: TextInputEditText
    private lateinit var chbxTerminos: CheckBox
    private lateinit var txtLogin: TextView
    private lateinit var btnRegistar: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro)

        // Inicializar variables
        txtNombre = findViewById(R.id.txtNombre)
        txtEmail = findViewById(R.id.txtCorreo)
        txtContrasena = findViewById(R.id.txtContrasena)
        txtConfirmarContrasena = findViewById(R.id.txtConfirmarContrasena)
        chbxTerminos = findViewById(R.id.chbxTerminos)
        txtLogin = findViewById(R.id.txtLogin)
        btnRegistar = findViewById(R.id.btnRegistrar)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.loading_register_message))
        progressDialog.setCancelable(false)

        // Inicializar Firebase Auth
        auth = Firebase.auth

        btnRegistar.setOnClickListener {
            val nombre = txtNombre.text.toString().trim()
            val email = txtEmail.text.toString().trim()
            val contrasena = txtContrasena.text.toString().trim()
            val confirmarContrasena = txtConfirmarContrasena.text.toString().trim()
            val terminosAceptados = chbxTerminos.isChecked

            if (!validarCampos())
                return@setOnClickListener

            progressDialog.show()

            // Registrar usuario en Firebase Auth
            registrarUsuario(nombre, email, contrasena)
        }

        txtLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validarCampos(): Boolean {
        val nombre = txtNombre.text.toString().trim()
        val email = txtEmail.text.toString().trim()
        val contrasena = txtContrasena.text.toString().trim()
        val confirmarContrasena = txtConfirmarContrasena.text.toString().trim()
        val terminosAceptados = chbxTerminos.isChecked

        if (nombre.isEmpty()) {
            txtNombre.error = getString(R.string.error_nombre_required)
            return false
        }

        if (email.isEmpty()) {
            txtEmail.error = getString(R.string.error_email_required)
            txtEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            txtEmail.error = getString(R.string.error_email_invalid)
            txtEmail.requestFocus()
            return false
        }

        if (contrasena.isEmpty()) {
            txtContrasena.error = getString(R.string.error_password_required)
            txtContrasena.requestFocus()
            return false
        }

        if (contrasena.length < MIN_PASSWORD_LENGTH) {
            txtContrasena.error = getString(R.string.error_password_min_length)
            txtContrasena.requestFocus()
            return false
        }

        if (contrasena != confirmarContrasena) {
            txtConfirmarContrasena.error = getString(R.string.error_passwords_do_not_match)
            return false
        }

        if (!terminosAceptados) {
            chbxTerminos.error = getString(R.string.error_condiciones_required)
            return false
        }

        return true
    }

    private fun registrarUsuario(nombre: String, email: String, contrasena: String) {
        auth.createUserWithEmailAndPassword(email, contrasena)
            .addOnCompleteListener(this) { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    // Registro exitoso
                    Log.d(EXTRA_SIGNUP, "signUpWithEmail:success")
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(nombre)
                        .build()
                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Log.d(UPDATE_PROFILE, "User profile updated.")
                            } else {
                                Log.d(ERROR_UPDATE_PROFILE, "User profile update failed")
                            }
                        }
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    // Error en el registro
                    Log.w(EXTRA_SIGNUP, "signUpWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        getString(R.string.registration_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
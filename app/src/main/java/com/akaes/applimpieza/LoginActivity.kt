package com.akaes.applimpieza

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {
    private lateinit var txtEmail: TextInputEditText
    private lateinit var txtContrasena: TextInputEditText
    private lateinit var txtForgotPsw: TextView
    private lateinit var txtRegistrate: TextView
    private lateinit var btnLogin: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Inicializar variables
        txtEmail = findViewById(R.id.txtNombre)
        txtContrasena = findViewById(R.id.txtContrasena)
        txtForgotPsw = findViewById(R.id.txtForgotPsw)
        txtRegistrate = findViewById(R.id.txtRegistrate)
        btnLogin = findViewById(R.id.btnLogin)
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.loading_login_message))
        progressDialog.setCancelable(false)

        // Inicializar Firebase Auth
        auth = Firebase.auth

        // Configurar accion al presionar enter en el campo de email
        txtEmail.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                txtContrasena.requestFocus()
                true
            } else {
                false
            }
        }

        // Configurar accion al presionar enter en el campo de contrasena
        txtContrasena.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnLogin.performClick()
                true
            } else {
                false
            }
        }

        // Recuperar contrasena
        txtForgotPsw.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.reset_password_title))

            val input = TextInputEditText(this)
            input.hint = getString(R.string.txtHintEmailInput)
            input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            builder.setView(input)

            builder.setPositiveButton(getString(R.string.txt_enviar)) { dialog, _ ->
                val email = input.text.toString().trim()
                enviarCorreoRecuperacion(email)
                dialog.dismiss()
            }

            builder.setNegativeButton(getString(R.string.txt_cancelar)) { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
        }

        // Iniciar sesion
        btnLogin.setOnClickListener {
            val email = txtEmail.text.toString().trim()
            val password = txtContrasena.text.toString()

            if (!validarCampos())
                return@setOnClickListener

            progressDialog.show()

            // Iniciar sesión con Firebase Auth
            autenticarUsuario(email, password)
        }

        // Ir a pantalla de Registro
        txtRegistrate.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validarCampos(): Boolean {
        val email = txtEmail.text.toString().trim()
        val password = txtContrasena.text.toString()

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

        if (password.isEmpty()) {
            txtContrasena.error = getString(R.string.error_password_required)
            txtContrasena.requestFocus()
            return false
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            txtContrasena.error = getString(R.string.error_password_min_length)
            txtContrasena.requestFocus()
            return false
        }

        return true
    }

    private fun autenticarUsuario(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    // Inicio de sesión exitoso
                    Log.d(EXTRA_LOGIN, "signInWithEmail:success")
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra(EXTRA_LOGIN, auth.currentUser!!.email)
                    startActivity(intent)
                } else {
                    // Si el inicio de sesión falla, muestra un mensaje al usuario.
                    Log.w(EXTRA_LOGIN, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        getString(R.string.login_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun enviarCorreoRecuperacion(email: String) {
        if (email.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_email_required), Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.error_email_invalid), Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.setMessage(getString(R.string.sending_reset_email))
        progressDialog.show()

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "correo enviado",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "getString(R.string.reset_email_failed)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
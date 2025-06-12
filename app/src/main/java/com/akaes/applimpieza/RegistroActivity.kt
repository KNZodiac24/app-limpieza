package com.akaes.applimpieza

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.CheckBox
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText


class RegistroActivity : AppCompatActivity() {
    private lateinit var nombreInput: TextInputEditText
    private lateinit var correoInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmarContrasenaInput: TextInputEditText
    private lateinit var chbxCondiciones: CheckBox
    private lateinit var btnRegistrar: Button
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }

        nombreInput = findViewById(R.id.txtNombre)
        correoInput = findViewById(R.id.txtCorreo)
        passwordInput = findViewById(R.id.txtContrasena)
        confirmarContrasenaInput = findViewById(R.id.txtConfirmarContrasena)
        chbxCondiciones = findViewById(R.id.chbxCondiciones)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        btnRegistrar.setOnClickListener {
            validarCampos()
        }
    }

    private fun validarCampos() {
        val nombre = nombreInput.text.toString().trim()
        val correo = correoInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirmarPassword = confirmarContrasenaInput.text.toString()

        when {

            nombre.isEmpty() -> {
                nombreInput.error = getString(R.string.error_nombre_required)
                nombreInput.requestFocus()
            }

            correo.isEmpty() -> {
                correoInput.error = getString(R.string.error_email_required)
                correoInput.requestFocus()
            }

            password.isEmpty() -> {
                passwordInput.error = getString(R.string.error_password_required)
                passwordInput.requestFocus()
            }

            confirmarPassword.isEmpty() -> {
                confirmarContrasenaInput.error =
                    getString(R.string.error_confirmar_password_required)
                confirmarContrasenaInput.requestFocus()
            }

            !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> {
                correoInput.error = getString(R.string.error_email_invalid)
                correoInput.requestFocus()
            }

            password != confirmarPassword -> {
                confirmarContrasenaInput.error = getString(R.string.error_passwords_do_not_match)
                confirmarContrasenaInput.requestFocus()
            }

            !chbxCondiciones.isChecked -> {
                chbxCondiciones.error = getString(R.string.error_condiciones_required)
                chbxCondiciones.requestFocus()
            }

            else -> {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}
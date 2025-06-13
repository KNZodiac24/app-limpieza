package com.akaes.applimpieza

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class UploadImageActivity : AppCompatActivity() {
    private lateinit var btnContinuar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_upload_image)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnContinuar = findViewById(R.id.btnContinuar)
        btnContinuar.setOnClickListener {
            Toast.makeText(this, "Imagen subida correctamente", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, DetalleActivity::class.java)
            startActivity(intent)
        }
    }
}
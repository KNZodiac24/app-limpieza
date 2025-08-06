package com.akaes.applimpieza

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ProfileActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var btnContactar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // Inicializar variables
        toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnContactar = findViewById(R.id.btnContactar)

        btnContactar.setOnClickListener {
            var intent = Intent(this, ChatActivity::class.java)
            // intent.putExtras() // Pasarle el currentUserId y el id de la otra persona con la que chatea
            startActivity(intent)
        }
    }
}
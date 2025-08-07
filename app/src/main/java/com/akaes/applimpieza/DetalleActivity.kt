package com.akaes.applimpieza

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.akaes.applimpieza.databinding.ActivityDetalleBinding
import com.akaes.applimpieza.databinding.ActivitySetupBinding
import com.akaes.applimpieza.repository.FirebaseRepository
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class DetalleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetalleBinding
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var buttonActualizarPerfil: Button
    private lateinit var buttonIrGaleria: Button
    private var userId: String = ""
    private val repository = FirebaseRepository()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tabLayout = binding.tabLayout
        viewPager2 = binding.viewPager2
        buttonActualizarPerfil = binding.btnActualizarPerfil
        buttonIrGaleria = binding.btnGaleriaTrabajos
        val adapter = ViewPagerAdapter(this)
        viewPager2.adapter = adapter
// ✨ Inicializar Cloudinary ANTES de todo
        Log.d("", "DEBUG: 🚀 Inicializando Cloudinary en SetupActivity...")
        repository.initializeCloudinary(this)

        // Obtener userId del intent
        userId = intent.getStringExtra("userId") ?: repository.getCurrentUserId() ?: ""

        if (userId.isEmpty()) {
            showError("Error: Usuario no encontrado")
            finish()
            return
        }



        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.tabItPendientes)
                }

                1 -> {
                    tab.text = getString(R.string.tabItEnProceso)
                }

                2 -> {
                    tab.text = getString(R.string.tabItCompletados)
                }
            }
        }.attach()
        buttonActualizarPerfil.setOnClickListener {
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
        }
        buttonIrGaleria.setOnClickListener {
            val intent = Intent(this, ProviderGalleryActivity::class.java)
            startActivity(intent)
        }

    }
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    private fun loadUserInfo() {
        lifecycleScope.launch {
            repository.getUser(userId)
                .onSuccess { user ->
                    user?.let {
                        binding.txtBienvenido.text = "Bienvenido, ${it.name}"
                    }
                }
                .onFailure { exception ->
                    showError("Error al cargar información del usuario: ${exception.message}")
                }
        }
    }
}
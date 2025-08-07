package com.akaes.applimpieza

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.akaes.applimpieza.models.Provider
import com.bumptech.glide.Glide

class ProviderAdapter(private val providers: List<Provider>) : RecyclerView.Adapter<ProviderAdapter.ProviderViewHolder>() {

    // Mapas para nombre y foto por userId
    val userNameMap = mutableMapOf<String, String>()
    val photoUrlMap = mutableMapOf<String, String>()

    inner class ProviderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombre: TextView = itemView.findViewById(R.id.txtNombreProveedor)
        val trabajos: TextView = itemView.findViewById(R.id.txtResumen)
        val imagen: ImageView = itemView.findViewById(R.id.imgProveedor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProviderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_proveedor, parent, false)
        return ProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProviderViewHolder, position: Int) {
        val provider = providers[position]

        // Si no hay nombre en el mapa, mostramos un placeholder
        val displayName = userNameMap[provider.userId] ?: "Proveedor"
        holder.nombre.text = displayName

        holder.trabajos.text = "${provider.completedJobs} trabajos completados • ${provider.rating}"
        val url = photoUrlMap[provider.userId].orEmpty()
        Glide.with(holder.itemView)
            .load(url.ifEmpty { R.drawable.ic_launcher_foreground })
            .circleCrop()
            .into(holder.imagen)
    }

    override fun getItemCount(): Int = providers.size
}

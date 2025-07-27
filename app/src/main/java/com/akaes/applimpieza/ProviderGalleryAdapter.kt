package com.akaes.applimpieza

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.akaes.applimpieza.R
import com.akaes.applimpieza.repository.FirebaseRepository
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.*

class ProviderGalleryAdapter(
    private var galleryItems: List<FirebaseRepository.ProviderGalleryItem>
) : RecyclerView.Adapter<ProviderGalleryAdapter.GalleryViewHolder>() {

    fun updateData(newItems: List<FirebaseRepository.ProviderGalleryItem>) {
        galleryItems = newItems
        notifyDataSetChanged()
    }

    class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val viewPagerPhotos: ViewPager2 = itemView.findViewById(R.id.viewPagerPhotos)
        val tabLayoutPhotoIndicator: TabLayout = itemView.findViewById(R.id.tabLayoutPhotoIndicator)
        val tvServiceType: TextView = itemView.findViewById(R.id.tvServiceType)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvCreatedAt: TextView = itemView.findViewById(R.id.tvCreatedAt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_provider_gallery, parent, false)
        return GalleryViewHolder(view)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val item = galleryItems[position]

        holder.tvServiceType.text = "Tipo de Servicio: ${item.serviceType}"
        holder.tvDescription.text = "Descripción: ${item.description}"

        // Formatear la fecha
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.tvCreatedAt.text = "Fecha: ${dateFormat.format(item.createdAt.toDate())}"

        // Configurar el ViewPager2 para las fotos
        if (item.photoUrls.isNotEmpty()) {
            val photoAdapter = ProviderGalleryPhotoAdapter(item.photoUrls)
            holder.viewPagerPhotos.adapter = photoAdapter
            holder.viewPagerPhotos.visibility = View.VISIBLE

            // Configurar los indicadores del TabLayout
            if (item.photoUrls.size > 1) { // Mostrar indicadores solo si hay más de una foto
                holder.tabLayoutPhotoIndicator.visibility = View.VISIBLE
                TabLayoutMediator(holder.tabLayoutPhotoIndicator, holder.viewPagerPhotos) { tab, position ->
                    // No es necesario establecer texto o icono para los indicadores de puntos
                }.attach()
            } else {
                holder.tabLayoutPhotoIndicator.visibility = View.GONE
            }

        } else {
            holder.viewPagerPhotos.visibility = View.GONE
            holder.tabLayoutPhotoIndicator.visibility = View.GONE
            // Opcional: Mostrar un placeholder si no hay fotos
        }
    }

    override fun getItemCount(): Int = galleryItems.size
}
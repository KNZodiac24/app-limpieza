package com.akaes.applimpieza

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.akaes.applimpieza.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class ProviderGalleryPhotoAdapter(private val photoUrls: List<String>) :
    RecyclerView.Adapter<ProviderGalleryPhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageViewGalleryPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoUrl = photoUrls[position]
        Glide.with(holder.itemView.context)
            .load(photoUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL) // Cachear la imagen
            .placeholder(R.drawable.ic_image_placeholder) // Opcional: imagen de carga
            .error(R.drawable.ic_image_error) // Opcional: imagen de error
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = photoUrls.size
}
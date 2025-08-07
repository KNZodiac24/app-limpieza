package com.akaes.applimpieza

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.akaes.applimpieza.models.ProviderGallery
import com.bumptech.glide.Glide

class GalleryAdapter(private val items: List<String>) :
    RecyclerView.Adapter<GalleryAdapter.GalleryVH>() {

    inner class GalleryVH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgGallery)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        GalleryVH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gallery, parent, false)
        )

    override fun onBindViewHolder(holder: GalleryVH, position: Int) {
        val url = items[position]
        Glide.with(holder.img.context)
            .load(url)
            .centerCrop()
            .into(holder.img)
    }

    override fun getItemCount() = items.size
}

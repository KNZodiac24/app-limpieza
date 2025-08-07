package com.akaes.applimpieza

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akaes.applimpieza.models.ProviderGallery
import com.akaes.applimpieza.models.Review
import java.text.DateFormat

class ReviewAdapter(private val reviews: List<Review>) :
    RecyclerView.Adapter<ReviewAdapter.ReviewVH>() {

    inner class ReviewVH(v: View) : RecyclerView.ViewHolder(v) {
        val rating: TextView = v.findViewById(R.id.txtReviewRating)
        val date: TextView = v.findViewById(R.id.txtReviewDate)
        val comment: TextView = v.findViewById(R.id.txtReviewComment)
        val photosRecycler: RecyclerView = v.findViewById(R.id.recyclerReviewPhotos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ReviewVH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_review, parent, false)
        )

    override fun onBindViewHolder(holder: ReviewVH, position: Int) {
        val rev = reviews[position]
        holder.rating.text = "${rev.rating} ⭐"
        holder.date.text = DateFormat.getDateInstance().format(rev.createdAt.toDate())
        holder.comment.text = rev.comment

        // Fotos de la reseña
        holder.photosRecycler.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//            adapter = GalleryAdapter(
//                rev.photos.map { it.photoUrl } // Asumiendo que cada foto es un URL
//            )
        }
    }

    override fun getItemCount() = reviews.size
}

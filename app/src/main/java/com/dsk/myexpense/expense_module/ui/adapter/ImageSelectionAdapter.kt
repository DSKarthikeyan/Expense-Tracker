package com.dsk.myexpense.expense_module.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.util.Utility

class ImageSelectionAdapter(
    private val images: List<Int>,
    private val onImageClick: (Int) -> Unit
) : RecyclerView.Adapter<ImageSelectionAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageViewItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_selection, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageResId = images[position]

        Utility.loadImageIntoView(holder.imageView, imageResId, holder.itemView.context, isCircular = true)

        // Add a label or distinctive appearance for the "Select from phone storage" option
        if (imageResId == R.drawable.ic_add_photo) {
            holder.imageView.alpha = 0.8f // Make it slightly transparent
            holder.imageView.setOnClickListener { onImageClick(imageResId) }
        } else {
            holder.imageView.setOnClickListener { onImageClick(imageResId) }
        }
    }

    override fun getItemCount(): Int = images.size
}


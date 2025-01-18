package com.dsk.myexpense.expense_module.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.data.model.ProfileOption

class ProfileOptionAdapter(
    private val options: List<ProfileOption>,
    private val onClick: (ProfileOption) -> Unit
) : RecyclerView.Adapter<ProfileOptionAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(option: ProfileOption) {
            itemView.findViewById<ImageView>(R.id.ivOptionIcon).setImageResource(option.iconResId)
            itemView.findViewById<TextView>(R.id.tvOptionTitle).text = option.title

            itemView.setOnClickListener { onClick(option) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(options[position])
    }

    override fun getItemCount(): Int = options.size
}


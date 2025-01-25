package com.dsk.myexpense.expense_module.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.data.model.Category

class CategorySpinnerAdapter(
    private val context: Context,
    private val categories: List<Category>
) : BaseAdapter() {

    override fun getCount(): Int = categories.size

    override fun getItem(position: Int): Category = categories[position]

    override fun getItemId(position: Int): Long = categories[position].id.toLong()

    fun getItemPosition(categoryId: Int): Int {
        return categories.indexOfFirst { it.id == categoryId }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return createView(position, convertView, parent, R.layout.spinner_item)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return createView(position, convertView, parent, R.layout.spinner_dropdown_item_view)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup?, layoutId: Int): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(layoutId, parent, false)
        val category = getItem(position)

        val iconImageView = view.findViewById<ImageView>(R.id.spinner_icon)
        val nameTextView = view.findViewById<TextView>(R.id.spinner_text)

        // Set the icon if it's not null
        if (category.iconResId != 0) { // Check for a valid resource ID
            iconImageView.setImageResource(category.iconResId)
        } else {
            iconImageView.setImageResource(R.drawable.ic_other_income) // Optional: Fallback icon
        }

        nameTextView.text = category.name

        return view
    }
}

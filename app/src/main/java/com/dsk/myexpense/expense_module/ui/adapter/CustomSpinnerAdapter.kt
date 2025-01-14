package com.dsk.myexpense.expense_module.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.dsk.myexpense.R

class CustomSpinnerAdapter(
    context: Context,
    private val items: List<String>,
    private val placeholderLayout: Int,
    private val dropdownLayout: Int
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    // Return the number of items in the list
    override fun getCount(): Int {
        return items.size
    }

    // Return the item at the given position
    override fun getItem(position: Int): Any {
        return items[position]
    }

    // Return the item ID (you can return position or other unique IDs if needed)
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    // Provide the view for each item in the spinner (collapsed view)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(placeholderLayout, parent, false)

        // Ensure that the IDs in findViewById() are correct
        val textView: TextView = view.findViewById(R.id.textViewSpinner)
        val imageView: ImageView = view.findViewById(R.id.dropdownArrow)

        // Set the text for the TextView in the collapsed spinner
        textView.text = items[position]

        // Optionally handle the dropdown icon (you can set the image here if needed)
        imageView.setImageResource(R.drawable.ic_arrow_down_24)

        return view
    }

    // Provide the view for each item in the dropdown (expanded view)
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(dropdownLayout, parent, false)

        // Set the text for the dropdown item (only TextView)
        val textView: TextView = view.findViewById(android.R.id.text1)
        textView.text = items[position]

        return view
    }
}

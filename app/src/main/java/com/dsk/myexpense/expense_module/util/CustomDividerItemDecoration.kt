package com.dsk.myexpense.expense_module.util

import android.content.Context
import androidx.annotation.AttrRes
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.recyclerview.widget.RecyclerView
import com.dsk.myexpense.R

class CustomDividerItemDecoration(
    private val context: Context,
    private val height: Int, // Divider height in pixels
) : RecyclerView.ItemDecoration() {

    private val paint = Paint()

    init {
        val colorRes =  resolveAttribute(R.attr.colorOnSurface)
        paint.color = colorRes
    }

    private fun resolveAttribute(@AttrRes attr: Int): Int {
        val typedValue = context.obtainStyledAttributes(intArrayOf(attr))
        val color = typedValue.getColor(0, 0)
        typedValue.recycle()
        return color
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        for (i in 0 until parent.childCount - 1) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + height

            c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: android.view.View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.set(0, 0, 0, height)
    }
}

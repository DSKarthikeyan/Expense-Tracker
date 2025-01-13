package com.dsk.myexpense.expense_module.util

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.dsk.myexpense.expense_module.ui.adapter.CardAdapter

class CustomTouchHelperCallback(private val adapter: CardAdapter) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
            return makeMovementFlags(dragFlags, swipeFlags)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            // Handle item move
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            // Perform your move logic, like reordering the items in your adapter
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // Handle swipe
            val position = viewHolder.adapterPosition
            // Perform your swipe logic (remove item, etc.)
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            // Customize UI for selected item if needed (like showing a shadow or changing the background)
        }

        override fun isLongPressDragEnabled(): Boolean {
            return true  // Enable drag on long press if desired
        }
    }

    fun setupRecyclerView(recyclerView: RecyclerView, adapter: CardAdapter) {
        val itemTouchHelper = ItemTouchHelper(CustomTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }


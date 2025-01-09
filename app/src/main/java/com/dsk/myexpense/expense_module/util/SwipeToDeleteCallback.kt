package com.dsk.myexpense.expense_module.util

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.ui.viewmodel.HomeDetailsViewModel
import com.dsk.myexpense.expense_module.ui.adapter.MyItemRecyclerViewAdapter

class SwipeToDeleteCallback(
    private val recyclerView: RecyclerView,
    private val viewModel: HomeDetailsViewModel,
    private val onSwipe: (ExpenseDetails) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        val adapter = recyclerView.adapter as? MyItemRecyclerViewAdapter
        if (adapter != null) {
            val expenseDetails = adapter.getExpenseDetailsAt(position)
            viewModel.deleteExpenseDetails(expenseDetails)
            onSwipe(expenseDetails)
        }
    }
}

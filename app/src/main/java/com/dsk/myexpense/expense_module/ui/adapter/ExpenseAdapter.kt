package com.dsk.myexpense.expense_module.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dsk.myexpense.databinding.ItemExpenseBinding
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseAdapter : ListAdapter<ExpenseDetails, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = getItem(position)
        holder.bind(expense)
    }

    inner class ExpenseViewHolder(private val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: ExpenseDetails) {
            binding.senderName.text = expense.expenseSenderName
            binding.receiverName.text = expense.expenseReceiverName
            binding.amount.text = expense.amount.toString()
            binding.description.text = expense.expenseDescription
            binding.date.text = formatDate(expense.expenseAddedDate)

        }
    }

    private fun formatDate(dateInMillis: Long): String {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return formatter.format(Date(dateInMillis))
    }
}

class ExpenseDiffCallback : DiffUtil.ItemCallback<ExpenseDetails>() {
    override fun areItemsTheSame(oldItem: ExpenseDetails, newItem: ExpenseDetails): Boolean =
        oldItem.expenseID == newItem.expenseID

    override fun areContentsTheSame(oldItem: ExpenseDetails, newItem: ExpenseDetails): Boolean =
        oldItem == newItem
}

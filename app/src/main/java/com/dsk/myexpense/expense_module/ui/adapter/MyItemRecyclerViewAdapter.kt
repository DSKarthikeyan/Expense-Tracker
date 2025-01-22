package com.dsk.myexpense.expense_module.ui.adapter

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.ItemGroupListDetailBinding
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.util.Utility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyItemRecyclerViewAdapter(
    private val appLoadingViewModel: AppLoadingViewModel,
    private val expenseDetailsClickListener: ExpenseDetailClickListener
) : RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder>() {

    private var expenseDetails = mutableListOf<ExpenseDetails>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemGroupListDetailBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = expenseDetails[position]
        holder.expenseDetailsName.text = item.expenseSenderName
        val currencySymbol = holder.expenseAmountDetail.context.getString(
            R.string.text_amount_value,
            appLoadingViewModel.getCurrencySymbol(holder.expenseAmountDetail.context),
            item.amount
        )
        val (formattedAmount, color) = if (item.isIncome) {
            "+ $currencySymbol " to R.color.teal_700
        } else {
            "- $currencySymbol " to R.color.red
        }

        holder.expenseAmountDetail.text = formattedAmount
        holder.expenseAmountDetail.setTextColor(
            holder.expenseAmountDetail.resources.getColor(color, null)
        )

        Utility.updateDate(holder.expenseAmountDate, item.expenseAddedDate)

        CoroutineScope(Dispatchers.IO).launch {
            val categoryImage = item.categoryId?.let { appLoadingViewModel.getCategoryNameByID(it) }
            withContext(Dispatchers.Main) {
                val iconResId = categoryImage?.iconResId ?: R.drawable.ic_other_expenses
                holder.expenseCategoryImageView.setImageResource(iconResId)
                holder.expenseCategoryImageView.clipToOutline = true
            }
        }

        holder.itemView.setOnClickListener {
            if (position in expenseDetails.indices) {
                expenseDetailsClickListener.onItemClicked(expenseDetails[position])
            } else {
                Log.e("RecyclerViewAdapter", "Index out of bounds: $position")
            }
        }
    }

    fun updateList(newList: List<ExpenseDetails>) {
        val diffCallback = ExpenseDetailsDiffCallback(expenseDetails, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        expenseDetails.clear()
        expenseDetails.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun getExpenseDetailsAt(position: Int): ExpenseDetails = expenseDetails[position]

    override fun getItemCount(): Int = expenseDetails.size

    fun <T : Comparable<T>> sortBy(selector: (ExpenseDetails) -> T) {
        expenseDetails = expenseDetails.sortedWith(compareBy(selector)).toMutableList()
        notifyDataSetChanged()
    }

    fun sortByAmount(selector: (ExpenseDetails) -> Double) {
        expenseDetails = expenseDetails.sortedWith(compareBy(selector)).toMutableList()
        notifyDataSetChanged()
    }

    inner class ViewHolder(binding: ItemGroupListDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val expenseDetailsName: TextView = binding.transactionName
        val expenseAmountDetail: TextView = binding.transactionAmount
        val expenseAmountDate: TextView = binding.transactionDate
        val expenseCategoryImageView: ImageView = binding.expenseCategoryImageView

        init {
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    expenseDetailsClickListener.onItemLongClicked(expenseDetails[position])
                }
                true
            }
        }
    }

    interface ExpenseDetailClickListener {
        fun onItemClicked(expenseDetails: ExpenseDetails)
        fun onItemLongClicked(expenseDetails: ExpenseDetails)
    }
}

class ExpenseDetailsDiffCallback(
    private val oldList: List<ExpenseDetails>, private val newList: List<ExpenseDetails>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].expenseID == newList[newItemPosition].expenseID
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}

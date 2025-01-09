package com.dsk.myexpense.expense_module.ui.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import com.dsk.myexpense.expense_module.util.AppConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MyItemRecyclerViewAdapter(
    private val context: Context,
    private val appLoadingViewModel: AppLoadingViewModel,
    private val expenseDetailsClickListener: ExpenseDetailClickListener
) : RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder>() {

    private var expenseDetails = ArrayList<ExpenseDetails>()

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
        holder.expenseAmountDetail.text = item.amount.toString()

        // Update date dynamically
        updateDate(holder.expenseAmountDate, item.expenseAddedDate)

        // Coroutine to load category image
        CoroutineScope(Dispatchers.IO).launch {
            val categoryImage = item.categoryId?.let { appLoadingViewModel.getCategoryNameByID(it) }
            withContext(Dispatchers.Main) {
                val iconResId = categoryImage?.iconResId ?: R.drawable.ic_other_expenses
                holder.expenseCategoryImageView.setImageResource(iconResId)
                holder.expenseCategoryImageView.clipToOutline = true
            }
        }

        holder.itemView.setOnClickListener {
            if (expenseDetails != null && position in expenseDetails.indices && position < expenseDetails.size) {
                expenseDetailsClickListener.onItemClicked(expenseDetails[position])
            } else {
                Log.e("RecyclerViewAdapter", "Index out of bounds: $position")
            }
        }

    }

    private fun updateDate(textView: TextView, timestamp: Long) {
        val currentTime = System.currentTimeMillis()
        val differenceInMillis = currentTime - timestamp
        val oneMinute = 60 * 1000
        val oneHour = 60 * oneMinute
        val oneDay = 24 * oneHour

        val formattedTime: String = when {
            // Future timestamps
            differenceInMillis < 0 -> {
                val futureDifference = -differenceInMillis
                when {
                    futureDifference < oneMinute -> "In ${futureDifference / 1000} second${if (futureDifference / 1000 == 1L) "" else "s"}"
                    futureDifference < oneHour -> "In ${futureDifference / oneMinute} minute${if (futureDifference / oneMinute == 1L) "" else "s"}"
                    futureDifference < oneDay -> "In ${futureDifference / oneHour} hour${if (futureDifference / oneHour == 1L) "" else "s"}"
                    else -> "In the future (${SimpleDateFormat(AppConstants.DATE_FORMAT_STRING, Locale.getDefault()).format(Date(timestamp))})"
                }
            }
            // Past timestamps
            differenceInMillis < oneMinute -> "${differenceInMillis / 1000} second${if (differenceInMillis / 1000 == 1L) "" else "s"} ago"
            differenceInMillis < oneHour -> "${differenceInMillis / oneMinute} minute${if (differenceInMillis / oneMinute == 1L) "" else "s"} ago"
            differenceInMillis < oneDay -> "${differenceInMillis / oneHour} hour${if (differenceInMillis / oneHour == 1L) "" else "s"} ago"
            isToday(timestamp) -> "Today"
            isYesterday(timestamp) -> "Yesterday"
            else -> SimpleDateFormat(AppConstants.DATE_FORMAT_STRING, Locale.getDefault()).format(Date(timestamp))
        }

        textView.text = formattedTime

        // Schedule updates for "seconds ago", "minutes ago", and "hours ago"
        if (differenceInMillis < oneDay && differenceInMillis >= 0) {
            val delay = when {
                differenceInMillis < oneMinute -> 1000L // Update every second
                differenceInMillis < oneHour -> oneMinute.toLong() // Update every minute
                else -> oneHour.toLong() // Update every hour
            }
            textView.postDelayed({
                updateDate(textView, timestamp)
            }, delay)
        }
    }


    private fun isToday(timestamp: Long): Boolean {
        val currentCalendar = Calendar.getInstance()
        val eventCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }

        return currentCalendar.get(Calendar.YEAR) == eventCalendar.get(Calendar.YEAR) &&
                currentCalendar.get(Calendar.DAY_OF_YEAR) == eventCalendar.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(timestamp: Long): Boolean {
        val currentCalendar = Calendar.getInstance()
        val eventCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }

        return currentCalendar.get(Calendar.YEAR) == eventCalendar.get(Calendar.YEAR) &&
                currentCalendar.get(Calendar.DAY_OF_YEAR) - eventCalendar.get(Calendar.DAY_OF_YEAR) == 1
    }

    fun updateList(newList: List<ExpenseDetails>) {
        val diffCallback = ExpenseDetailsDiffCallback(expenseDetails, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        expenseDetails.clear()
        expenseDetails.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun getExpenseDetailsAt(position: Int): ExpenseDetails {
        return expenseDetails[position]
    }

    override fun getItemCount(): Int = expenseDetails.size

    fun <T : Comparable<T>> sortBy(selector: (ExpenseDetails) -> T) {
        expenseDetails = ArrayList(expenseDetails.sortedWith(compareBy(selector)))
        notifyDataSetChanged()
    }

    fun sortByAmount(selector: (ExpenseDetails) -> Double) {
        expenseDetails =  ArrayList(expenseDetails.sortedWith(compareBy(selector)))
        notifyDataSetChanged()
    }

    inner class ViewHolder(binding: ItemGroupListDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val expenseDetailsName: TextView = binding.transactionName
        val expenseAmountDetail: TextView = binding.transactionAmount
        val expenseAmountDate: TextView = binding.transactionDate
        val expenseCategoryImageView: ImageView = binding.expenseCategoryImageView

        override fun toString(): String {
            return super.toString() + " '" + expenseDetails.size + "'"
        }

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
    private val oldList: List<ExpenseDetails>,
    private val newList: List<ExpenseDetails>
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

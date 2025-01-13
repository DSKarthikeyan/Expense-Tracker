package com.dsk.myexpense.expense_module.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dsk.myexpense.databinding.ItemAccountBinding
import com.dsk.myexpense.expense_module.data.model.AccountEntity

class AccountAdapter(
    private val accounts: List<AccountEntity>,
    private val onAccountStatusChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    inner class AccountViewHolder(private val binding: ItemAccountBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(account: AccountEntity) {
            binding.accountName.text = account.accountType
            binding.accountToggle.isChecked = account.isConnected

            binding.accountToggle.setOnCheckedChangeListener { _, isChecked ->
                onAccountStatusChanged(account.accountType, isChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(accounts[position])
    }

    override fun getItemCount(): Int = accounts.size
}
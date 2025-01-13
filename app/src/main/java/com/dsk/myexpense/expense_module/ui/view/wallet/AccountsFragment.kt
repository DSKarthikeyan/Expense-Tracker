package com.dsk.myexpense.expense_module.ui.view.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.dsk.myexpense.databinding.FragmentWalletAccountsBinding
import com.dsk.myexpense.expense_module.ui.adapter.AccountAdapter
import com.dsk.myexpense.expense_module.ui.viewmodel.WalletViewModel

class AccountsFragment : Fragment() {

    private var _binding: FragmentWalletAccountsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WalletViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletAccountsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.accounts.observe(viewLifecycleOwner) { accounts ->
            binding.accountList.adapter = AccountAdapter(accounts) { accountType, isConnected ->
                viewModel.updateAccountStatus(accountType, isConnected)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
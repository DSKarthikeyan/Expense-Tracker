package com.dsk.myexpense.expense_module.ui.view.accountdetails

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.FragmentSettingsAccountDetailsBinding
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.data.model.ProfileOption
import com.dsk.myexpense.expense_module.ui.adapter.ProfileOptionAdapter
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.ui.viewmodel.HomeDetailsViewModel
import com.dsk.myexpense.expense_module.util.AppConstants
import com.dsk.myexpense.expense_module.util.CommonDialog
import com.dsk.myexpense.expense_module.util.Utility
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarView
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarViewModel
import kotlinx.coroutines.launch

class AccountsDetailsFragment : Fragment() {

    private var fragmentSettingsAccountDetailsBinding: FragmentSettingsAccountDetailsBinding? = null
    private val binding get() = fragmentSettingsAccountDetailsBinding!!
    private lateinit var adapter: ProfileOptionAdapter
    private val homeDetailsViewModel: HomeDetailsViewModel by viewModels {
        GenericViewModelFactory {
            HomeDetailsViewModel(
                requireContext(),
                ExpenseApplication.getExpenseRepository(requireContext()),
                ExpenseApplication.getSettingsRepository(requireContext())
            )
        }
    }
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                // Persist permission for future access
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d("DsK", "Account image load error ${e.localizedMessage}")
                }

                // Save or use the URI
                selectedImageUri = it
                fragmentSettingsAccountDetailsBinding?.ivProfile?.let {
                    Utility.loadImageIntoView(it, selectedImageUri!!, requireContext(), isCircular = true)
                }
            }
        }

    private lateinit var headerBarViewModel: HeaderBarViewModel
    private lateinit var headerBarView: HeaderBarView
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentSettingsAccountDetailsBinding =
            FragmentSettingsAccountDetailsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        prepareHeaderBarData()
    }

    private fun prepareHeaderBarData() {
        headerBarViewModel = ViewModelProvider(this)[HeaderBarViewModel::class.java]
        headerBarView = binding.headerBarLayout

        // Bind ViewModel LiveData to the HeaderBarView
        headerBarViewModel.headerTitle.observe(viewLifecycleOwner, { title ->
            headerBarView.setHeaderTitle(title)
        })

        headerBarViewModel.leftIconResource.observe(viewLifecycleOwner, { iconResId ->
            headerBarView.setLeftIcon(iconResId)
        })

        headerBarViewModel.rightIconResource.observe(viewLifecycleOwner, { iconResId ->
            headerBarView.setRightIcon(iconResId)
        })

        headerBarViewModel.isLeftIconVisible.observe(viewLifecycleOwner, { isVisible ->
            headerBarView.setLeftIconVisibility(isVisible)
        })

        headerBarViewModel.isRightIconVisible.observe(viewLifecycleOwner, { isVisible ->
            headerBarView.setRightIconVisibility(isVisible)
        })

        // Example: Updating the header dynamically
        headerBarViewModel.setHeaderTitle(getString(R.string.text_profile_details))
        headerBarViewModel.setLeftIconResource(R.drawable.ic_arrow_left_24)
        headerBarViewModel.setRightIconResource(R.drawable.ic_notification_unfilled)
        headerBarViewModel.setLeftIconVisibility(true)
        headerBarViewModel.setRightIconVisibility(true)

        // Handle icon clicks
        headerBarView.setOnLeftIconClickListener {
            onLeftIconClick()
        }

        headerBarView.setOnRightIconClickListener {
            onRightIconClick()
        }
    }

    private fun onLeftIconClick() {
        activity?.onBackPressed()
    }

    private fun onRightIconClick() {
        // Logic for right icon click
    }

    private fun initUI() {
        fragmentSettingsAccountDetailsBinding?.recyclerViewOptions?.setHasFixedSize(true)
        fragmentSettingsAccountDetailsBinding?.recyclerViewOptions?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        val optionsList = listOf(
            ProfileOption(R.drawable.ic_action_friends, getString(R.string.text_invite_friends)),
            ProfileOption(
                R.drawable.ic_settings,
                getString(R.string.text_settings)
            )  // Add Settings option
        )

        adapter = ProfileOptionAdapter(optionsList) { option: ProfileOption ->
            when (option.title) {
                getString(R.string.text_settings) -> navigateToSettings()
                getString(R.string.text_invite_friends) -> {
                    // Share the app URL with friends
                    shareAppUrl()
                }

                else -> {}
            }
        }
        fragmentSettingsAccountDetailsBinding?.recyclerViewOptions?.adapter = adapter

        homeDetailsViewModel.fetchUser()
        lifecycleScope.launch {
            homeDetailsViewModel.userDetails.observe(viewLifecycleOwner) { user ->
                // Handle the collected user data
                if (user == null) {
                    CommonDialog().showUserDialog(
                        context = requireContext(),
                        pickImageLauncher = pickImageLauncher,
                        onSave = { name, profilePictureUri, imageView ->
                            if (name.isNotEmpty() && profilePictureUri != null) {
                                homeDetailsViewModel.saveUser(name, profilePictureUri.toString())
                            } else {
                                // Show a toast message if validation fails
                                Toast.makeText(requireContext(), "Details not saved", Toast.LENGTH_SHORT).show()
                            }
                        },
                        preSelectedImageUri = selectedImageUri // Initially no image
                    )
                } else {
                    updateUserDetails(user.name, user.profilePicture)
                }
            }
        }

        fragmentSettingsAccountDetailsBinding?.ivProfile?.setOnClickListener {
            // Launch the image picker when the image view is clicked
            pickImageLauncher.launch(AppConstants.APP_IMAGE_SELECTION_FORMAT) // Trigger the image picker
        }
    }

    private fun updateUserDetails(name: String, profilePictureUri: String) {
        fragmentSettingsAccountDetailsBinding?.tvProfileName?.text = name
        fragmentSettingsAccountDetailsBinding?.ivProfile?.let {
            Utility.loadImageIntoView(it, Uri.parse(profilePictureUri), requireContext(), isCircular = true)
        }
    }

    private fun shareAppUrl() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, AppConstants.ANDROID_APP_URL)
            type = AppConstants.APP_LINK_SHARE_FORMAT
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.text_share_app_url)))
    }

    private fun navigateToSettings() {
        findNavController().navigate(R.id.action_accountsDetailsFragment_to_settingsFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentSettingsAccountDetailsBinding = null
    }
}

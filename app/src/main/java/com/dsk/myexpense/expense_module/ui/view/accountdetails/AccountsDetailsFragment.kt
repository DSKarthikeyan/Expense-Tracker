import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.FragmentSettingsAccountDetailsBinding
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.ui.adapter.ProfileOptionAdapter
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.ui.viewmodel.HomeDetailsViewModel
import com.dsk.myexpense.expense_module.util.AppConstants
import com.dsk.myexpense.expense_module.util.Utility
import kotlinx.coroutines.launch

data class ProfileOption(
    val iconResId: Int,  // Resource ID for the icon drawable
    val title: String    // Title for the option
)

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
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it // Update the image URI
            }
        }

    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentSettingsAccountDetailsBinding = FragmentSettingsAccountDetailsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    private fun initUI() {
        fragmentSettingsAccountDetailsBinding?.recyclerViewOptions?.setHasFixedSize(true)
        fragmentSettingsAccountDetailsBinding?.recyclerViewOptions?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        val optionsList = listOf(
            ProfileOption(R.drawable.ic_action_friends, getString(R.string.text_invite_friends)),
            ProfileOption(R.drawable.ic_settings, getString(R.string.text_settings))  // Add Settings option
        )

        adapter = ProfileOptionAdapter(optionsList) { option: ProfileOption ->
            when (option.title) {
                getString(R.string.text_settings) -> navigateToSettings()
                getString(R.string.text_invite_friends)-> {
                     // Share the app URL with friends
                     shareAppUrl()
                 }
                else -> {}
            }
        }
        fragmentSettingsAccountDetailsBinding?.recyclerViewOptions?.adapter = adapter

        fragmentSettingsAccountDetailsBinding?.ivBack?.setOnClickListener {
            activity?.onBackPressed()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                homeDetailsViewModel.user.collect { user ->
                    if (user == null) {
                        Utility.showUserDialog(
                            context = requireContext(),
                            pickImageLauncher = pickImageLauncher
                        ) { name, profilePictureUri ->
                            homeDetailsViewModel.saveUser(name, profilePictureUri.toString())
                            updateUserDetails(name, profilePictureUri)
                        }

                    } else {
                        updateUserDetails(user.name, Uri.parse(user.profilePicture))
                    }
                }
            }
        }
    }

    private fun updateUserDetails(name: String, profilePictureUri: Uri?){
        fragmentSettingsAccountDetailsBinding?.tvProfileName?.text = name
        // Use Glide to load the image
        fragmentSettingsAccountDetailsBinding?.ivProfile?.let {
            Glide.with(requireContext())
                .load(profilePictureUri) // Load the image URI
                .placeholder(R.drawable.ic_action_friends) // Placeholder image
                .error(R.drawable.ic_action_friends) // Error image
                .into(it)
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

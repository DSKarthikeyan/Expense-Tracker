import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.FragmentSettingsAccountDetailsBinding
import com.dsk.myexpense.expense_module.ui.adapter.ProfileOptionAdapter
import com.dsk.myexpense.expense_module.util.AppConstants

data class ProfileOption(
    val iconResId: Int,  // Resource ID for the icon drawable
    val title: String    // Title for the option
)

class AccountsDetailsFragment : Fragment() {

    private var fragmentSettingsAccountDetailsBinding: FragmentSettingsAccountDetailsBinding? = null
    private val binding get() = fragmentSettingsAccountDetailsBinding!!
    private lateinit var adapter: ProfileOptionAdapter

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

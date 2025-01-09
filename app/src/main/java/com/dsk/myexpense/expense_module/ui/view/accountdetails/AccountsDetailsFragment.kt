import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.FragmentAccountsBinding
import com.dsk.myexpense.expense_module.ui.adapter.ProfileOptionAdapter
import com.dsk.myexpense.expense_module.util.AppConstants

data class ProfileOption(
    val iconResId: Int,  // Resource ID for the icon drawable
    val title: String    // Title for the option
)

class AccountsDetailsFragment : Fragment() {

    private var fragmentAccountsBinding: FragmentAccountsBinding? = null
    private val binding get() = fragmentAccountsBinding!!
    private lateinit var adapter: ProfileOptionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentAccountsBinding = FragmentAccountsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    private fun initUI() {
        fragmentAccountsBinding?.recyclerViewOptions?.setHasFixedSize(true)
        fragmentAccountsBinding?.recyclerViewOptions?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        val optionsList = listOf(
            ProfileOption(R.drawable.ic_action_friends, "Invite Friends"),
            ProfileOption(R.drawable.ic_settings, "Settings")  // Add Settings option
        )

        adapter = ProfileOptionAdapter(optionsList) { option: ProfileOption ->
            when (option.title) {
                "Settings" -> navigateToSettings()
                 "Invite Friends"-> {
                     // Share the app URL with friends
                     shareAppUrl()
                 }
                else -> {}
            }
        }
        fragmentAccountsBinding?.recyclerViewOptions?.adapter = adapter

        fragmentAccountsBinding?.ivBack?.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun shareAppUrl() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, AppConstants.ANDROID_APP_URL)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share App URL"))
    }

    private fun navigateToSettings() {
        findNavController().navigate(R.id.action_accountsDetailsFragment_to_settingsFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentAccountsBinding = null
    }
}

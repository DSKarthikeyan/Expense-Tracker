package com.dsk.myexpense.expense_module.ui.view

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.ActivityAddNewExpenseBinding
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.ui.adapter.CategorySpinnerAdapter
import com.dsk.myexpense.expense_module.ui.view.settings.SettingsViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.CategoryViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.ui.viewmodel.HomeDetailsViewModel
import com.dsk.myexpense.expense_module.util.AppConstants
import com.dsk.myexpense.expense_module.util.BundleKeyValues
import com.dsk.myexpense.expense_module.util.CommonDialog
import com.dsk.myexpense.expense_module.util.CurrencyUtils
import com.dsk.myexpense.expense_module.util.PermissionManager
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarView
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddNewExpenseActivity : BottomSheetDialogFragment() {

    private lateinit var binding: ActivityAddNewExpenseBinding
    private lateinit var categories: List<Category>
    private var invoiceImage: Bitmap? = null
    private var isNewExpense = true
    private var preloadedExpenseDetails: ExpenseDetails? = null
    private lateinit var selectedCurrency: String
    private lateinit var headerBarViewModel: HeaderBarViewModel
    private lateinit var headerBarView: HeaderBarView

    private val homeDetailsViewModel: HomeDetailsViewModel by viewModels {
        GenericViewModelFactory {
            HomeDetailsViewModel(
                requireContext(),
                ExpenseApplication.getExpenseRepository(requireContext()),
                ExpenseApplication.getCategoryRepository(requireContext()),
                ExpenseApplication.getCurrencyRepository(requireContext()),
                ExpenseApplication.getSettingsRepository(requireContext())
            )
        }
    }

    private val categoryViewModel: CategoryViewModel by viewModels {
        GenericViewModelFactory {
            CategoryViewModel(ExpenseApplication.getCategoryRepository(requireContext()))
        }
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        GenericViewModelFactory {
            SettingsViewModel(
                ExpenseApplication.getSettingsRepository(requireContext()),
                ExpenseApplication.getCategoryRepository(requireContext())
            )
        }
    }

    // Flag to check if the dialog is already shown
    private var isCategoryDialogOpen = false

    private val appLoadingViewModel: AppLoadingViewModel by viewModels {
        GenericViewModelFactory {
            AppLoadingViewModel(ExpenseApplication.getCategoryRepository(requireContext()),
                ExpenseApplication.getCurrencyRepository(requireContext()))
        }
    }

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedUris = result.data?.clipData?.let { clipData ->
                    (0 until clipData.itemCount).map { clipData.getItemAt(it).uri }
                } ?: listOfNotNull(result.data?.data)
                handleSelectedPhotos(selectedUris)
            } else {
                Log.d("AddNewExpenseActivity", "Photo picker cancelled")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getParcelable<ExpenseDetails>(BundleKeyValues.EXPENSE_DETAILS_KEY_EXPENSE_DETAILS)?.let {
            preloadedExpenseDetails = it
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivityAddNewExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderBar()
        setupListeners()
        setupDropdown()
        setupDatePicker()

        preloadedExpenseDetails?.let { preloadData(it) } ?: setCurrentDate()

        selectedCurrency = CurrencyUtils.getCurrencySymbol(requireContext()).toString()

        binding.addNewExpenseWidget.apply {
            amountExpenseGroup.hint = "$selectedCurrency 48.00" // Set hint on TextInputLayout
            amountExpenseGroup.isHintEnabled = true
            addExpenseAmountTextView.apply {
                visibility = View.VISIBLE
                addTextChangedListener(createCurrencyTextWatcher(selectedCurrency, this))
            }
        }
    }

    private fun setupHeaderBar() {
        headerBarViewModel = ViewModelProvider(this)[HeaderBarViewModel::class.java]
        headerBarView = binding.addNewExpenseWidget.headerBarLayout

        headerBarViewModel.apply {
            headerTitle.observe(viewLifecycleOwner) { headerBarView.setHeaderTitle(it) }
            leftIconResource.observe(viewLifecycleOwner) { headerBarView.setLeftIcon(it) }
            rightIconResource.observe(viewLifecycleOwner) { headerBarView.setRightIcon(it) }
            isLeftIconVisible.observe(viewLifecycleOwner) { headerBarView.setLeftIconVisibility(it) }
            isRightIconVisible.observe(viewLifecycleOwner) { headerBarView.setRightIconVisibility(it) }
        }
        headerBarViewModel.setLeftIconResource(R.drawable.ic_arrow_down_24)
        headerBarViewModel.setRightIconResource(R.drawable.ic_menu_24)
        headerBarView.apply {
            setOnLeftIconClickListener { dismiss() }
            setOnRightIconClickListener { onRightIconClick() }
        }

        updateUIBasedOnSelection()
    }

    private fun onRightIconClick() {
        showPopupMenu(headerBarView.getRightIcon())
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.category_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            // Prevent multiple clicks while the dialog is open
            if (isCategoryDialogOpen) {
                Log.d("SettingsFragment", "Category dialog already open, ignoring click.")
                false
            }
            // Set the flag to indicate the dialog is open
            isCategoryDialogOpen = true

            // Show the category selection dialog
            CommonDialog().showCategorySelectionDialog(
                requireContext(),
                categoryViewModel,
                onCategorySelected = { selectedCategory ->
                    // Handle the selected category
                    selectedCategory?.let {
                        // Save the selected category to the ViewModel and DB
                        settingsViewModel.setSelectedCategory(it)
                    }
                },
                onDismissDialog = { isCategorySelected ->
                    // Reset the flag after the dialog is dismissed
                    isCategoryDialogOpen = false
                }
            )
            true
        }

        popupMenu.show()
    }

    private fun updateUIBasedOnSelection() {
        binding.addNewExpenseWidget.apply {
            expenseNameGroup.isHintEnabled = true
            if (isNewExpense) {
                headerBarViewModel.setHeaderTitle(getString(R.string.text_expense))
                addExpenseButton.text = getString(R.string.text_add_expense)
                expenseNameGroup.hint = getString(R.string.text_add_expense) // Updated
            } else {
                headerBarViewModel.setHeaderTitle(getString(R.string.text_income))
                addExpenseButton.text = getString(R.string.text_add_income)
                expenseNameGroup.hint = getString(R.string.text_add_income) // Updated
            }
            addExpenseButton.setBackgroundResource(R.drawable.button_background)
            addExpenseNameTextView.clearFocus()
            addExpenseNameTextView.requestFocus()
        }
    }

    private fun setupListeners() {
        binding.addNewExpenseWidget.apply {
            addExpenseButton.setOnClickListener { validateAndSubmitExpense() }
            addExpenseAddInvoiceView.setOnClickListener { requestMediaPermission() }
            radioGroupTabLayout.setOnCheckedChangeListener { _, checkedId ->
                isNewExpense = checkedId == R.id.addNewExpense
                setupDropdown()
                updateUIBasedOnSelection()
                when (checkedId) {
                    R.id.addNewExpense -> {
                        binding.addNewExpenseWidget.addNewExpense.setBackgroundResource(R.drawable.radio_button_background)
                        binding.addNewExpenseWidget.addNewIncome.setBackgroundColor(
                            resources.getColor(
                                R.color.transparent,
                                null
                            )
                        )
                    }

                    R.id.addNewIncome -> {
                        binding.addNewExpenseWidget.addNewExpense.setBackgroundColor(
                            resources.getColor(
                                R.color.transparent,
                                null
                            )
                        )
                        binding.addNewExpenseWidget.addNewIncome.setBackgroundResource(R.drawable.radio_button_background)
                    }
                }
            }
        }
        binding.addNewExpenseWidget.apply {
            binding.addNewExpenseWidget.clearInvoiceButton.setOnClickListener {
                clearInvoiceImage()
            }
        }
    }

    private fun clearInvoiceImage() {
        invoiceImage = null
        binding.addNewExpenseWidget.addExpenseAddInvoiceView.apply {
            binding.addNewExpenseWidget.addInvoiceImageView.setImageBitmap(null)
            visibility = View.GONE
        }
        Toast.makeText(context, "Invoice image Removed", Toast.LENGTH_SHORT).show()
    }

    private fun preloadData(expenseDetails: ExpenseDetails) {
        binding.addNewExpenseWidget.apply {
            if(!expenseDetails.isIncome) addNewExpense.isChecked = true else addNewIncome.isChecked = true
            addExpenseNameTextView.setText(expenseDetails.expenseSenderName)
            addExpenseDescriptionTextView.setText(expenseDetails.expenseDescription)
            addExpenseAmountTextView.setText(expenseDetails.amount.toString())

            appLoadingViewModel.viewModelScope.launch {
                categories = appLoadingViewModel.getCategoriesByType(getCategoryType())
                val adapter = CategorySpinnerAdapter(requireContext(), categories)
                spinnerCategoryType.adapter = adapter
                val selectedCategoryIndex = expenseDetails.categoryId ?: -1

                if (selectedCategoryIndex != -1) {
                    spinnerCategoryType.setSelection(adapter.getItemPosition(selectedCategoryIndex))
                }
            }

            val dateFormatter =
                SimpleDateFormat(AppConstants.DATE_TIME_FORMAT_STRING, Locale.getDefault())
            addExpenseDateTextView.text =
                dateFormatter.format(Date(expenseDetails.expenseAddedDate))
        }
        isNewExpense = !expenseDetails.isIncome
    }

    private fun setupDropdown() {
        if (isAdded) {
            categories = appLoadingViewModel.getCategoriesByType(getCategoryType())
                val adapter = CategorySpinnerAdapter(requireContext(), categories)
                binding.addNewExpenseWidget.spinnerCategoryType.adapter = adapter
        } else {
            Log.e("AddNewExpenseActivity", "Fragment not attached to context")
        }
    }

    private fun setupDatePicker() {
        binding.addNewExpenseWidget.addExpenseDateView.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                        val selectedDate = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth, hourOfDay, minute)
                        }
                        val formattedDate = SimpleDateFormat(
                            AppConstants.DATE_TIME_FORMAT_STRING,
                            Locale.getDefault()
                        ).format(selectedDate.time)
                        binding.addNewExpenseWidget.addExpenseDateTextView.text = formattedDate
                    }, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.MINUTE], false).show()
                },
                calendar[Calendar.YEAR],
                calendar[Calendar.MONTH],
                calendar[Calendar.DAY_OF_MONTH]
            ).show()
        }
    }

    private fun setCurrentDate() {
        val currentDate =
            SimpleDateFormat(AppConstants.DATE_TIME_FORMAT_STRING, Locale.getDefault()).format(Date())
        binding.addNewExpenseWidget.addExpenseDateTextView.text = currentDate
    }

    private fun validateAndSubmitExpense() {
        val widget = binding.addNewExpenseWidget
        val expenseName = widget.addExpenseNameTextView.text.toString()
        val expenseDescription = widget.addExpenseDescriptionTextView.text.toString()
        val expenseAmount = getNumericValueFromText(
            widget.addExpenseAmountTextView.text.toString(),
            selectedCurrency
        )
        val selectedCategory = categories[widget.spinnerCategoryType.selectedItemPosition]
        val selectedDate = widget.addExpenseDateTextView.text.toString()

        // Validate fields
        if (expenseName.isEmpty()) {
            widget.addExpenseNameTextView.error = AppConstants.KEY_ERROR_MESSAGE_ADD_NEW_EXPENSE_NAME_EMPTY
            widget.addExpenseNameTextView.requestFocus()
            return
        }

        if (widget.addExpenseAmountTextView.text.toString().isEmpty()) {
            widget.addExpenseAmountTextView.error = AppConstants.KEY_ERROR_MESSAGE_ADD_NEW_EXPENSE_AMOUNT_EMPTY
            widget.addExpenseAmountTextView.requestFocus()
            return
        }

        val expenseAmountValue = getNumericValueFromText(
            widget.addExpenseAmountTextView.text.toString(),
            selectedCurrency
        )
        if (expenseAmountValue <= 0) {
            widget.addExpenseAmountTextView.error = AppConstants.KEY_ERROR_MESSAGE_ADD_NEW_EXPENSE_AMOUNT_VALID_VALUE
            widget.addExpenseAmountTextView.requestFocus()
            return
        }

        if (selectedDate.isEmpty()) {
            Toast.makeText(requireContext(), AppConstants.KEY_ERROR_MESSAGE_ADD_NEW_EXPENSE_VALID_DATE, Toast.LENGTH_SHORT)
                .show()
            widget.addExpenseDateView.performClick() // Open date picker
            return
        }

        val dateInMilliseconds = getDateInMilliseconds(selectedDate)
        if (dateInMilliseconds <= 0) {
            Toast.makeText(requireContext(), AppConstants.KEY_ERROR_MESSAGE_ADD_NEW_EXPENSE_INVALID_DATE, Toast.LENGTH_SHORT).show()
            widget.addExpenseDateView.performClick()
            return
        }

        val expenseDetailToSave = preloadedExpenseDetails?.copy(
            amount = expenseAmount,
            expenseAddedDate = dateInMilliseconds,
            isIncome = !isNewExpense,
            categoryId = selectedCategory.id,
            expenseSenderName = expenseName,
            expenseDescription = expenseDescription
        ) ?: ExpenseDetails(
            amount = expenseAmount,
            expenseAddedDate = dateInMilliseconds,
            isIncome = !isNewExpense,
            categoryId = selectedCategory.id,
            expenseSenderName = expenseName,
            expenseReceiverName = expenseName,
            expenseDescription = expenseDescription,
            expenseMessageSenderName = "Direct App"
        )

        if (expenseDetailToSave.expenseID != null) {
            homeDetailsViewModel.updateExpense(
                requireContext(),
                expenseDetailToSave,
                invoiceImage,
                selectedCategory.name
            )
            Log.d("DsK","expenseDetailToSave $expenseDetailToSave")
        } else {
            homeDetailsViewModel.insertExpense(
                requireContext(),
                expenseDetailToSave,
                invoiceImage,
                selectedCategory.name
            )
        }

        dismiss()
    }

    private fun getNumericValueFromText(input: String, currencySymbol: String): Double {
        return input.replace("$currencySymbol ", AppConstants.EMPTY_STRING).trim().toDoubleOrNull() ?: 0.0
    }

    private fun getDateInMilliseconds(selectedDate: String): Long {
        val formatter = SimpleDateFormat(AppConstants.DATE_TIME_FORMAT_STRING, Locale.getDefault())
        return try {
            formatter.parse(selectedDate)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getCategoryType(): String {
        return if (isNewExpense) getString(R.string.text_expense) else getString(R.string.text_income)
    }

    private fun requestMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionManager.requestPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES,
                onGranted = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                        SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2
                    ) {
                        showPhotoPicker()
                    } else {
                        accessMedia()
                    }
                },
                onDenied = {
                    Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            accessMedia()
        }
    }

    private fun accessMedia() {
        context?.let { context ->
            val contentResolver = context.contentResolver
            val imageUris = mutableListOf<Uri>()

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            val query = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            query?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    imageUris.add(contentUri)
                }
            }

            if (imageUris.isNotEmpty()) {
                handleSelectedPhotos(imageUris)
            } else {
                Log.d("AddNewExpenseActivity", "No images found in media")
            }
        }
    }

    private fun showPhotoPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 14+, we use ACTION_PICK_IMAGES to open the new photo picker UI
            val intent = Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = AppConstants.APP_IMAGE_SELECTION_FORMAT // Setting image type for the picker
                putExtra(
                    MediaStore.EXTRA_PICK_IMAGES_MAX,
                    4
                ) // Allowing the user to pick up to 10 images
            }
            photoPickerLauncher.launch(intent)
        } else {
            // For devices below Android 14, use the traditional approach (as already implemented)
            accessMedia()
        }
    }

    private fun handleSelectedPhotos(selectedUris: List<Uri>) {
        if (selectedUris.isNotEmpty()) {
            val inputStream = context?.contentResolver?.openInputStream(selectedUris[0])
            invoiceImage = BitmapFactory.decodeStream(inputStream)

            if (invoiceImage != null) {
                binding.addNewExpenseWidget.addExpenseAddInvoiceView.apply {
                    binding.addNewExpenseWidget.addInvoiceImageView.setImageBitmap(invoiceImage)
                    visibility = View.VISIBLE
                }
            }
        } else {
            Toast.makeText(context, "No photos selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCurrencyTextWatcher(
        currencySymbol: String,
        editText: TextInputEditText
    ): TextWatcher {
        return object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                isUpdating = true
                val inputText = s.toString()
                val requiredPrefix = "$currencySymbol "

                if (!inputText.startsWith(requiredPrefix)) {
                    val updatedText =
                        "$requiredPrefix${inputText.replace(currencySymbol, AppConstants.EMPTY_STRING).trim()}"
                    editText.setText(updatedText)
                    editText.setSelection(updatedText.length)
                }

                isUpdating = false
            }
        }
    }
}

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
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.ui.viewmodel.HomeDetailsViewModel
import com.dsk.myexpense.expense_module.util.AppConstants
import com.dsk.myexpense.expense_module.util.PermissionManager
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarView
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddNewExpenseActivity : BottomSheetDialogFragment() {

    private lateinit var addExpenseView: ActivityAddNewExpenseBinding
    private lateinit var categories: List<Category>
    private var invoiceImage: Bitmap? = null
    private var isNewExpense = true
    private lateinit var selectedCurrency: String
    private var preloadedExpenseDetails: ExpenseDetails? = null
    private lateinit var headerBarViewModel: HeaderBarViewModel
    private lateinit var headerBarView: HeaderBarView

    private val homeDetailsViewModel: HomeDetailsViewModel by viewModels {
        GenericViewModelFactory {
            HomeDetailsViewModel((requireActivity().application as ExpenseApplication).expenseRepository)
        }
    }

    private val appLoadingViewModel: AppLoadingViewModel by viewModels {
        GenericViewModelFactory {
            AppLoadingViewModel((requireActivity().application as ExpenseApplication).expenseRepository)
        }
    }

    // Activity result launcher for photo picker
    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedUris = result.data?.clipData?.let { clipData ->
                (0 until clipData.itemCount).map { clipData.getItemAt(it).uri }
            } ?: listOfNotNull(result.data?.data)

            handleSelectedPhotos(selectedUris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getParcelable<ExpenseDetails>("expenseDetails")?.let {
            preloadedExpenseDetails = it
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        addExpenseView = ActivityAddNewExpenseBinding.inflate(inflater, container, false)
        return addExpenseView.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupListeners()
        setupDropdown()
        setupDatePicker()

        preloadedExpenseDetails?.let { preloadData(it) } ?: setCurrentDate()

        headerBarViewModel = ViewModelProvider(this)[HeaderBarViewModel::class.java]
        headerBarView = addExpenseView.addNewExpenseWidget.headerBarLayout

        // Bind ViewModel LiveData to the HeaderBarView
        headerBarViewModel.headerTitle.observe(this, { title ->
            headerBarView.setHeaderTitle(title)
        })

        headerBarViewModel.leftIconResource.observe(this, { iconResId ->
            headerBarView.setLeftIcon(iconResId)
        })

        headerBarViewModel.rightIconResource.observe(this, { iconResId ->
            headerBarView.setRightIcon(iconResId)
        })

        headerBarViewModel.isLeftIconVisible.observe(this, { isVisible ->
            headerBarView.setLeftIconVisibility(isVisible)
        })

        headerBarViewModel.isRightIconVisible.observe(this, { isVisible ->
            headerBarView.setRightIconVisibility(isVisible)
        })

        // Example: Updating the header dynamically
        updateUIBasedOnSelection()
        headerBarViewModel.setLeftIconResource(R.drawable.ic_arrow_down_24)
        headerBarViewModel.setRightIconResource(R.drawable.ic_menu_24)
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
        dismiss()
    }

    private fun onRightIconClick() {
        // Logic for right icon click
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.layoutParams?.height =
            ViewGroup.LayoutParams.MATCH_PARENT
    }

    private fun setupViews() {
        PermissionManager.init(this)
    }

    private fun setupListeners() {
        addExpenseView.addNewExpenseWidget.apply {
            addExpenseButton.setOnClickListener { validateAndSubmitExpense() }
            addExpenseAddInvoiceView.setOnClickListener { requestMediaPermission() }
            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                isNewExpense = when (checkedId) {
                    R.id.addNewIncome -> false
                    R.id.addNewExpense -> true
                    else -> isNewExpense
                }
                setupDropdown()
                updateUIBasedOnSelection()
            }
        }
    }

    private fun preloadData(expenseDetails: ExpenseDetails) {
        addExpenseView.addNewExpenseWidget.apply {
            addExpenseNameTextView.setText(expenseDetails.expenseSenderName)
            addExpenseDescriptionTextView.setText(expenseDetails.expenseDescription)
            addExpenseAmountTextView.setText(expenseDetails.amount.toString())
            // Preload category
            appLoadingViewModel.viewModelScope.launch {
                categories = appLoadingViewModel.getCategoriesByType(if (isNewExpense) getString(R.string.text_expense) else getString(R.string.text_income))

                // Set spinner adapter and preselect category
                val adapter = CategorySpinnerAdapter(requireContext(), categories)
                addExpenseView.addNewExpenseWidget.spinnerCategoryType.adapter = adapter

                val selectedCategoryIndex = categories.indexOfFirst { it.id == expenseDetails.categoryId }
                if (selectedCategoryIndex != -1) {
                    addExpenseView.addNewExpenseWidget.spinnerCategoryType.setSelection(selectedCategoryIndex)
                }
            }
            // Convert milliseconds to date and display it
            val dateFormatter = SimpleDateFormat(AppConstants.DATE_FORMAT_STRING, Locale.getDefault())
            addExpenseDateTextView.text = dateFormatter.format(Date(expenseDetails.expenseAddedDate))
        }
        isNewExpense = !expenseDetails.isIncome
    }

    private fun setupDropdown() {
        appLoadingViewModel.viewModelScope.launch {
            val categoryType = if (isNewExpense) getString(R.string.text_expense) else getString(R.string.text_income)
            categories = appLoadingViewModel.getCategoriesByType(categoryType)
            addExpenseView.addNewExpenseWidget.spinnerCategoryType.adapter =
                CategorySpinnerAdapter(requireContext(), categories)
        }
    }

    private fun setupDatePicker() {
        addExpenseView.addNewExpenseWidget.addExpenseDateView.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = context?.let { context ->
                DatePickerDialog(context, { _, year, month, dayOfMonth ->
                    val timePickerDialog = TimePickerDialog(context, { _, hourOfDay, minute ->
                        val selectedDate = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth, hourOfDay, minute)
                        }
                        val formattedDate = SimpleDateFormat(
                            AppConstants.DATE_FORMAT_STRING,
                            Locale.getDefault()
                        ).format(selectedDate.time)
                        addExpenseView.addNewExpenseWidget.addExpenseDateTextView.text = formattedDate
                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)

                    timePickerDialog.show()
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            }
            datePickerDialog?.show()
        }
    }

    private fun setCurrentDate() {
        val currentDate = SimpleDateFormat(AppConstants.DATE_FORMAT_STRING, Locale.getDefault()).format(Date())
        addExpenseView.addNewExpenseWidget.addExpenseDateTextView.text = currentDate
    }

    private fun validateAndSubmitExpense() {
        val widget = addExpenseView.addNewExpenseWidget
        val expenseName = widget.addExpenseNameTextView.text.toString()
        val expenseDescription = widget.addExpenseDescriptionTextView.text.toString()
        val expenseAmount = widget.addExpenseAmountTextView.text?.parseToDouble()
        val selectedCategory = categories[widget.spinnerCategoryType.selectedItemPosition]
        val selectedDate = widget.addExpenseDateTextView.text.toString()

        Log.d("AddNewExpense", "Expense Name: $expenseName")
        Log.d("AddNewExpense", "Description: $expenseDescription")
        Log.d("AddNewExpense", "Amount: $expenseAmount")
        Log.d("AddNewExpense", "Category: ${selectedCategory.id}")
        Log.d("AddNewExpense", "Date: $selectedDate")

        val dateInMilliseconds = getDateInMilliseconds(selectedDate)
        val expenseDetailToSave = preloadedExpenseDetails?.copy(
            amount = expenseAmount!!,
            expenseAddedDate = dateInMilliseconds,
            isIncome = !isNewExpense,
            categoryId = selectedCategory.id,
            expenseSenderName = expenseName,
            expenseDescription = expenseDescription
        ) ?: ExpenseDetails(
            amount = expenseAmount!!,
            expenseAddedDate = dateInMilliseconds,
            isIncome = !isNewExpense,
            categoryId = selectedCategory.id,
            expenseSenderName = expenseName,
            expenseReceiverName = expenseName,
            expenseDescription = expenseDescription,
            expenseMessageSenderName = "Direct App"
        )

        Log.d("AddNewExpense", "Final Expense Details: $expenseDetailToSave")

        if (expenseDetailToSave.expenseID != null) {
            homeDetailsViewModel.updateExpense(expenseDetailToSave, invoiceImage, selectedCategory.name)
            Log.d("AddNewExpense", "Updating expense")
        } else {
            homeDetailsViewModel.insertExpense(expenseDetailToSave, invoiceImage, selectedCategory.name)
            Log.d("AddNewExpense", "Inserting new expense")
        }

        dismiss()
    }


    private fun getDateInMilliseconds(selectedDate: String): Long {
        val dateWithTimeFormatter = SimpleDateFormat(AppConstants.DATE_FORMAT_STRING, Locale.getDefault())
        val dateWithoutTimeFormatter = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())

        return try {
            val parsedDate = try {
                // Try parsing with time first
                dateWithTimeFormatter.parse(selectedDate)
            } catch (e: ParseException) {
                // Fallback to parsing without time
                dateWithoutTimeFormatter.parse(selectedDate)?.let {
                    // Add current time to the parsed date
                    val calendar = Calendar.getInstance().apply {
                        time = it
                        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
                        set(Calendar.HOUR_OF_DAY, currentHour)
                        set(Calendar.MINUTE, currentMinute)
                        set(Calendar.SECOND, 0)
                    }
                    return@let Date(calendar.timeInMillis)
                }
            }
            parsedDate?.time ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    private fun Editable?.parseToDouble(): Double? {
        return this?.toString()?.toDoubleOrNull()
    }

    private fun updateUIBasedOnSelection() {
        if (isNewExpense) {
            headerBarViewModel.setHeaderTitle(getString(R.string.text_expense))
            addExpenseView.addNewExpenseWidget.addExpenseButton.text = getString(R.string.text_add_expense)
        } else {
            headerBarViewModel.setHeaderTitle(getString(R.string.text_invoice))
            addExpenseView.addNewExpenseWidget.addExpenseButton.text = getString(R.string.text_add_invoice)
        }
    }

    private fun requestMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionManager.requestPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES,
                onGranted = { showPhotoPicker() },
                onDenied = { Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show() }
            )
        } else {
            accessMedia()
        }
    }

    private fun showPhotoPicker() {
        val intent = Intent(MediaStore.ACTION_PICK_IMAGES).apply {
            type = "image/*"
            putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 10)
        }
        photoPickerLauncher.launch(intent)
    }

    private fun handleSelectedPhotos(selectedUris: List<Uri>) {
        if (selectedUris.isNotEmpty()) {
            val firstUri = selectedUris[0]
            val inputStream = context?.contentResolver?.openInputStream(firstUri)
            invoiceImage = BitmapFactory.decodeStream(inputStream)
            addExpenseView.addNewExpenseWidget.addInvoiceImageView.setImageBitmap(invoiceImage)
        } else {
            Toast.makeText(context, "No photos selected", Toast.LENGTH_SHORT).show()
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
            }
        }
    }
}

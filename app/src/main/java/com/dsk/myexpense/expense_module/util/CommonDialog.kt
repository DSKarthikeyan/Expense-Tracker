package com.dsk.myexpense.expense_module.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.ui.adapter.CurrencyAdapter
import com.dsk.myexpense.expense_module.ui.adapter.ImageSelectionAdapter
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.CategoryViewModel
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommonDialog {

    fun showCurrencySelectionDialog(
        context: Context,
        appLoadingViewModel: AppLoadingViewModel,
        onCurrencySelected: (String, Double) -> Unit
    ) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_currency_selection, null)

        val dialog = AlertDialog.Builder(context).setView(view).setCancelable(true)
            .setNegativeButton(context.resources.getString(R.string.text_cancel)) { dialogInterface, _ ->
                dialogInterface.dismiss()  // Close the dialog if Cancel is pressed
            }.create()

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewCurrencies)
        val searchEditText = view.findViewById<TextInputEditText>(R.id.searchEditText)

        // Fetch and filter unique currencies
        CoroutineScope(Dispatchers.IO).launch {
            val allCurrencies = appLoadingViewModel.getCurrenciesFromLocalDB()
            val distinctCurrencies =
                allCurrencies.distinctBy { it.name } // Assuming 'name' is a unique field
            withContext(Dispatchers.Main) {
                val adapter = CurrencyAdapter(distinctCurrencies) { selectedCurrency ->
                    onCurrencySelected(selectedCurrency.name, selectedCurrency.code)
                    dialog.dismiss()
                }

                recyclerView.adapter = adapter
                recyclerView.layoutManager = LinearLayoutManager(context)

                // Add search functionality
//                searchEditText.addTextChangedListener { editableQuery ->
//                    val query = editableQuery.toString()
//                    // Filter the adapter based on the search query
//                    val filteredList = distinctCurrencies.filter {
//                        it.name.contains(query, ignoreCase = true)
//                    }
//                    adapter.updateCurrencies(filteredList) // Ensure `updateCurrencies` is defined in your adapter
//                }

                searchEditText.addTextChangedListener { query ->
                    // Filter the adapter based on the search query
                    adapter.filter(query.toString())
//                    adapter.updateCurrencies(filteredList)
                }
            }
        }

        dialog.show()
    }

    private var isDialogOpened = false

    fun showCategorySelectionDialog(
        context: Context,
        categoryViewModel: CategoryViewModel,
        onCategorySelected: (Category?) -> Unit,
        onDismissDialog: (Boolean?) -> Unit
    ) {
        // Observe the LiveData from the ViewModel
        categoryViewModel.categories.observe(context as LifecycleOwner) { categories ->
            // If there are no categories, handle that scenario
            if (categories.isNullOrEmpty()) {
                Log.d("CommonDialog", "categories: empty")
                onCategorySelected(null)  // Return null if no categories available
            } else {
                Log.d("CommonDialog", "categories: not empty")

                // Create an ArrayAdapter for the list of category names
                val adapter =
                    object : ArrayAdapter<Category>(context, R.layout.item_category, categories) {
                        override fun getView(
                            position: Int, convertView: View?, parent: ViewGroup
                        ): View {
                            val view = convertView ?: LayoutInflater.from(context)
                                .inflate(R.layout.item_category, parent, false)

                            // Get category and set up the views
                            val category = getItem(position)
                            val categoryNameTextView =
                                view.findViewById<TextView>(R.id.categoryName)
                            val deleteButton = view.findViewById<ImageView>(R.id.deleteButton)

                            categoryNameTextView.text = category?.name

                            // Handle the delete button click
                            deleteButton.setOnClickListener {
                                category?.let {
                                    // Call the ViewModel to delete the category
                                    categoryViewModel.deleteCategory(it)
                                    Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }

                            return view
                        }
                    }

                // Create the dialog for selecting a category
                val dialog = AlertDialog.Builder(context)
                    .setTitle(context.resources.getString(R.string.text_category))
                    .setCancelable(true).setAdapter(adapter) { _, which ->
                        // When a category is selected, fetch the corresponding category
                        val selectedCategory = categories[which]
                        onCategorySelected(selectedCategory)  // Pass the selected category to the callback
                    }
                    .setNegativeButton(context.resources.getString(R.string.text_cancel)) { dialogInterface, _ ->
                        isDialogOpened = false
                        dialogInterface.dismiss()  // Close the dialog if Cancel is pressed
                        onDismissDialog(false)
                    }.create()

                // Add the "Add New Category" button at the bottom of the dialog
                dialog.setButton(
                    AlertDialog.BUTTON_POSITIVE,
                    context.resources.getString(R.string.text_add_new_category)
                ) { dialogInterface, _ ->
                    showAddCategoryDialog(context, categoryViewModel) { newCategory ->
                        if (newCategory != null) {
                            onCategorySelected(newCategory)  // Return the new category
                        } else {
                            onCategorySelected(null) // Return null if no category was added
                        }
                    }
                    isDialogOpened = false
                    dialogInterface.dismiss()  // Dismiss the dialog once the button is clicked
                }

                // Set an onDismissListener to trigger onDismissDialog callback
                dialog.setOnDismissListener {
                    isDialogOpened = false
                    // This will be called when the dialog is dismissed (either canceled or selected)
                    onDismissDialog(true)
                }

                Log.d("CommonDialog", "isDialogOpened $isDialogOpened")
                if (!isDialogOpened) {
                    isDialogOpened = true
                    dialog.show()
                }
            }
        }
    }

    private fun showAddCategoryDialog(
        context: Context, categoryViewModel: CategoryViewModel, onCategoryAdded: (Category?) -> Unit
    ) {
        // Inflate the custom layout
        val layout = LayoutInflater.from(context).inflate(R.layout.dialog_add_category, null)

        // Find the views in the custom layout
        val categoryNameEditText = layout.findViewById<EditText>(R.id.categoryNameEditText)
        val typeSpinner = layout.findViewById<Spinner>(R.id.typeSpinner)

        // Create an array of types (income, expense)
        val types = arrayOf(
            context.resources.getString(R.string.text_income),
            context.resources.getString(R.string.text_expense)
        )
        var selectedType = context.resources.getString(R.string.text_income)  // Default type

        // Set up the spinner
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter
        typeSpinner.setSelection(0)  // Default to "income"
        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                selectedType =
                    types[position]  // Update selected type based on the spinner selection
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }

        // Create the dialog to add the new category
        val addCategoryDialog = AlertDialog.Builder(context)
            .setTitle(context.resources.getString(R.string.text_add_new_category)).setView(layout)
            .setPositiveButton(context.resources.getString(R.string.text_add)) { dialog, _ ->
                val categoryName = categoryNameEditText.text.toString()
                if (categoryName.isNotBlank()) {
                    // Create a new category object
                    val newCategory = Category(
                        name = categoryName,
                        type = selectedType,
                        iconResId = R.drawable.ic_other_expenses
                    )
                    // Insert the new category into the database
                    categoryViewModel.addCategory(newCategory)

                    // Observe the result of category addition
                    categoryViewModel.newCategory.observe(context as LifecycleOwner) { addedCategory ->
                        onCategoryAdded(addedCategory)  // Return the added category
                    }

                    dialog.dismiss()
                } else {
                    // Handle empty category name case
                    Toast.makeText(context, "Category name cannot be empty", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .setNegativeButton(context.resources.getString(R.string.text_cancel)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }.create()

        // Show the dialog for adding a new category
        addCategoryDialog.show()
    }

    fun showUserDialog(
        context: Context,
        pickImageLauncher: ActivityResultLauncher<String>,
        onSave: (name: String, profilePictureUri: Uri?, imageView: ImageView) -> Unit,
        preSelectedImageUri: Uri? = null
    ): View {
        // Inflate the dialog view
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_user, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        val profilePictureImageView = dialogView.findViewById<ImageView>(R.id.profilePictureImageView)

        // Set initial image (if any)
        preSelectedImageUri?.let {
            Utility.loadImageIntoView(profilePictureImageView, preSelectedImageUri, context, isCircular = true)
        }

        var selectedImageURIValue: Uri? = null
        // Handle the image selection click
        profilePictureImageView.setOnClickListener {
            // Launch the image picker
            CommonDialog().showImageSelectionDialog(
                context = context,
                availableImages = Utility.getDefaultProfileImages(context, R.array.profile_default_images),
                pickImageLauncher = pickImageLauncher
            ) { selectedImageUri ->
                // Update the image in the User Dialog after selection
                if (selectedImageUri != null) {
                    Utility.loadImageIntoView(
                        profilePictureImageView,
                        selectedImageUri,
                        context,
                        isCircular = true
                    )
                    selectedImageURIValue = selectedImageUri
                }
            }
        }

        // Build the AlertDialog
        val alertDialog = android.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton(R.string.text_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.text_save, null)
            .create()

        // Show the dialog and set up custom behavior
        alertDialog.show()

        // Handle the save button click
        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            val name = nameEditText.text.toString()

            // Validate the inputs
            if (name.isNotEmpty() && selectedImageURIValue!= null) {
                onSave(name, selectedImageURIValue, profilePictureImageView)
                alertDialog.dismiss()
            } else {
                // Show a toast message if validation fails
                Toast.makeText(context, "Please fill in all details", Toast.LENGTH_SHORT).show()
            }
        }

        return dialogView
    }

    // Function to show image selection dialog
    private fun showImageSelectionDialog(
        context: Context,
        availableImages: List<Int>,
        pickImageLauncher: ActivityResultLauncher<String>,
        onImageSelected: (Uri?) -> Unit
    ) {
        // Add a placeholder for selecting from phone storage
        val extendedImages = listOf(R.drawable.ic_add_photo) + availableImages
        var imageSelectionDialog: AlertDialog? = null
        // Inflate the dialog view
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_image_selection, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewImages)

        // Set up RecyclerView
        recyclerView.layoutManager = GridLayoutManager(context, 3)
        recyclerView.adapter = ImageSelectionAdapter(extendedImages) { selectedImageResId ->
            if (selectedImageResId == R.drawable.ic_add_photo) {
                // Launch phone storage picker
                pickImageLauncher.launch(AppConstants.APP_IMAGE_SELECTION_FORMAT)
            } else {
                // Create URI for the selected drawable resource
                val imageUri = Uri.parse("android.resource://${context.packageName}/$selectedImageResId")
                Log.d("DsK","imageUri $imageUri")
                onImageSelected(imageUri)
                imageSelectionDialog?.dismiss()
            }
        }

        // Build and show the dialog
        imageSelectionDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setTitle("Select an Image")
            .setNegativeButton(R.string.text_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        imageSelectionDialog.show()
    }

}


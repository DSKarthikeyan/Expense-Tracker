package com.dsk.myexpense.expense_module.util

import android.content.Context
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
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.ui.adapter.CurrencyAdapter
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
        categoryViewModel.fetchCategories() // Ensure categories are loaded first
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
                                view.findViewById<TextView>(R.id.category_name)
                            val deleteButton = view.findViewById<ImageView>(R.id.delete_button)

                            categoryNameTextView.text = category?.name

                            // Handle the delete button click
                            deleteButton.setOnClickListener {
                                category?.let {
                                    // Call the ViewModel to delete the category
                                    categoryViewModel.deleteCategory(it)
                                    Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT)
                                        .show()

                                    // Notify the adapter to refresh the list
                                    categoryViewModel.fetchCategories() // Re-fetch the updated list of categories
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


}


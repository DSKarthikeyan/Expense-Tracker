package com.dsk.myexpense.expense_module.util

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.model.Currency
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility classes for doing stuffs such as hiding keyboard, checking if network is available etc
 */

object Utility {

    val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    //hide keyboard
    fun hideKeyboard(activity: Activity) {
        val imm: InputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view: View? = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    //check if network is connected
    fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        return false
    }

    fun getDateTime(milliseconds: Long): Pair<String?, String?> {
        val date = Date(milliseconds)
        val dateFormat = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm a", Locale.getDefault())

        val formattedDate = dateFormat.format(date)
        val formattedTime = timeFormat.format(date)

        return Pair(formattedDate, formattedTime)
    }

    fun isAppInForeground(application: Application): Boolean {
        val activityManager =
            application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses

        return runningProcesses.any { it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }
    }

    fun getForegroundActivity(application: Application): Activity? {
        return if (application is ExpenseApplication) {
            application.getCurrentActivity()
        } else {
            null
        }
    }

    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) // Adjust format and quality if needed
        return stream.toByteArray()
    }

    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    fun isDarkModeEnabled(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    fun getResourcesName(prefix: String, key: String, context: Context): String {
        val resourceName = "$prefix$key".lowercase() // Combine prefix and key
        val resourceId = context.resources.getIdentifier(resourceName, "string", context.packageName)

        return if (resourceId != 0) {
            context.getString(resourceId) // Return the string resource if it exists
        } else {
            "Resource not found" // Return a default message if the resource is missing
        }
    }

    fun convertExpenseAmountToUSD(
        context: Context,
        expenseDetails: ExpenseDetails
    ): ExpenseDetails {
        val exchangeRate = CurrencyCache.getExchangeRate(context)
        val amountInUSD = CurrencyUtils.convertToUSD(expenseDetails.amount, exchangeRate)

        Log.d("DsK","convertExpenseAmountToUSD $exchangeRate $amountInUSD")
        // Return a new ExpenseDetails instance with the updated amount
        return expenseDetails.copy(amount = amountInUSD)
    }

    fun exportToCsv(context: Context, expenseDetails: List<ExpenseDetails>, categories: List<Category>, currencies: List<Currency>) {
        try {
            val expenseFile = File(context.filesDir, "expense_details.csv")
            val categoryFile = File(context.filesDir, "categories.csv")
            val currencyFile = File(context.filesDir, "currencies.csv")

            // Export ExpenseDetails to CSV
            val expenseWriter = FileWriter(expenseFile)
            expenseWriter.append("expenseSenderName,expenseMessageSenderName,expenseReceiverName,expenseDescription,amount,isIncome,categoryId,expenseID,expenseAddedDate\n")
            expenseDetails.forEach {
                expenseWriter.append("${it.expenseSenderName},${it.expenseMessageSenderName},${it.expenseReceiverName},${it.expenseDescription},${it.amount},${it.isIncome},${it.categoryId},${it.expenseID},${it.expenseAddedDate}\n")
            }
            expenseWriter.flush()
            expenseWriter.close()

            // Export Categories to CSV
            val categoryWriter = FileWriter(categoryFile)
            categoryWriter.append("id,name,type,iconResId\n")
            categories.forEach {
                categoryWriter.append("${it.id},${it.name},${it.type},${it.iconResId}\n")
            }
            categoryWriter.flush()
            categoryWriter.close()

            // Export Currencies to CSV
            val currencyWriter = FileWriter(currencyFile)
            currencyWriter.append("id,code,name,symbol\n")
            currencies.forEach {
                currencyWriter.append("${it.id},${it.code},${it.name},${it.symbol}\n")
            }
            currencyWriter.flush()
            currencyWriter.close()

            Log.d("DsK","$expenseFile -- $categoryFile -- $currencyFile")
            Toast.makeText(context, "Exported to CSV successfully! $categoryFile", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export to CSV ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun importFromCsv(context: Context): Triple<List<ExpenseDetails>, List<Category>, List<Currency>> {
        val expenseDetails = mutableListOf<ExpenseDetails>()
        val categories = mutableListOf<Category>()
        val currencies = mutableListOf<Currency>()

        try {
            val expenseFile = File(context.filesDir, "expense_details.csv")
            val categoryFile = File(context.filesDir, "categories.csv")
            val currencyFile = File(context.filesDir, "currencies.csv")

            // Read ExpenseDetails from CSV
            val expenseReader = BufferedReader(FileReader(expenseFile))
            expenseReader.readLine() // Skip header
            var line: String?
            while (expenseReader.readLine().also { line = it } != null) {
                val data = line!!.split(",")
                expenseDetails.add(
                    ExpenseDetails(
                        expenseSenderName = data[0],
                        expenseMessageSenderName = data[1],
                        expenseReceiverName = data[2],
                        expenseDescription = data[3],
                        amount = data[4].toDouble(),
                        isIncome = data[5].toBoolean(),
                        categoryId = data[6].toInt(),
                        expenseID = data[7].toInt(),
                        expenseAddedDate = data[8].toLong()
                    )
                )
            }
            expenseReader.close()

            // Read Categories from CSV
            val categoryReader = BufferedReader(FileReader(categoryFile))
            categoryReader.readLine() // Skip header
            while (categoryReader.readLine().also { line = it } != null) {
                val data = line!!.split(",")
                categories.add(Category(id = data[0].toInt(), name = data[1], type = data[2], iconResId = data[3].toInt()))
            }
            categoryReader.close()

            // Read Currencies from CSV
            val currencyReader = BufferedReader(FileReader(currencyFile))
            currencyReader.readLine() // Skip header
            while (currencyReader.readLine().also { line = it } != null) {
                val data = line!!.split(",")
                currencies.add(Currency(id = data[0].toInt(), code = data[1].toDouble(), name = data[2], symbol = data[3]))
            }
            currencyReader.close()

            Toast.makeText(context, "Imported from CSV successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to import from CSV", Toast.LENGTH_SHORT).show()
        }

        return Triple(expenseDetails, categories, currencies)
    }

    fun exportToJson(context: Context, expenseDetails: List<ExpenseDetails>, categories: List<Category>, currencies: List<Currency>) {
        try {
            val jsonData = mapOf(
                "expenses" to expenseDetails,
                "categories" to categories,
                "currencies" to currencies
            )
            val json = Gson().toJson(jsonData)
            val file = File(context.filesDir, "data.json")
            file.writeText(json)
            Log.d("DsK","JSON File Path $file")
            Toast.makeText(context, "Exported to JSON successfully! JSON File Path $file", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export to JSON", Toast.LENGTH_SHORT).show()
        }
    }

    fun importFromJson(context: Context): Triple<List<ExpenseDetails>, List<Category>, List<Currency>> {
        val expenseDetails: MutableList<ExpenseDetails> = mutableListOf()
        val categories: MutableList<Category> = mutableListOf()
        val currencies: MutableList<Currency> = mutableListOf()

        try {
            val file = File(context.filesDir, "data.json")
            val json = file.readText()
            val jsonData = Gson().fromJson(json, Map::class.java)

            val expensesJson = jsonData["expenses"] as List<Map<String, Any>>
            expensesJson.forEach {
                expenseDetails.add(
                    ExpenseDetails(
                        expenseSenderName = it["expenseSenderName"] as String,
                        expenseMessageSenderName = it["expenseMessageSenderName"] as String,
                        expenseReceiverName = it["expenseReceiverName"] as String,
                        expenseDescription = it["expenseDescription"] as String,
                        amount = (it["amount"] as Double),
                        isIncome = it["isIncome"] as Boolean,
                        categoryId = (it["categoryId"] as Double).toInt(),
                        expenseID = (it["expenseID"] as Double).toInt(),
                        expenseAddedDate = (it["expenseAddedDate"] as Double).toLong()
                    )
                )
            }

            val categoriesJson = jsonData["categories"] as List<Map<String, Any>>
            categoriesJson.forEach {
                categories.add(Category(it["id"] as Int, it["name"] as String, it["type"] as String, it["iconResId"] as Int))
            }

            val currenciesJson = jsonData["currencies"] as List<Map<String, Any>>
            currenciesJson.forEach {
                currencies.add(Currency(it["id"] as Int, it["code"] as Double, it["name"] as String, it["symbol"] as String))
            }

            Toast.makeText(context, "Imported from JSON successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to import from JSON", Toast.LENGTH_SHORT).show()
        }

        return Triple(expenseDetails, categories, currencies)
    }


    fun writeToFile(context: Context, fileName: String, data: String) {
        val file = File(context.filesDir, fileName)
        file.writeText(data)
    }

    fun readFromFile(context: Context, fileName: String): String {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) file.readText() else ""
    }

    fun showUserDialog(
        context: Context,
        pickImageLauncher: ActivityResultLauncher<String>,
        onSave: (name: String, profilePictureUri: Uri?) -> Unit
    ): View {
        // Inflate the dialog view
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_user, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        val profilePictureImageView = dialogView.findViewById<ImageView>(R.id.profilePictureImageView)

        // Declare a variable to hold the selected image URI
        var selectedImageUri: Uri? = null

        // Handle the image selection click
        profilePictureImageView.setOnClickListener {
            // Launch the image picker when the image view is clicked
            pickImageLauncher.launch("image/*") // Trigger the image picker
        }

        // Build the AlertDialog
        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)  // Prevent the dialog from being dismissed unless valid data is provided
            .setPositiveButton(R.string.text_save) { dialog, _ ->
                // Get the name entered by the user
                val name = nameEditText.text.toString()

                // Validate the inputs
                if (name.isNotEmpty() && selectedImageUri != null) {
                    // If both name and image are provided, save the data
                    onSave(name, selectedImageUri)  // Pass the data to the callback
                    dialog.dismiss()  // Dismiss the dialog once the data is saved
                } else {
                    // Show a toast message if validation fails
                    Toast.makeText(context, "Please fill in all details", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.text_cancel) { dialog, _ ->
                dialog.dismiss()  // Cancel button to dismiss the dialog
            }
            .create()

        alertDialog.show()
        return dialogView
    }




}
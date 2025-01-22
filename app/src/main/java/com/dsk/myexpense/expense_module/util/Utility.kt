package com.dsk.myexpense.expense_module.util

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.model.Currency
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.source.local.db.DailyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.db.MonthlyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.db.WeeklyExpenseSum
import com.dsk.myexpense.expense_module.data.source.local.db.WeeklyExpenseWithTime
import com.dsk.myexpense.expense_module.util.AppConstants.Companion.APP_CSV_FILE_NAME_EXTENSION
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

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
        val dateFormat = SimpleDateFormat(AppConstants.DATE_FORMAT_STRING, Locale.getDefault())
        val timeFormat = SimpleDateFormat(AppConstants.RAILWAY_TIME_FORMAT_STRING, Locale.getDefault())

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
        val resourceId = context.resources.getIdentifier(resourceName, AppConstants.TEXT_RESOURCE_TYPE_STRING, context.packageName)

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
        val exchangeRate = CurrencyUtils.getExchangeRate(context)
        val amountInUSD = CurrencyUtils.convertToUSD(expenseDetails.amount, exchangeRate)

        // Return a new ExpenseDetails instance with the updated amount
        return expenseDetails.copy(amount = amountInUSD)
    }

    fun exportToCsv(context: Context, expenseDetails: List<ExpenseDetails>, categories: List<Category>, currencies: List<Currency>) {
        try {
            val expenseFile = File(context.filesDir, AppConstants.APP_CSV_EXPENSE_DETAILS_FILE_NAME+APP_CSV_FILE_NAME_EXTENSION)
            val categoryFile = File(context.filesDir, AppConstants.APP_CSV_CATEGORIES_DETAILS_FILE_NAME+APP_CSV_FILE_NAME_EXTENSION)
            val currencyFile = File(context.filesDir, AppConstants.APP_CSV_CURRENCIES_DETAILS_FILE_NAME+APP_CSV_FILE_NAME_EXTENSION)

            // Export ExpenseDetails to CSV
            val expenseWriter = FileWriter(expenseFile)
            expenseWriter.append("${BundleKeyValues.EXPENSE_DETAILS_KEY_SENDER_NAME}," +
                    "${BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT_SENDER_NAME}," +
                    "${BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT_RECEIVER_NAME}," +
                    "${BundleKeyValues.NOTIFICATION_KEY_EXPENSE_MESSAGE_DESCRIPTION}," +
                    "${BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT}," +
                    "${BundleKeyValues.NOTIFICATION_KEY_EXPENSE_IS_INCOME}," +
                    "${BundleKeyValues.EXPENSE_DETAILS_KEY_CATEGORY_ID}," +
                    "${BundleKeyValues.EXPENSE_DETAILS_KEY_EXPENSE_ID}," +
                    "${BundleKeyValues.EXPENSE_DETAILS_KEY_EXPENSE_ADDED_DATE}\n")
            expenseDetails.forEach {
                expenseWriter.append("${it.expenseSenderName},${it.expenseMessageSenderName},${it.expenseReceiverName},${it.expenseDescription},${it.amount},${it.isIncome},${it.categoryId},${it.expenseID},${it.expenseAddedDate}\n")
            }
            expenseWriter.flush()
            expenseWriter.close()

            // Export Categories to CSV
            val categoryWriter = FileWriter(categoryFile)
            categoryWriter.append("${BundleKeyValues.EXPENSE_DETAILS_KEY_CATEGORY_FILE_ID}," +
                    "${BundleKeyValues.EXPENSE_DETAILS_KEY_CATEGORY_FILE_NAME}," +
                    "${BundleKeyValues.EXPENSE_DETAILS_KEY_CATEGORY_FILE_TYPE}," +
                    "${BundleKeyValues.EXPENSE_DETAILS_KEY_CATEGORY_FILE_ICON_ID}\n")
            categories.forEach {
                categoryWriter.append("${it.id},${it.name},${it.type},${it.iconResId}\n")
            }
            categoryWriter.flush()
            categoryWriter.close()

            // Export Currencies to CSV
            val currencyWriter = FileWriter(currencyFile)
            currencyWriter.append("${BundleKeyValues.EXPENSE_DETAILS_KEY_CURRENCY_FILE_ID}," +
                    "${BundleKeyValues.EXPENSE_DETAILS_KEY_CURRENCY_FILE_CODE}," +
                    "${BundleKeyValues.EXPENSE_DETAILS_KEY_CURRENCY_FILE_NAME}," +
                    "${BundleKeyValues.EXPENSE_DETAILS_KEY_CURRENCY_FILE_SYMBOL}\n")
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
            val expenseFile = File(context.filesDir, AppConstants.APP_CSV_EXPENSE_DETAILS_FILE_NAME+APP_CSV_FILE_NAME_EXTENSION)
            val categoryFile = File(context.filesDir, AppConstants.APP_CSV_CATEGORIES_DETAILS_FILE_NAME+APP_CSV_FILE_NAME_EXTENSION)
            val currencyFile = File(context.filesDir, AppConstants.APP_CSV_CURRENCIES_DETAILS_FILE_NAME+APP_CSV_FILE_NAME_EXTENSION)

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
                AppConstants.APP_FILE_EXPENSES_KEY_NAME to expenseDetails,
                AppConstants.APP_FILE_CATEGORIES_KEY_NAME to categories,
                AppConstants.APP_FILE_CURRENCIES_KEY_NAME to currencies
            )
            val json = Gson().toJson(jsonData)
            val file = File(context.filesDir, AppConstants.APP_JSON_DATA_FILE_NAME)
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
            val file = File(context.filesDir,  AppConstants.APP_JSON_DATA_FILE_NAME)
            val json = file.readText()
            val jsonData = Gson().fromJson(json, Map::class.java)

            val expensesJson = jsonData[AppConstants.APP_FILE_EXPENSES_KEY_NAME] as List<Map<String, Any>>
            expensesJson.forEach {
                expenseDetails.add(
                    ExpenseDetails(
                        expenseSenderName = it[BundleKeyValues.EXPENSE_DETAILS_KEY_SENDER_NAME] as String,
                        expenseMessageSenderName = it[BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT_SENDER_NAME] as String,
                        expenseReceiverName = it[BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT_RECEIVER_NAME] as String,
                        expenseDescription = it[BundleKeyValues.NOTIFICATION_KEY_EXPENSE_MESSAGE_DESCRIPTION] as String,
                        amount = (it[BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT] as Double),
                        isIncome = it[BundleKeyValues.NOTIFICATION_KEY_EXPENSE_IS_INCOME] as Boolean,
                        categoryId = (it[BundleKeyValues.EXPENSE_DETAILS_KEY_CATEGORY_ID] as Double).toInt(),
                        expenseID = (it[BundleKeyValues.EXPENSE_DETAILS_KEY_EXPENSE_ID] as Double).toInt(),
                        expenseAddedDate = (it[BundleKeyValues.EXPENSE_DETAILS_KEY_EXPENSE_ADDED_DATE] as Double).toLong()
                    )
                )
            }

            val categoriesJson = jsonData[AppConstants.APP_FILE_CATEGORIES_KEY_NAME] as List<Map<String, Any>>
            categoriesJson.forEach {
                categories.add(Category(it[BundleKeyValues.EXPENSE_DETAILS_KEY_CATEGORY_FILE_ID] as Int,
                    it[BundleKeyValues.EXPENSE_DETAILS_KEY_CATEGORY_FILE_NAME] as String,
                    it[BundleKeyValues.EXPENSE_DETAILS_KEY_CATEGORY_FILE_TYPE] as String,
                    it[BundleKeyValues.EXPENSE_DETAILS_KEY_CATEGORY_FILE_ICON_ID] as Int))
            }

            val currenciesJson = jsonData[AppConstants.APP_FILE_CURRENCIES_KEY_NAME] as List<Map<String, Any>>
            currenciesJson.forEach {
                currencies.add(Currency(it[BundleKeyValues.EXPENSE_DETAILS_KEY_CURRENCY_FILE_ID] as Int,
                    it[BundleKeyValues.EXPENSE_DETAILS_KEY_CURRENCY_FILE_CODE] as Double,
                    it[BundleKeyValues.EXPENSE_DETAILS_KEY_CURRENCY_FILE_NAME] as String,
                    it[BundleKeyValues.EXPENSE_DETAILS_KEY_CURRENCY_FILE_SYMBOL] as String))
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
        return if (file.exists()) file.readText() else AppConstants.EMPTY_STRING
    }

    fun loadImageIntoView(imageView: ImageView, source: Any, context: Context, isCircular: Boolean = false) {
        try {
            val bitmap = when (source) {
                is Uri -> {
                    // Load from URI
                    val inputStream = context.contentResolver.openInputStream(source)
                    BitmapFactory.decodeStream(inputStream)
                }
                is Int -> {
                    // Load from drawable resource ID
                    BitmapFactory.decodeResource(context.resources, source)
                }
                else -> throw IllegalArgumentException("Unsupported image source type")
            }

            // Apply circular cropping if needed
            val finalBitmap = if (isCircular) {
                cropBitmapToCircleWithBorder(bitmap)
            } else {
                bitmap
            }

            // Set the bitmap into the ImageView
            imageView.setImageBitmap(finalBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ImageLoader", "Error loading image: ${e.localizedMessage}")
        }
    }

    // Helper function to crop a bitmap into a circle with a white border
    private fun cropBitmapToCircleWithBorder(bitmap: Bitmap, borderWidth: Float = 8f): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw the white border
        val radius = size / 2f
        paint.color = Color.WHITE
        canvas.drawCircle(radius, radius, radius, paint)

        // Draw the circular bitmap
        val innerRadius = radius - borderWidth
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawCircle(radius, radius, innerRadius, paint)
        paint.xfermode = null
        canvas.drawBitmap(
            bitmap,
            Rect(0, 0, bitmap.width, bitmap.height),
            RectF(borderWidth, borderWidth, size - borderWidth, size - borderWidth),
            paint
        )

        return output
    }


    fun getDefaultProfileImages(context: Context, resources: Int): List<Int> {
        // Retrieve the string-array (this should contain only the resource names, not the full path)
        val drawableNames = context.resources.getStringArray(resources)

        // Resolve drawable resource IDs by stripping the 'res/drawable/' part
        val resolvedResources = drawableNames.map { drawableName ->
            val resourceId = context.resources.getIdentifier(drawableName, AppConstants.TEXT_RESOURCE_TYPE_DRAWABLE, context.packageName)
            resourceId
        }

        return resolvedResources
    }

    fun determineCardType(cardNumber: String): String {
        return when {
            cardNumber.startsWith("4") -> "Visa"  // Visa cards typically start with '4'
            cardNumber.startsWith("5") -> "MasterCard"  // MasterCard cards typically start with '5'
            cardNumber.startsWith("3") && (cardNumber[1] == '4' || cardNumber[1] == '7') -> "American Express"  // American Express starts with '34' or '37'
            cardNumber.startsWith("6") -> "Discover"  // Discover cards typically start with '6'
            cardNumber.startsWith("35") -> "JCB"  // JCB cards typically start with '35'
            cardNumber.startsWith("2") -> "UnionPay"  // UnionPay cards typically start with '2'
            else -> "Test Card"  // For other cases
        }
    }

    fun updateDate(textView: TextView, timestamp: Long) {
        val currentTime = System.currentTimeMillis()
        val differenceInMillis = currentTime - timestamp
        val oneMinute = 60 * 1000
        val oneHour = 60 * oneMinute
        val oneDay = 24 * oneHour

        val formattedTime: String = when {
            differenceInMillis < 0 -> {
                val futureDifference = -differenceInMillis
                when {
                    futureDifference < oneMinute -> "In ${futureDifference / 1000} second${if (futureDifference / 1000 == 1L) "" else "s"}"
                    futureDifference < oneHour -> "In ${futureDifference / oneMinute} minute${if (futureDifference / oneMinute == 1L) "" else "s"}"
                    futureDifference < oneDay -> "In ${futureDifference / oneHour} hour${if (futureDifference / oneHour == 1L) "" else "s"}"
                    else -> "In the future (${SimpleDateFormat(AppConstants.DATE_TIME_FORMAT_STRING, Locale.getDefault()).format(Date(timestamp))})"
                }
            }
            differenceInMillis < oneMinute -> "${differenceInMillis / 1000} second${if (differenceInMillis / 1000 == 1L) "" else "s"} ago"
            differenceInMillis < oneHour -> "${differenceInMillis / oneMinute} minute${if (differenceInMillis / oneMinute == 1L) "" else "s"} ago"
            differenceInMillis < oneDay -> "${differenceInMillis / oneHour} hour${if (differenceInMillis / oneHour == 1L) "" else "s"} ago"
            isToday(timestamp) -> "Today"
            isYesterday(timestamp) -> "Yesterday"
            else -> SimpleDateFormat(AppConstants.DATE_TIME_FORMAT_STRING, Locale.getDefault()).format(Date(timestamp))
        }

        textView.text = formattedTime

        if (differenceInMillis in 0..<oneDay) {
            val delay = when {
                differenceInMillis < oneMinute -> 1000L
                differenceInMillis < oneHour -> oneMinute.toLong()
                else -> oneHour.toLong()
            }
            textView.postDelayed({
                updateDate(textView, timestamp)
            }, delay)
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        val currentCalendar = Calendar.getInstance()
        val eventCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return currentCalendar.get(Calendar.YEAR) == eventCalendar.get(Calendar.YEAR) &&
                currentCalendar.get(Calendar.DAY_OF_YEAR) == eventCalendar.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(timestamp: Long): Boolean {
        val currentCalendar = Calendar.getInstance()
        val eventCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return currentCalendar.get(Calendar.YEAR) == eventCalendar.get(Calendar.YEAR) &&
                currentCalendar.get(Calendar.DAY_OF_YEAR) - eventCalendar.get(Calendar.DAY_OF_YEAR) == 1
    }

    fun shareAppUrl(context: Context) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, AppConstants.ANDROID_APP_URL)
            type = AppConstants.APP_LINK_SHARE_FORMAT
        }
        context.startActivity(Intent.createChooser(shareIntent, context.resources.getString(R.string.text_share_app_url)))
    }

    fun getCurrentTimeWelcomeMessage(context: Context): String{
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (currentHour) {
            in 0..11 -> context.resources.getString(R.string.greeting_morning)
            in 12..17 -> context.resources.getString(R.string.greeting_afternoon)
            else -> context.resources.getString(R.string.greeting_evening)
        }

        return greeting
    }

    fun prepareDayChartData(expenseData: List<DailyExpenseWithTime>): List<Pair<String, Int>> =
        expenseData.map { expense ->
            val formattedTime = SimpleDateFormat("hh a", Locale.getDefault()).format(Date(expense.time ?: 0L))
            formattedTime to (expense.amount ?: 0)
        }

    fun prepareWeekChartData(expenseData: List<WeeklyExpenseSum>): List<Pair<String, Int>> =
        expenseData.map { expense ->
            val dayInMillis = expense.day ?: 0L
            val dateFormat = SimpleDateFormat("d", Locale.getDefault())

            val dayFormatted = dateFormat.format(Date(dayInMillis))
            dayFormatted to (expense.sum ?: 0)
        }

    fun prepareYearChartData(expenseData: List<MonthlyExpenseWithTime>): List<Pair<String, Int>> =
        expenseData.map { expense ->
            val formattedMonth = SimpleDateFormat("MMM yy", Locale.getDefault()).format(Date(expense.time ?: 0L))
            formattedMonth to (expense.amount ?: 0)
        }

    fun convertDayToWeekOfMonth(data: List<WeeklyExpenseSum>): List<Pair<String, Int>> =
        data.map {
            val calendar = Calendar.getInstance().apply { timeInMillis = it.day ?: 0L }
            val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH)
            val suffix = when (weekOfMonth) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
            "${weekOfMonth}${suffix} wk" to (it.sum ?: 0)
        }

    fun Any.isIncome(): Boolean {
        return when (this) {
            is DailyExpenseWithTime -> this.isIncome
            is WeeklyExpenseSum -> this.isIncome
            is WeeklyExpenseWithTime -> this.isIncome
            is MonthlyExpenseWithTime -> this.isIncome
            else -> throw IllegalArgumentException("Unknown data type: ${this::class.java.simpleName}")
        }
    }
}
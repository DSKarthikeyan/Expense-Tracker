package com.dsk.myexpense.expense_module.util

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.core.ExpenseApplication.Companion.getSettingsRepository
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.ui.view.settings.SettingsRepository
import java.io.ByteArrayOutputStream
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

//    fun makeStatusBarTransparent(activity: Activity){
//        val decor = activity.window.decorView
//        decor.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
//        val w = activity.window
//        w.statusBarColor = Color.TRANSPARENT
//    }

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
}
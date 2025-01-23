package com.dsk.myexpense.expense_module.data.source.local.sharedPref

import android.content.Context
import com.dsk.myexpense.expense_module.data.model.User
import com.dsk.myexpense.expense_module.util.AppConstants

class SharedPreferencesManager(context: Context) {

    private val sharedPreferences =
        context.getSharedPreferences(AppConstants.USER_SHARED_PREF_NAME, Context.MODE_PRIVATE)

    fun saveUser(name: String, profilePicture: String) {
        sharedPreferences.edit().apply {
            putString(AppConstants.USER_SHARED_PREF_USER_DETAILS_NAME, name)
            putString(AppConstants.USER_SHARED_PREF_USER_PROFILE_PICTURE_NAME, profilePicture)
            apply()
        }
    }

    fun getUser(): User? {
        val name = sharedPreferences.getString(AppConstants.USER_SHARED_PREF_USER_DETAILS_NAME, null)
        val profilePicture =
            sharedPreferences.getString(AppConstants.USER_SHARED_PREF_USER_PROFILE_PICTURE_NAME, null)

        return if (name != null && profilePicture != null) {
            User(name = name, profilePicture = profilePicture)
        } else {
            null
        }
    }

    fun deleteUser() {
        sharedPreferences.edit().clear().apply()
    }
}

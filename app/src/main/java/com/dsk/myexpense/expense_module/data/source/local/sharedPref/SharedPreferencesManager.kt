package com.dsk.myexpense.expense_module.data.source.local.sharedPref

import android.content.Context
import com.dsk.myexpense.expense_module.data.model.User

class SharedPreferencesManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveUser(name: String, profilePicture: String) {
        sharedPreferences.edit().apply {
            putString("user_name", name)
            putString("user_profile_picture", profilePicture)
            apply()
        }
    }

    fun getUser(): User? {
        val name = sharedPreferences.getString("user_name", null)
        val profilePicture = sharedPreferences.getString("user_profile_picture", null)

        return if (name != null && profilePicture != null) {
            User(
                name = name, profilePicture = profilePicture
            )
        } else {
            null
        }
    }

    fun deleteUser() {
        sharedPreferences.edit().clear().apply()
    }
}

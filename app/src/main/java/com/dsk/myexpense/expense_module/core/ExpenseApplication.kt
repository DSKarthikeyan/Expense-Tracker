package com.dsk.myexpense.expense_module.core

import android.app.*
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.dsk.myexpense.expense_module.data.repository.ExpenseRepository
import com.dsk.myexpense.expense_module.data.source.local.ExpenseTrackerDB
import com.dsk.myexpense.expense_module.data.source.network.CurrencyAPIService
import com.dsk.myexpense.expense_module.ui.view.settings.SettingsDataStore
import com.dsk.myexpense.expense_module.ui.view.settings.SettingsRepository
import com.dsk.myexpense.expense_module.util.SmsJobService
import java.lang.ref.WeakReference

class ExpenseApplication : Application() {

    // Database initialization
    private val database by lazy { ExpenseTrackerDB.getDatabase(this) }

    // Repository initialization
    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepository(
            database.getExpenseDAO(),
            database.getExpenseTransactionDAO(),
            database.getExpenseCategoryDAO(),
            database.getExpenseCurrencyDAO(),
            CurrencyAPIService
        )
    }

    // Settings Repository initialization
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(SettingsDataStore.getInstance(this))
    }

    // Companion object for singleton access
    companion object {
        private const val JOB_ID_SMS = 1234567890

        // Provide access to the repositories
        fun getExpenseRepository(context: Context): ExpenseRepository {
            return (context.applicationContext as ExpenseApplication).expenseRepository
        }

        fun getSettingsRepository(context: Context): SettingsRepository {
            return (context.applicationContext as ExpenseApplication).settingsRepository
        }
    }

    // Reference to the current activity
    private var currentActivity: WeakReference<Activity?> = WeakReference(null)

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(ActivityLifecycleManager())
        scheduleSmsJob()
    }

    /**
     * Schedules a job service to handle SMS processing.
     */
    private fun scheduleSmsJob() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobInfo = JobInfo.Builder(
                JOB_ID_SMS,
                ComponentName(this, SmsJobService::class.java)
            )
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build()

            jobScheduler.schedule(jobInfo)
        }
    }

    /**
     * Retrieves the currently active activity.
     * @return The current activity or null if none is active.
     */
    fun getCurrentActivity(): Activity? = currentActivity.get()

    /**
     * Activity lifecycle callback manager to track the current activity.
     */
    private inner class ActivityLifecycleManager : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {
            currentActivity = WeakReference(activity)
        }

        override fun onActivityResumed(activity: Activity) {
            currentActivity = WeakReference(activity)
        }

        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {
            // Clear the reference if the activity stops
            if (currentActivity.get() === activity) {
                currentActivity.clear()
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {
            if (currentActivity.get() === activity) {
                currentActivity.clear()
            }
        }
    }
}

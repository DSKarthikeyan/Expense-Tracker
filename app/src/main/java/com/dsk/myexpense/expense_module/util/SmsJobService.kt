package com.dsk.myexpense.expense_module.util

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent

class SmsJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        // Perform the background task
        val intent = Intent(this, SmsReceiver::class.java)
        // Sending result back through a broadcast or internal notification.
        sendBroadcast(intent)

        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }
}

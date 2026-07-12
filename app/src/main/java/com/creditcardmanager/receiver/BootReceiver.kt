package com.creditcardmanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.creditcardmanager.utils.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            reminderScheduler.scheduleDailyReminders()
        }
    }
}

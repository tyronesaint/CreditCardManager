package com.creditcardmanager.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettings @Inject constructor(@ApplicationContext context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "credit_card_manager_settings"

        // Payment reminder settings
        private const val KEY_PAYMENT_DAYS_AHEAD = "payment_days_ahead"
        private const val KEY_PAYMENT_REMINDER_TIMES = "payment_reminder_times"
        private const val KEY_PAYMENT_REMINDER_HOUR = "payment_reminder_hour"
        private const val KEY_PAYMENT_REMINDER_MINUTE = "payment_reminder_minute"

        // Activity reminder settings
        private const val KEY_ACTIVITY_REMINDER_ENABLED = "activity_reminder_enabled"
        private const val KEY_ACTIVITY_REMINDER_HOUR = "activity_reminder_hour"

        // Default values
        private const val DEFAULT_PAYMENT_DAYS_AHEAD = 7
        private const val DEFAULT_REMINDER_TIMES = 3 // 3 days, 1 day, same day
        private const val DEFAULT_REMINDER_HOUR = 10
        private const val DEFAULT_REMINDER_MINUTE = 0
    }

    // Payment reminder: how many days before due date to show in dashboard
    var paymentDaysAhead: Int
        get() = prefs.getInt(KEY_PAYMENT_DAYS_AHEAD, DEFAULT_PAYMENT_DAYS_AHEAD)
        set(value) = prefs.edit().putInt(KEY_PAYMENT_DAYS_AHEAD, value).apply()

    // How many reminder push notifications (3 = 3days, 1day, same day)
    var paymentReminderTimes: Int
        get() = prefs.getInt(KEY_PAYMENT_REMINDER_TIMES, DEFAULT_REMINDER_TIMES)
        set(value) = prefs.edit().putInt(KEY_PAYMENT_REMINDER_TIMES, value).apply()

    // Reminder time (hour)
    var paymentReminderHour: Int
        get() = prefs.getInt(KEY_PAYMENT_REMINDER_HOUR, DEFAULT_REMINDER_HOUR)
        set(value) = prefs.edit().putInt(KEY_PAYMENT_REMINDER_HOUR, value).apply()

    // Reminder time (minute)
    var paymentReminderMinute: Int
        get() = prefs.getInt(KEY_PAYMENT_REMINDER_MINUTE, DEFAULT_REMINDER_MINUTE)
        set(value) = prefs.edit().putInt(KEY_PAYMENT_REMINDER_MINUTE, value).apply()

    // Activity claim reminder
    var activityReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_ACTIVITY_REMINDER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ACTIVITY_REMINDER_ENABLED, value).apply()

    var activityReminderHour: Int
        get() = prefs.getInt(KEY_ACTIVITY_REMINDER_HOUR, DEFAULT_REMINDER_HOUR)
        set(value) = prefs.edit().putInt(KEY_ACTIVITY_REMINDER_HOUR, value).apply()

    // Get reminder days based on paymentReminderTimes setting
    fun getReminderDaysBefore(): List<Int> {
        return when (paymentReminderTimes) {
            1 -> listOf(0) // Same day only
            2 -> listOf(1, 0) // 1 day + same day
            3 -> listOf(3, 1, 0) // 3 days, 1 day, same day
            4 -> listOf(7, 3, 1, 0) // 7, 3, 1, same day
            else -> listOf(3, 1, 0)
        }
    }

    // Get reminder time as string "HH:mm"
    fun getReminderTimeString(): String {
        return String.format("%02d:%02d", paymentReminderHour, paymentReminderMinute)
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}

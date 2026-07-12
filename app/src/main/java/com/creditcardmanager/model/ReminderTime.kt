package com.creditcardmanager.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReminderTime(
    val offsetDays: Int = 0,
    val timeOfDay: String = "09:00"
) : Parcelable {
    fun getDisplayText(): String {
        return when {
            offsetDays == 0 -> "当天 $timeOfDay"
            offsetDays == 1 -> "提前1天 $timeOfDay"
            offsetDays > 1 -> "提前${offsetDays}天 $timeOfDay"
            else -> "$timeOfDay"
        }
    }
}

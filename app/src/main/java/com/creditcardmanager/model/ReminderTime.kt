package com.creditcardmanager.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReminderTime(
    val offsetDays: Int = 0,
    val timeOfDay: String = "09:00"
) : Parcelable

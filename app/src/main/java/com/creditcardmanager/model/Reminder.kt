package com.creditcardmanager.model

import android.os.Parcelable
import com.creditcardmanager.model.enums.SourceType
import kotlinx.parcelize.Parcelize

@Parcelize
data class Reminder(
    val id: String,
    val sourceType: SourceType,
    val sourceId: String? = null,
    val title: String,
    val remindTimes: List<ReminderTime> = emptyList(),
    val enabled: Boolean = true,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

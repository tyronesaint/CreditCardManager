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
    val repeatType: ReminderRepeatType = ReminderRepeatType.ONCE,
    val repeatValue: String? = null,  // 具体值：几号/周几/每月几次
    val enabled: Boolean = true,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
enum class ReminderRepeatType : Parcelable {
    ONCE,           // 一次性
    DAILY,          // 每天
    WEEKLY,         // 每周（repeatValue: 1=周一, 2=周二...7=周日）
    MONTHLY_DATE,   // 每月几号（repeatValue: "15" = 每月15号）
    MONTHLY_COUNT   // 每月几次（repeatValue: "3" = 每月3次）
}

package com.creditcardmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.creditcardmanager.model.enums.SourceType

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val sourceType: SourceType,
    val sourceId: String? = null,
    val title: String,
    val remindTimesJson: String,
    val enabled: Boolean = true,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

package com.creditcardmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_progress")
data class ActivityProgressEntity(
    @PrimaryKey val activityId: String,
    val periodKey: String,
    val currentAmount: Double = 0.0,
    val currentCount: Int = 0,
    val currentCashback: Double = 0.0,
    val todayCashback: Double = 0.0,
    val isAchieved: Boolean = false,
    val continuousDone: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

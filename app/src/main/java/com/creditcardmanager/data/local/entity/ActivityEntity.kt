package com.creditcardmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.creditcardmanager.model.enums.ActivityLevel
import com.creditcardmanager.model.enums.ActivityType
import com.creditcardmanager.model.enums.PeriodType

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val level: ActivityLevel,
    val bankId: String? = null,
    val cardId: String? = null,
    val type: ActivityType,
    val periodType: PeriodType,
    val description: String? = null,
    val targetAmount: Double? = null,
    val targetCount: Int? = null,
    val minPerAmount: Double? = null,
    val cashbackRate: Double? = null,
    val dailyCap: Double? = null,
    val monthlyCap: Double? = null,
    val withdrawThreshold: Double? = null,
    val requiredPeriods: Int? = null,
    val innerType: String? = null,
    val innerTargetAmount: Double? = null,
    val innerTargetCount: Int? = null,
    val innerMinPerAmount: Double? = null,
    val minAmount: Double? = null,
    val filterJson: String? = null,
    val rewardJson: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

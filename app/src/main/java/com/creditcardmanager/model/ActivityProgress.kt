package com.creditcardmanager.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ActivityProgress(
    val activityId: String,
    val periodKey: String,
    val currentAmount: Double = 0.0,
    val currentCount: Int = 0,
    val currentCashback: Double = 0.0,
    val todayCashback: Double = 0.0,
    val isAchieved: Boolean = false,
    val continuousDone: Int = 0,
    val manualBaseline: Double? = null,
    val baselineSource: String? = null,
    val manualSince: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable

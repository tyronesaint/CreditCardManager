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
    val updatedAt: Long = System.currentTimeMillis(),
    // ↓↓↓ 新增3个字段（手动调整用，默认null不影响旧数据）
    val manualBaseline: Double? = null,      // 手动设置的基准值（查账/拍脑袋的数）
    val baselineSource: String? = null,      // "CHECK"=查账/"MANUAL"=手动/"null"=消费累加
    val manualSince: Long? = null            // 手动调整的时间戳（毫秒），之后新消费从这天算
)

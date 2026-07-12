package com.creditcardmanager.model

import android.os.Parcelable
import com.creditcardmanager.model.enums.ClaimOffsetType
import com.creditcardmanager.model.enums.RewardType
import kotlinx.parcelize.Parcelize

@Parcelize
data class ActivityReward(
    val rewardType: RewardType = RewardType.NONE,
    val claimOffsetType: ClaimOffsetType? = null,
    val claimDay: Int? = null,
    val claimTime: String? = null,
    val claimReminderEnabled: Boolean = false
) : Parcelable

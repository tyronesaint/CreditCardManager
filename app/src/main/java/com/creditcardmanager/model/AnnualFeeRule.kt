package com.creditcardmanager.model

import android.os.Parcelable
import com.creditcardmanager.model.enums.AnnualFeeLogic
import kotlinx.parcelize.Parcelize

@Parcelize
data class AnnualFeeRule(
    val logic: AnnualFeeLogic,
    val targetAmount: Double = 0.0,
    val targetCount: Int = 0
) : Parcelable

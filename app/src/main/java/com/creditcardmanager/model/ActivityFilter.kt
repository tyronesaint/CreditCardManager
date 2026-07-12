package com.creditcardmanager.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ActivityFilter(
    val includeTagIds: List<String> = emptyList(),
    val excludeTagIds: List<String> = emptyList(),
    val includeChannels: List<String> = emptyList(),
    val excludeChannels: List<String> = emptyList(),
    val excludeTransactionTypes: List<String> = emptyList(),
    val maxCountPerPeriod: Int = 0
) : Parcelable

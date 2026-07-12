package com.creditcardmanager.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ActivityWithProgress(
    val activity: Activity,
    val progress: ActivityProgress,
    val cardName: String? = null,
    val bankName: String? = null
) : Parcelable

package com.creditcardmanager.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tag(
    val id: String,
    val name: String,
    val color: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

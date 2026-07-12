package com.creditcardmanager.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Bank(
    val id: String,
    val name: String,
    val shortName: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

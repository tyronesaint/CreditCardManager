package com.creditcardmanager.model

import java.time.LocalDate

data class Transaction(
    val id: String,
    val cardId: String,
    val amount: Double,
    val spendDate: LocalDate,
    val tagId: String,
    val channel: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) 

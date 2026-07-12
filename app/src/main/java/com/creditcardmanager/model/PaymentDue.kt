package com.creditcardmanager.model

import java.time.LocalDate

data class PaymentDue(
    val cardId: String,
    val cardName: String,
    val bankShortName: String?,
    val last4: String?,
    val statementAmount: Double,
    val dueDate: LocalDate,
    val daysRemaining: Int
) 

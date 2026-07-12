package com.creditcardmanager.model

import com.creditcardmanager.model.enums.CardStatus
import java.time.LocalDate

data class InterestFreeInfo(
    val cardId: String,
    val cardName: String,
    val bankShortName: String?,
    val last4: String?,
    val status: CardStatus,
    val spendDate: LocalDate,
    val statementDate: LocalDate,
    val dueDate: LocalDate,
    val interestFreeDays: Int
) 

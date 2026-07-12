package com.creditcardmanager.utils

import com.creditcardmanager.model.Card
import com.creditcardmanager.model.InterestFreeInfo
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object InterestFreeCalculator {
    fun calculate(card: Card, spendDate: LocalDate = LocalDate.now()): InterestFreeInfo {
        val statementDate = DateUtils.getStatementDate(card.statementDay, spendDate)
        val dueDate = card.getDueDateForStatement(statementDate)
        val days = ChronoUnit.DAYS.between(spendDate, dueDate).toInt()
        return InterestFreeInfo(cardId = card.id, cardName = card.name, bankShortName = null, last4 = card.last4,
            status = card.status, spendDate = spendDate, statementDate = statementDate, dueDate = dueDate, interestFreeDays = days)
    }
    fun calculateAll(cards: List<Card>, spendDate: LocalDate = LocalDate.now()): List<InterestFreeInfo> {
        return cards.filter { it.status == com.creditcardmanager.model.enums.CardStatus.ACTIVE }
            .map { calculate(it, spendDate) }.sortedByDescending { it.interestFreeDays }
    }
}

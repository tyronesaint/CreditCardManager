package com.creditcardmanager.model

import com.creditcardmanager.model.enums.CardStatus
import com.creditcardmanager.model.enums.DueDayType
import java.time.LocalDate

data class Card(
    val id: String,
    val bankId: String,
    val name: String,
    val last4: String? = null,
    val creditLimit: Double? = null,
    val status: CardStatus = CardStatus.ACTIVE,
    val statementDay: Int,
    val dueDayType: DueDayType = DueDayType.FIXED_DATE,
    val dueDay: Int? = null,
    val dueIntervalDays: Int? = null,
    val annualFeeEnabled: Boolean = false,
    val annualFeeStartDate: LocalDate? = null,
    val annualFeeRule: AnnualFeeRule? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getDisplayName(): String = if (last4 != null) "$name (*$last4)" else name
    fun getDueDateForStatement(statementDate: LocalDate): LocalDate {
        return when (dueDayType) {
            DueDayType.FIXED_DATE -> {
                val due = statementDate.withDayOfMonth(dueDay ?: 1)
                if (due.isBefore(statementDate) || due.isEqual(statementDate)) due.plusMonths(1) else due
            }
            DueDayType.FIXED_INTERVAL -> statementDate.plusDays((dueIntervalDays ?: 20).toLong())
        }
    }
}

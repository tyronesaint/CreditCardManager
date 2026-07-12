package com.creditcardmanager.utils

import com.creditcardmanager.model.enums.PeriodType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object DateUtils {
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val displayFormatter = DateTimeFormatter.ofPattern("MM月dd日")
    private val periodFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private val weekFormatter = DateTimeFormatter.ofPattern("yyyy-'W'ww")
    private val quarterFormatter = DateTimeFormatter.ofPattern("yyyy-'Q'Q")

    fun formatDate(date: LocalDate): String = date.format(displayFormatter)
    fun formatIso(date: LocalDate): String = date.format(isoFormatter)

    fun getPeriodKey(periodType: PeriodType, date: LocalDate = LocalDate.now()): String {
        return when (periodType) {
            PeriodType.NATURAL_DAY -> date.format(isoFormatter)
            PeriodType.NATURAL_WEEK -> date.format(weekFormatter)
            PeriodType.NATURAL_MONTH -> date.format(periodFormatter)
            PeriodType.NATURAL_QUARTER -> date.format(quarterFormatter)
            else -> date.format(periodFormatter)
        }
    }

    fun getPeriodStart(periodType: PeriodType, date: LocalDate = LocalDate.now()): LocalDate {
        return when (periodType) {
            PeriodType.NATURAL_DAY -> date
            PeriodType.NATURAL_WEEK -> date.with(DayOfWeek.MONDAY)
            PeriodType.NATURAL_MONTH -> date.withDayOfMonth(1)
            PeriodType.NATURAL_QUARTER -> date.withMonth(((date.monthValue - 1) / 3) * 3 + 1).withDayOfMonth(1)
            else -> date.withDayOfMonth(1)
        }
    }

    fun getPeriodEnd(periodType: PeriodType, date: LocalDate = LocalDate.now()): LocalDate {
        return when (periodType) {
            PeriodType.NATURAL_DAY -> date
            PeriodType.NATURAL_WEEK -> date.with(DayOfWeek.SUNDAY)
            PeriodType.NATURAL_MONTH -> YearMonth.from(date).atEndOfMonth()
            PeriodType.NATURAL_QUARTER -> {
                val quarterEndMonth = ((date.monthValue - 1) / 3) * 3 + 3
                YearMonth.of(date.year, quarterEndMonth).atEndOfMonth()
            }
            else -> YearMonth.from(date).atEndOfMonth()
        }
    }

    fun getStatementDate(statementDay: Int, spendDate: LocalDate): LocalDate {
        val currentMonthStatement = spendDate.withDayOfMonth(statementDay)
        return if (spendDate.isAfter(currentMonthStatement) || spendDate.isEqual(currentMonthStatement)) {
            currentMonthStatement.plusMonths(1)
        } else currentMonthStatement
    }

    fun getStatementPeriod(statementDay: Int, date: LocalDate): Pair<LocalDate, LocalDate> {
        val currentStatement = date.withDayOfMonth(statementDay)
        return if (date.dayOfMonth <= statementDay) {
            Pair(currentStatement.minusMonths(1).plusDays(1), currentStatement)
        } else {
            Pair(currentStatement.plusDays(1), currentStatement.plusMonths(1))
        }
    }
}

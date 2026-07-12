package com.creditcardmanager.utils

import com.creditcardmanager.model.Activity
import com.creditcardmanager.model.ActivityProgress
import com.creditcardmanager.model.Transaction
import com.creditcardmanager.model.enums.ActivityType
import java.time.LocalDate

object ActivityCalculator {
    fun calculateProgress(activity: Activity, transactions: List<Transaction>, existingProgress: ActivityProgress? = null): ActivityProgress {
        val periodKey = DateUtils.getPeriodKey(activity.periodType)
        val today = LocalDate.now()
        var currentAmount = 0.0
        var currentCount = 0
        var currentCashback = 0.0
        var todayCashback = 0.0
        var isAchieved = false
        var continuousDone = existingProgress?.continuousDone ?: 0
        val filteredTransactions = transactions.filter { ActivityMatcher.matchTransaction(it, activity, null) }

        when (activity.type) {
            ActivityType.AMOUNT_TARGET -> {
                currentAmount = filteredTransactions.sumOf { it.amount }
                isAchieved = activity.targetAmount != null && currentAmount >= activity.targetAmount
            }
            ActivityType.COUNT_TARGET -> {
                currentCount = filteredTransactions.size
                isAchieved = activity.targetCount != null && currentCount >= activity.targetCount
            }
            ActivityType.CASHBACK_RATE -> {
                val rate = activity.cashbackRate ?: 0.0
                val dailyCap = activity.dailyCap ?: 0.0
                val monthlyCap = activity.monthlyCap ?: 0.0
                val todayTransactions = filteredTransactions.filter { it.spendDate == today }
                val monthTransactions = filteredTransactions.filter { it.spendDate.year == today.year && it.spendDate.month == today.month }
                var todayTotal = 0.0
                for (t in todayTransactions) {
                    val theoretical = t.amount * rate
                    val remainingDaily = if (dailyCap > 0) dailyCap - todayTotal else theoretical
                    val actual = if (theoretical > remainingDaily && dailyCap > 0) remainingDaily else theoretical
                    todayTotal += actual
                    todayCashback += actual
                }
                var monthTotal = 0.0
                for (t in monthTransactions) {
                    val theoretical = t.amount * rate
                    val remainingMonthly = if (monthlyCap > 0) monthlyCap - monthTotal else theoretical
                    val actual = if (theoretical > remainingMonthly && monthlyCap > 0) remainingMonthly else theoretical
                    monthTotal += actual
                }
                currentCashback = monthTotal
                isAchieved = if (monthlyCap > 0) currentCashback >= monthlyCap else false
            }
            ActivityType.CONTINUOUS_PERIOD -> {
                val innerAchieved = when (activity.innerType) {
                    "amount_target" -> {
                        currentAmount = filteredTransactions.sumOf { it.amount }
                        activity.innerTargetAmount != null && currentAmount >= activity.innerTargetAmount
                    }
                    "count_target" -> {
                        currentCount = filteredTransactions.size
                        activity.innerTargetCount != null && currentCount >= activity.innerTargetCount
                    }
                    else -> false
                }
                if (innerAchieved) continuousDone = (existingProgress?.continuousDone ?: 0) + 1
                else continuousDone = 0
                isAchieved = activity.requiredPeriods != null && continuousDone >= activity.requiredPeriods
            }
            ActivityType.FIRST_SPEND -> {
                isAchieved = filteredTransactions.isNotEmpty()
                currentCount = if (isAchieved) 1 else 0
            }
            ActivityType.CHECKIN_DAILY -> {
                isAchieved = existingProgress?.isAchieved ?: false
                currentCount = existingProgress?.currentCount ?: 0
            }
        }
        return ActivityProgress(activityId = activity.id, periodKey = periodKey, currentAmount = currentAmount,
            currentCount = currentCount, currentCashback = currentCashback, todayCashback = todayCashback,
            isAchieved = isAchieved, continuousDone = continuousDone)
    }

    fun getCashbackPreview(activity: Activity, transaction: Transaction): Double? {
        if (activity.type != ActivityType.CASHBACK_RATE) return null
        if (!ActivityMatcher.matchTransaction(transaction, activity, null)) return null
        val rate = activity.cashbackRate ?: return null
        return transaction.amount * rate
    }
}

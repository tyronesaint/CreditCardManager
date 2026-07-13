package com.creditcardmanager.utils

import com.creditcardmanager.model.Activity
import com.creditcardmanager.model.ActivityProgress
import com.creditcardmanager.model.Transaction
import com.creditcardmanager.model.enums.ActivityType
import java.time.LocalDate

object ActivityCalculator {
    /**
     * @param periodKeyOverride BIND_STATEMENT时必须传账单周期的periodKey，否则抛异常
     */
    fun calculateProgress(
        activity: Activity,
        transactions: List<Transaction>,
        existingProgress: ActivityProgress? = null,
        periodKeyOverride: String? = null
    ): ActivityProgress {
        // 1. 确定periodKey（BIND_STATEMENT必须用调用侧传的，避免之前的坑）
        val periodKey = if (activity.periodType == com.creditcardmanager.model.enums.PeriodType.BIND_STATEMENT) {
            periodKeyOverride ?: throw IllegalArgumentException("BIND_STATEMENT活动必须传periodKeyOverride")
        } else {
            DateUtils.getPeriodKey(activity.periodType)
        }

        // 2. 提取手动调整的基准值（核心改动：旧消费不参与计算）
        val manualBaseline = existingProgress?.manualBaseline ?: 0.0
        val manualSince = existingProgress?.manualSince ?: 0L
        val today = LocalDate.now()

        // 3. 只累加【手动调整之后】的新消费（旧消费直接排除，符合你"可有可无"的语义）
        val validTransactions = transactions.filter { txn ->
            // 没手动调整过：全累加；调整过：只累加调整时间之后的消费
            manualSince == 0L || txn.spendDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli() >= manualSince
        }

        // 4. 原有计算逻辑保留，只改累加的基础值
        var currentAmount = manualBaseline
        var currentCount = existingProgress?.currentCount ?: 0
        var currentCashback = 0.0
        var todayCashback = 0.0
        var isAchieved = false
        var continuousDone = existingProgress?.continuousDone ?: 0

        val filteredTransactions = validTransactions.filter {
            ActivityMatcher.matchTransaction(it, activity, null)
        }

        when (activity.type) {
            ActivityType.AMOUNT_TARGET -> {
                currentAmount += filteredTransactions.sumOf { it.amount }
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
                val monthTransactions = filteredTransactions.filter {
                    it.spendDate.year == today.year && it.spendDate.month == today.month
                }
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
                // TODO: 跨期逻辑暂不改动（结构性坑，动的话容易炸，后续再优化）
                val innerAchieved = when (activity.innerType) {
                    "amount_target" -> {
                        currentAmount += filteredTransactions.sumOf { it.amount }
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
            ActivityType.CONSECUTIVE_DAYS -> {
                val spendDates = filteredTransactions.map { it.spendDate }.distinct().sorted()
                var maxConsecutive = 0
                var currentConsecutive = 0
                var prevDate: LocalDate? = null
                for (date in spendDates) {
                    if (prevDate != null && date.minusDays(1) == prevDate) {
                        currentConsecutive++
                    } else {
                        currentConsecutive = 1
                    }
                    maxConsecutive = maxOf(maxConsecutive, currentConsecutive)
                    prevDate = date
                }
                currentCount = maxConsecutive
                isAchieved = activity.targetCount != null && maxConsecutive >= activity.targetCount
            }
            ActivityType.WEEKLY_CLAIM -> {
                val thisWeekTransactions = filteredTransactions.filter {
                    it.spendDate >= DateUtils.getPeriodStart(com.creditcardmanager.model.enums.PeriodType.NATURAL_WEEK, today)
                }
                isAchieved = thisWeekTransactions.isNotEmpty()
                currentCount = if (isAchieved) 1 else 0
            }
        }
        return ActivityProgress(
            activityId = activity.id,
            periodKey = periodKey,
            currentAmount = currentAmount,
            currentCount = currentCount,
            currentCashback = currentCashback,
            todayCashback = todayCashback,
            isAchieved = isAchieved,
            continuousDone = continuousDone
        )
    }

    fun getCashbackPreview(activity: Activity, transaction: Transaction): Double? {
        if (activity.type != ActivityType.CASHBACK_RATE) return null
        if (!ActivityMatcher.matchTransaction(transaction, activity, null)) return null
        val rate = activity.cashbackRate ?: return null
        return transaction.amount * rate
    }
}

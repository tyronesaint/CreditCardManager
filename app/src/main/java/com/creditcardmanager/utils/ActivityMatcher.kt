package com.creditcardmanager.utils

import com.creditcardmanager.model.Activity
import com.creditcardmanager.model.Card
import com.creditcardmanager.model.Transaction
import com.creditcardmanager.model.enums.ActivityLevel
import com.creditcardmanager.model.enums.ActivityType

object ActivityMatcher {
    fun matchTransaction(transaction: Transaction, activity: Activity, card: Card? = null): Boolean {
        val scopeMatch = when (activity.level) {
            ActivityLevel.BANK -> {
                // 银行级活动：传入的 card 可能为 null（从 transactions 列表遍历时）
                // 此时只要有 card 参数且 bankId 匹配即可；如果 card 为 null，则尝试用 transaction 的 cardId 兜底
                if (card != null) {
                    card.bankId == activity.bankId
                } else {
                    // 兜底：如果 card 为 null，默认允许通过（后续在 ViewModel 中会通过 bankId 过滤交易）
                    activity.bankId != null
                }
            }
            ActivityLevel.CARD -> transaction.cardId == activity.cardId
        }
        if (!scopeMatch) return false
        val filter = activity.filter
        if (filter.includeTagIds.isNotEmpty() && transaction.tagId !in filter.includeTagIds) return false
        if (transaction.tagId in filter.excludeTagIds) return false
        val channel = transaction.channel
        if (channel != null) {
            if (filter.includeChannels.isNotEmpty() && channel !in filter.includeChannels) return false
            if (channel in filter.excludeChannels) return false
        }
        if (activity.type == ActivityType.COUNT_TARGET && activity.minPerAmount != null) {
            if (activity.minPerAmount > 0 && transaction.amount < activity.minPerAmount) return false
        }
        if (activity.type == ActivityType.FIRST_SPEND && activity.minAmount != null) {
            if (transaction.amount < activity.minAmount) return false
        }
        if (activity.type == ActivityType.CONSECUTIVE_DAYS && activity.minPerAmount != null) {
            if (activity.minPerAmount > 0 && transaction.amount < activity.minPerAmount) return false
        }
        return true
    }

    fun shouldCountInPeriod(activity: Activity, currentCount: Int): Boolean {
        val maxCount = activity.filter.maxCountPerPeriod
        return maxCount <= 0 || currentCount < maxCount
    }
}

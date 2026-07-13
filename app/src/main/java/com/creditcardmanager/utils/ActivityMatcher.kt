package com.creditcardmanager.utils

import com.creditcardmanager.model.Activity
import com.creditcardmanager.model.Card
import com.creditcardmanager.model.Transaction
import com.creditcardmanager.model.enums.ActivityLevel
import com.creditcardmanager.model.enums.ActivityType

object ActivityMatcher {
    fun matchTransaction(transaction: Transaction, activity: Activity, card: Card? = null): Boolean {
        // 上层已经按 scope (bankId/cardId) 过滤了交易，这里只需要做额外过滤
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

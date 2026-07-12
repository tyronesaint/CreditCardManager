package com.creditcardmanager.model.enums

enum class ActivityType {
    AMOUNT_TARGET,      // 金额达标
    COUNT_TARGET,       // 笔数达标
    CASHBACK_RATE,      // 比例返现
    CONTINUOUS_PERIOD,  // 连续达标
    FIRST_SPEND,        // 首刷奖
    CHECKIN_DAILY,      // 每日签到
    CONSECUTIVE_DAYS,   // 连续消费N天（如云闪付无界卡）
    WEEKLY_CLAIM        // 每周固定日期领取（如建行社保卡周三领券）
}

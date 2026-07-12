package com.creditcardmanager.model

import java.time.LocalDateTime

data class UpcomingClaim(
    val activityId: String,
    val activityName: String,
    val claimTime: LocalDateTime,
    val daysUntil: Int
) 

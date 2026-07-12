package com.creditcardmanager.model


data class DashboardData(
    val topCards: List<InterestFreeInfo> = emptyList(),
    val upcomingPayments: List<PaymentDue> = emptyList(),
    val bankActivities: List<ActivityWithProgress> = emptyList(),
    val cardActivities: List<ActivityWithProgress> = emptyList(),
    val upcomingClaims: List<UpcomingClaim> = emptyList()
) 

package com.creditcardmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creditcardmanager.data.repository.*
import com.creditcardmanager.model.*
import com.creditcardmanager.model.enums.ActivityLevel
import com.creditcardmanager.model.enums.CardStatus
import com.creditcardmanager.model.enums.PeriodType
import com.creditcardmanager.utils.ActivityCalculator
import com.creditcardmanager.utils.DateUtils
import com.creditcardmanager.utils.InterestFreeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val bankRepo: BankRepository, private val cardRepo: CardRepository,
    private val activityRepo: ActivityRepository, private val progressRepo: ActivityProgressRepository,
    private val transactionRepo: TransactionRepository, private val reminderRepo: ReminderRepository
) : ViewModel() {
    private val _dashboardData = MutableStateFlow(DashboardData())
    val dashboardData: StateFlow<DashboardData> = _dashboardData.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            val banks = bankRepo.getAllBanks().first()
            val cards = cardRepo.getActiveCards().first()
            val activities = activityRepo.getAllActiveActivities().first()
            val bankMap = banks.associateBy { it.id }
            val today = LocalDate.now()

            val topCards = cards.map { card ->
                val info = InterestFreeCalculator.calculate(card)
                info.copy(bankShortName = bankMap[card.bankId]?.shortName)
            }.sortedByDescending { it.interestFreeDays }.take(3)

            val upcomingPayments = cards.map { card ->
                val statementDate = DateUtils.getStatementDate(card.statementDay, today)
                val dueDate = card.getDueDateForStatement(statementDate)
                val statementPeriod = DateUtils.getStatementPeriod(card.statementDay, today)
                val amount = transactionRepo.getTotalAmountByCardAndDateRange(card.id, statementPeriod.first, statementPeriod.second)
                PaymentDue(cardId = card.id, cardName = card.getDisplayName(), bankShortName = bankMap[card.bankId]?.shortName,
                    last4 = card.last4, statementAmount = amount, dueDate = dueDate,
                    daysRemaining = ChronoUnit.DAYS.between(today, dueDate).toInt())
            }.filter { it.daysRemaining in 0..30 }.sortedBy { it.daysRemaining }.take(3)

            val bankActivities = mutableListOf<ActivityWithProgress>()
            val cardActivities = mutableListOf<ActivityWithProgress>()

            for (activity in activities) {
                val (start, end) = when (activity.periodType) {
                    PeriodType.BIND_STATEMENT -> {
                        val card = cards.find { it.id == activity.cardId }
                        if (card != null) DateUtils.getStatementPeriod(card.statementDay, today)
                        else DateUtils.getPeriodStart(PeriodType.NATURAL_MONTH, today) to DateUtils.getPeriodEnd(PeriodType.NATURAL_MONTH, today)
                    }
                    else -> DateUtils.getPeriodStart(activity.periodType, today) to DateUtils.getPeriodEnd(activity.periodType, today)
                }
                val transactions: List<Transaction> = if (activity.level == ActivityLevel.BANK) {
                    val bankCards = cards.filter { it.bankId == activity.bankId }
                    val lists: List<List<Transaction>> = bankCards.map { transactionRepo.getTransactionsByCardAndDateRange(it.id, start, end).first() }
                    lists.flatten()
                } else {
                    activity.cardId?.let { transactionRepo.getTransactionsByCardAndDateRange(it, start, end).first() } ?: emptyList()
                }
                val progress = ActivityCalculator.calculateProgress(activity, transactions)
                val cardName = activity.cardId?.let { cid -> cards.find { it.id == cid }?.name }
                val bankName = activity.bankId?.let { bid -> bankMap[bid]?.name }
                val awp = ActivityWithProgress(activity, progress, cardName, bankName)
                if (activity.level == ActivityLevel.BANK) bankActivities.add(awp) else cardActivities.add(awp)
            }

            _dashboardData.value = DashboardData(topCards = topCards, upcomingPayments = upcomingPayments,
                bankActivities = bankActivities.take(5), cardActivities = cardActivities.take(5), upcomingClaims = emptyList())
            _isLoading.value = false
        }
    }
}

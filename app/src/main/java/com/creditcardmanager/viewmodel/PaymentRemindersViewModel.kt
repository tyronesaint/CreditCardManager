package com.creditcardmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creditcardmanager.data.repository.BankRepository
import com.creditcardmanager.data.repository.CardRepository
import com.creditcardmanager.data.repository.TransactionRepository
import com.creditcardmanager.model.PaymentDue
import com.creditcardmanager.utils.AppSettings
import com.creditcardmanager.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class PaymentRemindersViewModel @Inject constructor(
    private val bankRepo: BankRepository,
    private val cardRepo: CardRepository,
    private val transactionRepo: TransactionRepository,
    private val appSettings: AppSettings
) : ViewModel() {
    private val _payments = MutableStateFlow<List<PaymentDue>>(emptyList())
    val payments: StateFlow<List<PaymentDue>> = _payments.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { loadPayments() }

    fun loadPayments() {
        viewModelScope.launch {
            _isLoading.value = true
            val banks = bankRepo.getAllBanks().first()
            val cards = cardRepo.getActiveCards().first()
            val bankMap = banks.associateBy { it.id }
            val today = LocalDate.now()
            val paymentDaysAhead = appSettings.paymentDaysAhead

            val allPayments = cards.map { card ->
                val statementDate = DateUtils.getStatementDate(card.statementDay, today)
                val dueDate = card.getDueDateForStatement(statementDate)
                val statementPeriod = DateUtils.getStatementPeriod(card.statementDay, today)
                val amount = transactionRepo.getTotalAmountByCardAndDateRange(card.id, statementPeriod.first, statementPeriod.second)
                PaymentDue(
                    cardId = card.id,
                    cardName = card.getDisplayName(),
                    bankShortName = bankMap[card.bankId]?.shortName,
                    last4 = card.last4,
                    statementAmount = amount,
                    dueDate = dueDate,
                    daysRemaining = ChronoUnit.DAYS.between(today, dueDate).toInt()
                )
            }.sortedBy { it.daysRemaining }

            _payments.value = allPayments
            _isLoading.value = false
        }
    }

    fun markAsPaid(cardId: String) {
        // In a real app, this would create a "paid" transaction or mark in database
        // For now, we just refresh the list
        loadPayments()
    }
}

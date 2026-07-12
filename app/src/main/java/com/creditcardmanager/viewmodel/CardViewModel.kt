package com.creditcardmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creditcardmanager.data.repository.*
import com.creditcardmanager.model.*
import com.creditcardmanager.utils.ActivityCalculator
import com.creditcardmanager.utils.DateUtils
import com.creditcardmanager.utils.InterestFreeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CardViewModel @Inject constructor(
    private val bankRepo: BankRepository, private val cardRepo: CardRepository,
    private val activityRepo: ActivityRepository, private val transactionRepo: TransactionRepository,
    private val progressRepo: ActivityProgressRepository
) : ViewModel() {
    private val _cards = MutableStateFlow<List<Pair<Bank, List<Card>>>>(emptyList())
    val cards: StateFlow<List<Pair<Bank, List<Card>>>> = _cards.asStateFlow()
    private val _selectedCard = MutableStateFlow<CardDetail?>(null)
    val selectedCard: StateFlow<CardDetail?> = _selectedCard.asStateFlow()

    data class CardDetail(val card: Card, val bank: Bank?, val interestFreeInfo: InterestFreeInfo,
        val statementAmount: Double, val annualFeeProgress: AnnualFeeProgress?, val activities: List<ActivityWithProgress>,
        val recentTransactions: List<Transaction>)
    data class AnnualFeeProgress(val currentAmount: Double, val currentCount: Int, val isAchieved: Boolean)

    init { loadCards() }

    fun loadCards() {
        viewModelScope.launch {
            val banks = bankRepo.getAllBanks().first()
            val allCards = cardRepo.getAllCards().first()
            _cards.value = banks.map { bank -> bank to allCards.filter { it.bankId == bank.id }.sortedBy { it.sortOrder } }.filter { it.second.isNotEmpty() }
        }
    }

    fun selectCard(cardId: String) {
        viewModelScope.launch {
            val card = cardRepo.getCardById(cardId) ?: return@launch
            val bank = bankRepo.getBankById(card.bankId)
            val today = LocalDate.now()
            val interestFree = InterestFreeCalculator.calculate(card, today).copy(bankShortName = bank?.shortName)
            val statementPeriod = DateUtils.getStatementPeriod(card.statementDay, today)
            val statementAmount = transactionRepo.getTotalAmountByCardAndDateRange(card.id, statementPeriod.first, statementPeriod.second)
            val annualFeeProgress = if (card.annualFeeEnabled && card.annualFeeStartDate != null && card.annualFeeRule != null) {
                val yearStart = card.annualFeeStartDate
                val yearEnd = yearStart.plusYears(1).minusDays(1)
                val amount = transactionRepo.getTotalAmountByCardAndDateRange(card.id, yearStart, yearEnd)
                val count = transactionRepo.getCountByCardAndDateRange(card.id, yearStart, yearEnd)
                val achieved = when (card.annualFeeRule.logic) {
                    com.creditcardmanager.model.enums.AnnualFeeLogic.AMOUNT_ONLY -> amount >= card.annualFeeRule.targetAmount
                    com.creditcardmanager.model.enums.AnnualFeeLogic.COUNT_ONLY -> count >= card.annualFeeRule.targetCount
                    com.creditcardmanager.model.enums.AnnualFeeLogic.BOTH_AND -> amount >= card.annualFeeRule.targetAmount && count >= card.annualFeeRule.targetCount
                    com.creditcardmanager.model.enums.AnnualFeeLogic.BOTH_OR -> amount >= card.annualFeeRule.targetAmount || count >= card.annualFeeRule.targetCount
                }
                AnnualFeeProgress(amount, count, achieved)
            } else null
            val activities = activityRepo.getActivitiesByCardId(cardId).first().map { act ->
                val (start, end) = DateUtils.getPeriodStart(act.periodType, today) to DateUtils.getPeriodEnd(act.periodType, today)
                val transactions = transactionRepo.getTransactionsByCardAndDateRange(cardId, start, end).first()
                val progress = ActivityCalculator.calculateProgress(act, transactions)
                ActivityWithProgress(act, progress, card.name, bank?.name)
            }
            val transactions = transactionRepo.getTransactionsByCardId(cardId).first().take(10)
            _selectedCard.value = CardDetail(card, bank, interestFree, statementAmount, annualFeeProgress, activities, transactions)
        }
    }

    fun addCard(card: Card) { viewModelScope.launch { cardRepo.saveCard(card); loadCards() } }
    fun updateCard(card: Card) { viewModelScope.launch { cardRepo.updateCard(card); loadCards(); selectCard(card.id) } }
    fun deleteCard(card: Card) { viewModelScope.launch { cardRepo.deleteCard(card); loadCards() } }
}

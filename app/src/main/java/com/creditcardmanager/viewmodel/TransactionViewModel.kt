package com.creditcardmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creditcardmanager.data.repository.*
import com.creditcardmanager.model.*
import com.creditcardmanager.model.enums.ActivityLevel
import com.creditcardmanager.model.enums.PeriodType
import com.creditcardmanager.utils.ActivityCalculator
import com.creditcardmanager.utils.ActivityMatcher
import com.creditcardmanager.utils.DateUtils
import com.creditcardmanager.utils.IdGenerator
import com.creditcardmanager.utils.InterestFreeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository, private val cardRepo: CardRepository,
    private val activityRepo: ActivityRepository, private val progressRepo: ActivityProgressRepository,
    private val bankRepo: BankRepository, private val tagRepo: TagRepository
) : ViewModel() {
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()
    private val _preview = MutableStateFlow<TransactionPreview?>(null)
    val preview: StateFlow<TransactionPreview?> = _preview.asStateFlow()
    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards: StateFlow<List<Card>> = _cards.asStateFlow()
    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    data class TransactionPreview(val card: Card?, val interestFreeDays: Int, val matchedActivities: List<MatchedActivity>)
    data class MatchedActivity(val activity: Activity, val cashbackPreview: Double?)

    init {
        viewModelScope.launch { cardRepo.getActiveCards().collect { _cards.value = it } }
        viewModelScope.launch { tagRepo.getAllTags().collect { _tags.value = it } }
    }

    fun loadTransactions(cardId: String) { viewModelScope.launch { transactionRepo.getTransactionsByCardId(cardId).collect { _transactions.value = it } } }

    fun previewTransaction(cardId: String, amount: Double, spendDate: LocalDate, tagId: String) {
        viewModelScope.launch {
            val card = cardRepo.getCardById(cardId)
            val interestFree = if (card != null) InterestFreeCalculator.calculate(card, spendDate).interestFreeDays else 0
            val tempTransaction = Transaction(id = "preview", cardId = cardId, amount = amount, spendDate = spendDate, tagId = tagId)
            val activities = activityRepo.getAllActiveActivities().first()
            val cards = cardRepo.getAllCards().first().associateBy { it.id }
            val matched = activities.mapNotNull { activity ->
                val activityCard = if (activity.level == ActivityLevel.CARD) activity.cardId?.let { cards[it] } else null
                if (ActivityMatcher.matchTransaction(tempTransaction, activity, activityCard)) {
                    MatchedActivity(activity, ActivityCalculator.getCashbackPreview(activity, tempTransaction))
                } else null
            }
            _preview.value = TransactionPreview(card, interestFree, matched)
        }
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepo.saveTransaction(transaction)
            val activities = activityRepo.getAllActiveActivities().first()
            val cards = cardRepo.getAllCards().first().associateBy { it.id }
            val today = LocalDate.now()
            for (activity in activities) {
                val activityCard = if (activity.level == ActivityLevel.CARD) activity.cardId?.let { cards[it] } else null
                if (ActivityMatcher.matchTransaction(transaction, activity, activityCard)) {
                    val (start, end) = when (activity.periodType) {
                        PeriodType.BIND_STATEMENT -> {
                            val c = activity.cardId?.let { cards[it] }
                            if (c != null) DateUtils.getStatementPeriod(c.statementDay, today)
                            else DateUtils.getPeriodStart(PeriodType.NATURAL_MONTH, today) to DateUtils.getPeriodEnd(PeriodType.NATURAL_MONTH, today)
                        }
                        else -> DateUtils.getPeriodStart(activity.periodType, today) to DateUtils.getPeriodEnd(activity.periodType, today)
                    }
                    val existingTransactions: List<Transaction> = if (activity.level == ActivityLevel.BANK) {
                        activity.bankId?.let { bid ->
                            val bankCards = cards.values.filter { it.bankId == bid }
                            val lists: List<List<Transaction>> = bankCards.map { transactionRepo.getTransactionsByCardAndDateRange(it.id, start, end).first() }
                            lists.flatten()
                        } ?: emptyList()
                    } else {
                        activity.cardId?.let { transactionRepo.getTransactionsByCardAndDateRange(it, start, end).first() } ?: emptyList()
                    }
                    val allTransactions = existingTransactions + transaction
                    val progress = ActivityCalculator.calculateProgress(activity, allTransactions)
                    progressRepo.saveProgress(progress)
                }
            }
            _preview.value = null
        }
    }

    fun createTransaction(cardId: String, amount: Double, spendDate: LocalDate, tagId: String, channel: String?, note: String?) {
        addTransaction(Transaction(id = IdGenerator.generateTransactionId(), cardId = cardId, amount = amount, spendDate = spendDate, tagId = tagId, channel = channel, note = note))
    }
    fun deleteTransaction(transaction: Transaction) { viewModelScope.launch { transactionRepo.deleteTransaction(transaction) } }
}

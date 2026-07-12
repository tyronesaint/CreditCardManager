package com.creditcardmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creditcardmanager.data.repository.*
import com.creditcardmanager.model.*
import com.creditcardmanager.model.enums.ActivityLevel
import com.creditcardmanager.model.enums.PeriodType
import com.creditcardmanager.model.enums.SourceType
import com.creditcardmanager.utils.ActivityCalculator
import com.creditcardmanager.utils.DateUtils
import com.creditcardmanager.utils.IdGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepo: ActivityRepository, private val progressRepo: ActivityProgressRepository,
    private val transactionRepo: TransactionRepository, private val cardRepo: CardRepository,
    private val bankRepo: BankRepository, private val reminderRepo: ReminderRepository
) : ViewModel() {
    private val _activities = MutableStateFlow<List<ActivityWithProgress>>(emptyList())
    val activities: StateFlow<List<ActivityWithProgress>> = _activities.asStateFlow()
    private val _selectedActivity = MutableStateFlow<ActivityDetail?>(null)
    val selectedActivity: StateFlow<ActivityDetail?> = _selectedActivity.asStateFlow()

    data class ActivityDetail(val activity: Activity, val progress: ActivityProgress, val cardName: String?,
        val bankName: String?, val transactions: List<Transaction>)

    enum class ActivityFilter { ALL, BANK, CARD, ARCHIVED }
    private var currentFilter: ActivityFilter = ActivityFilter.ALL

    init { loadActivities(ActivityFilter.ALL) }

    fun loadActivities(filter: ActivityFilter) {
        currentFilter = filter
        viewModelScope.launch {
            val flow = when (filter) {
                ActivityFilter.ALL -> activityRepo.getAllActiveActivities()
                ActivityFilter.BANK -> activityRepo.getBankActivities()
                ActivityFilter.CARD -> activityRepo.getCardActivities()
                ActivityFilter.ARCHIVED -> activityRepo.getAllArchivedActivities()
            }
            flow.collect { acts ->
                val banks = bankRepo.getAllBanks().first().associateBy { it.id }
                val cards = cardRepo.getAllCards().first().associateBy { it.id }
                val today = LocalDate.now()
                val list = acts.map { activity ->
                    val (start, end) = when (activity.periodType) {
                        PeriodType.BIND_STATEMENT -> {
                            val card = activity.cardId?.let { cards[it] }
                            if (card != null) DateUtils.getStatementPeriod(card.statementDay, today)
                            else DateUtils.getPeriodStart(PeriodType.NATURAL_MONTH, today) to DateUtils.getPeriodEnd(PeriodType.NATURAL_MONTH, today)
                        }
                        else -> DateUtils.getPeriodStart(activity.periodType, today) to DateUtils.getPeriodEnd(activity.periodType, today)
                    }
                    val transactions = if (activity.level == ActivityLevel.BANK) {
                        activity.bankId?.let { bid ->
                            cards.values.filter { it.bankId == bid }
                                .flatMap { transactionRepo.getTransactionsByCardAndDateRange(it.id, start, end).first() }
                        } ?: emptyList()
                    } else {
                        activity.cardId?.let { transactionRepo.getTransactionsByCardAndDateRange(it, start, end).first() } ?: emptyList()
                    }
                    val progress = ActivityCalculator.calculateProgress(activity, transactions)
                    ActivityWithProgress(activity, progress, activity.cardId?.let { cards[it]?.name }, activity.bankId?.let { banks[it]?.name })
                }
                _activities.value = list
            }
        }
    }

    fun selectActivity(activityId: String) {
        viewModelScope.launch {
            val activity = activityRepo.getActivityById(activityId) ?: return@launch
            val banks = bankRepo.getAllBanks().first().associateBy { it.id }
            val cards = cardRepo.getAllCards().first().associateBy { it.id }
            val today = LocalDate.now()
            val (start, end) = when (activity.periodType) {
                PeriodType.BIND_STATEMENT -> {
                    val card = activity.cardId?.let { cards[it] }
                    if (card != null) DateUtils.getStatementPeriod(card.statementDay, today)
                    else DateUtils.getPeriodStart(PeriodType.NATURAL_MONTH, today) to DateUtils.getPeriodEnd(PeriodType.NATURAL_MONTH, today)
                }
                else -> DateUtils.getPeriodStart(activity.periodType, today) to DateUtils.getPeriodEnd(activity.periodType, today)
            }
            val transactions = if (activity.level == ActivityLevel.BANK) {
                activity.bankId?.let { bid ->
                    cards.values.filter { it.bankId == bid }
                        .flatMap { transactionRepo.getTransactionsByCardAndDateRange(it.id, start, end).first() }
                } ?: emptyList()
            } else {
                activity.cardId?.let { transactionRepo.getTransactionsByCardAndDateRange(it, start, end).first() } ?: emptyList()
            }
            val progress = ActivityCalculator.calculateProgress(activity, transactions)
            _selectedActivity.value = ActivityDetail(activity, progress, activity.cardId?.let { cards[it]?.name }, activity.bankId?.let { banks[it]?.name }, transactions)
        }
    }

    fun addActivity(activity: Activity) {
        viewModelScope.launch {
            activityRepo.saveActivity(activity)
            if (activity.reward.claimReminderEnabled) createClaimReminder(activity)
            loadActivities(currentFilter)
        }
    }
    fun updateActivity(activity: Activity) { viewModelScope.launch { activityRepo.updateActivity(activity); loadActivities(currentFilter); selectActivity(activity.id) } }
    fun archiveActivity(activityId: String) { viewModelScope.launch { activityRepo.archiveActivity(activityId); loadActivities(currentFilter) } }
    fun deleteActivity(activity: Activity) { viewModelScope.launch { activityRepo.deleteActivity(activity); loadActivities(currentFilter) } }
    fun markAchieved(activityId: String) {
        viewModelScope.launch {
            val progress = progressRepo.getProgressByActivityIdSync(activityId)
            val newProgress = (progress ?: ActivityProgress(activityId, DateUtils.getPeriodKey(PeriodType.NATURAL_MONTH))).copy(isAchieved = true)
            progressRepo.saveProgress(newProgress); loadActivities(currentFilter)
        }
    }
    fun markCashbackFull(activityId: String, daily: Boolean = false, monthly: Boolean = false) {
        viewModelScope.launch {
            val progress = progressRepo.getProgressByActivityIdSync(activityId)
            var newProgress = progress ?: ActivityProgress(activityId, DateUtils.getPeriodKey(PeriodType.NATURAL_MONTH))
            if (daily) newProgress = newProgress.copy(todayCashback = 9999.0)
            if (monthly) newProgress = newProgress.copy(currentCashback = 9999.0, isAchieved = true)
            progressRepo.saveProgress(newProgress); loadActivities(currentFilter)
        }
    }
    private suspend fun createClaimReminder(activity: Activity) {
        val reminder = Reminder(id = IdGenerator.generateReminderId(), sourceType = SourceType.ACTIVITY, sourceId = activity.id,
            title = "领取奖励: ${activity.name}", remindTimes = listOf(ReminderTime(offsetDays = 0, timeOfDay = activity.reward.claimTime ?: "10:00")), enabled = true)
        reminderRepo.saveReminder(reminder)
    }
}

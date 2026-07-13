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

    enum class ActivityFilter { ALL, BANK, CARD, GENERAL, ARCHIVED }
    enum class SortType { BY_BANK, BY_CARD, BY_TYPE, BY_PROGRESS }

    private var currentFilter: ActivityFilter = ActivityFilter.ALL
    private var currentSort: SortType = SortType.BY_BANK

    init { loadActivities(ActivityFilter.ALL) }

    fun loadActivities(filter: ActivityFilter) {
        currentFilter = filter
        refreshActivities()
    }

    fun setSortType(sort: SortType) {
        currentSort = sort
        refreshActivities()
    }

    private fun refreshActivities() {
        viewModelScope.launch {
            val flow = when (currentFilter) {
                ActivityFilter.ALL -> activityRepo.getAllActiveActivities()
                ActivityFilter.BANK -> activityRepo.getBankActivities()
                ActivityFilter.CARD -> activityRepo.getCardActivities()
                ActivityFilter.GENERAL -> activityRepo.getAllActiveActivities()
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
                    val transactions: List<Transaction> = if (activity.level == ActivityLevel.BANK) {
                        if (activity.bankId != null) {
                            buildList {
                                for (c in cards.values) {
                                    if (c.bankId == activity.bankId) {
                                        addAll(transactionRepo.getTransactionsByCardAndDateRange(c.id, start, end))
                                    }
                                }
                            }
                        } else emptyList()
                    } else {
                        activity.cardId?.let { transactionRepo.getTransactionsByCardAndDateRange(it, start, end) } ?: emptyList()
                    }
                    // 获取现有进度用于连续达标累加
                    val existingProgress = progressRepo.getProgressByActivityIdSync(activity.id)
                    val progress = ActivityCalculator.calculateProgress(activity, transactions, existingProgress)
                    // 保存计算后的进度
                    progressRepo.saveProgress(progress)
                    ActivityWithProgress(activity, progress, activity.cardId?.let { cards[it]?.name }, activity.bankId?.let { banks[it]?.name })
                }
                val sorted = when (currentSort) {
                    SortType.BY_BANK -> list.sortedWith(compareBy({ it.bankName ?: "" }, { it.cardName ?: "" }))
                    SortType.BY_CARD -> list.sortedWith(compareBy({ it.cardName ?: "" }, { it.bankName ?: "" }))
                    SortType.BY_TYPE -> list.sortedBy { it.activity.type.name }
                    SortType.BY_PROGRESS -> list.sortedByDescending { it.progress.currentAmount / (it.activity.targetAmount ?: 1.0) }
                }
                _activities.value = sorted
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
            val transactions: List<Transaction> = if (activity.level == ActivityLevel.BANK) {
                if (activity.bankId != null) {
                    buildList {
                        for (c in cards.values) {
                            if (c.bankId == activity.bankId) {
                                addAll(transactionRepo.getTransactionsByCardAndDateRange(c.id, start, end))
                            }
                        }
                    }
                } else emptyList()
            } else {
                activity.cardId?.let { transactionRepo.getTransactionsByCardAndDateRange(it, start, end) } ?: emptyList()
            }
            val existingProgress = progressRepo.getProgressByActivityIdSync(activity.id)
            val progress = ActivityCalculator.calculateProgress(activity, transactions, existingProgress)
            progressRepo.saveProgress(progress)
            _selectedActivity.value = ActivityDetail(activity, progress, activity.cardId?.let { cards[it]?.name }, activity.bankId?.let { banks[it]?.name }, transactions)
        }
    }

    fun addActivity(activity: Activity) {
        viewModelScope.launch {
            activityRepo.saveActivity(activity)
            if (activity.reward.claimReminderEnabled) createClaimReminder(activity)
            refreshActivities()
        }
    }
    fun updateActivity(activity: Activity) {
        viewModelScope.launch {
            activityRepo.updateActivity(activity)
            refreshActivities()
            selectActivity(activity.id)
        }
    }
    fun archiveActivity(activityId: String) {
        viewModelScope.launch {
            activityRepo.archiveActivity(activityId)
            refreshActivities()
        }
    }
    fun unarchiveActivity(activityId: String) {
        viewModelScope.launch {
            activityRepo.unarchiveActivity(activityId)
            refreshActivities()
        }
    }
    fun deleteActivity(activity: Activity) {
        viewModelScope.launch {
            activityRepo.deleteActivity(activity)
            refreshActivities()
        }
    }
    fun markAchieved(activityId: String) {
        viewModelScope.launch {
            val progress = progressRepo.getProgressByActivityIdSync(activityId)
            val activity = activityRepo.getActivityById(activityId)
            val periodKey = activity?.let { DateUtils.getPeriodKey(it.periodType) } ?: DateUtils.getPeriodKey(PeriodType.NATURAL_MONTH)
            val newProgress = (progress ?: ActivityProgress(activityId, periodKey)).copy(isAchieved = true)
            progressRepo.saveProgress(newProgress)
            refreshActivities()
        }
    }
    fun markCashbackFull(activityId: String, daily: Boolean = false, monthly: Boolean = false) {
        viewModelScope.launch {
            val progress = progressRepo.getProgressByActivityIdSync(activityId)
            val activity = activityRepo.getActivityById(activityId)
            val periodKey = activity?.let { DateUtils.getPeriodKey(it.periodType) } ?: DateUtils.getPeriodKey(PeriodType.NATURAL_MONTH)
            var newProgress = progress ?: ActivityProgress(activityId, periodKey)
            if (daily) newProgress = newProgress.copy(todayCashback = 9999.0)
            if (monthly) newProgress = newProgress.copy(currentCashback = 9999.0, isAchieved = true)
            progressRepo.saveProgress(newProgress)
            refreshActivities()
        }
    }
    private suspend fun createClaimReminder(activity: Activity) {
        val reminder = Reminder(id = IdGenerator.generateReminderId(), sourceType = SourceType.ACTIVITY, sourceId = activity.id,
            title = "领取奖励: ${activity.name}", remindTimes = listOf(ReminderTime(offsetDays = 0, timeOfDay = activity.reward.claimTime ?: "10:00")), enabled = true)
        reminderRepo.saveReminder(reminder)
    }
}

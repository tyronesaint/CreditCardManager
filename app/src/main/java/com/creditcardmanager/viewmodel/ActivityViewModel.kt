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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepo: ActivityRepository,
    private val progressRepo: ActivityProgressRepository,
    private val transactionRepo: TransactionRepository,
    private val cardRepo: CardRepository,
    private val bankRepo: BankRepository,
    private val reminderRepo: ReminderRepository
) : ViewModel() {
    private val _activities = MutableStateFlow<List<ActivityWithProgress>>(emptyList())
    val activities: StateFlow<List<ActivityWithProgress>> = _activities.asStateFlow()
    private val _selectedActivity = MutableStateFlow<ActivityDetail?>(null)
    val selectedActivity: StateFlow<ActivityDetail?> = _selectedActivity.asStateFlow()

    data class ActivityDetail(
        val activity: Activity,
        val progress: ActivityProgress,
        val cardName: String?,
        val bankName: String?,
        val transactions: List<Transaction>
    )

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
        viewModelScope.launch(Dispatchers.IO) {
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
                            if (card != null) {
                                DateUtils.getStatementPeriod(card.statementDay, today)
                            } else {
                                DateUtils.getPeriodStart(PeriodType.NATURAL_MONTH, today) to
                                DateUtils.getPeriodEnd(PeriodType.NATURAL_MONTH, today)
                            }
                        }
                        else -> DateUtils.getPeriodStart(activity.periodType, today) to
                                DateUtils.getPeriodEnd(activity.periodType, today)
                    }
                    // BIND_STATEMENT的periodKey用账单起始月的"yyyy-MM"
                    val periodKey = if (activity.periodType == PeriodType.BIND_STATEMENT) {
                        "${start.year}-${String.format("%02d", start.monthValue)}"
                    } else {
                        DateUtils.getPeriodKey(activity.periodType)
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
                    val existingProgress = progressRepo.getProgressByActivityIdAndPeriodSync(activity.id, periodKey)
                    val progress = ActivityCalculator.calculateProgress(
                        activity = activity,
                        transactions = transactions,
                        existingProgress = existingProgress,
                        periodKeyOverride = if (activity.periodType == PeriodType.BIND_STATEMENT) periodKey else null
                    )
                    progressRepo.saveProgress(progress)
                    ActivityWithProgress(
                        activity,
                        progress,
                        activity.cardId?.let { cards[it]?.name },
                        activity.bankId?.let { banks[it]?.name }
                    )
                }
                val sorted = when (currentSort) {
                    SortType.BY_BANK -> list.sortedWith(compareBy({ it.bankName ?: "" }, { it.cardName ?: "" }))
                    SortType.BY_CARD -> list.sortedWith(compareBy({ it.cardName ?: "" }, { it.bankName ?: "" }))
                    SortType.BY_TYPE -> list.sortedBy { it.activity.type.name }
                    SortType.BY_PROGRESS -> list.sortedByDescending {
                        if (it.activity.targetAmount != null) it.progress.currentAmount / it.activity.targetAmount!! else 0.0
                    }
                }
                _activities.value = sorted
            }
        }
    }

    fun selectActivity(activityId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val activity = activityRepo.getActivityById(activityId) ?: return@launch
            val banks = bankRepo.getAllBanks().first().associateBy { it.id }
            val cards = cardRepo.getAllCards().first().associateBy { it.id }
            val today = LocalDate.now()
            val (start, end) = when (activity.periodType) {
                PeriodType.BIND_STATEMENT -> {
                    val card = activity.cardId?.let { cards[it] }
                    if (card != null) {
                        DateUtils.getStatementPeriod(card.statementDay, today)
                    } else {
                        DateUtils.getPeriodStart(PeriodType.NATURAL_MONTH, today) to
                        DateUtils.getPeriodEnd(PeriodType.NATURAL_MONTH, today)
                    }
                }
                else -> DateUtils.getPeriodStart(activity.periodType, today) to
                        DateUtils.getPeriodEnd(activity.periodType, today)
            }
            // BIND_STATEMENT的periodKey用账单起始月的"yyyy-MM"
            val periodKey = if (activity.periodType == PeriodType.BIND_STATEMENT) {
                "${start.year}-${String.format("%02d", start.monthValue)}"
            } else {
                DateUtils.getPeriodKey(activity.periodType)
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
            val existingProgress = progressRepo.getProgressByActivityIdAndPeriodSync(activity.id, periodKey)
            val progress = ActivityCalculator.calculateProgress(
                activity = activity,
                transactions = transactions,
                existingProgress = existingProgress,
                periodKeyOverride = if (activity.periodType == PeriodType.BIND_STATEMENT) periodKey else null
            )
            progressRepo.saveProgress(progress)
            _selectedActivity.value = ActivityDetail(
                activity,
                progress,
                activity.cardId?.let { cards[it]?.name },
                activity.bankId?.let { banks[it]?.name },
                transactions
            )
        }
    }

    fun addActivity(activity: Activity) {
        viewModelScope.launch(Dispatchers.IO) {
            activityRepo.saveActivity(activity)
            if (activity.reward.claimReminderEnabled) createClaimReminder(activity)
            refreshActivities()
        }
    }

    fun updateActivity(activity: Activity) {
        viewModelScope.launch(Dispatchers.IO) {
            activityRepo.updateActivity(activity)
            refreshActivities()
            selectActivity(activity.id)
        }
    }

    fun archiveActivity(activityId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            activityRepo.archiveActivity(activityId)
            refreshActivities()
        }
    }

    fun unarchiveActivity(activityId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            activityRepo.unarchiveActivity(activityId)
            refreshActivities()
        }
    }

    fun deleteActivity(activity: Activity) {
        viewModelScope.launch(Dispatchers.IO) {
            activityRepo.deleteActivity(activity)
            refreshActivities()
        }
    }

    /**
     * 新增：手动调整进度（核心方法，符合你"查账后旧消费可有可无"的语义）
     * @param baseline 你查出来的基准值（比如银行App里看到的返现金额）
     * @param source "CHECK"=查账/"MANUAL"=手动拍脑袋
     */
    fun manualAdjustProgress(
        activityId: String,
        periodKey: String,
        baseline: Double,
        source: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingProgress = progressRepo.getProgressByActivityIdAndPeriodSync(activityId, periodKey)
            val newProgress = (existingProgress ?: ActivityProgress(activityId, periodKey)).copy(
                manualBaseline = baseline,
                baselineSource = source,
                manualSince = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            progressRepo.saveProgress(newProgress)
            refreshActivities()
            selectActivity(activityId)
        }
    }

    /**
     * 修正：markAchieved/markCashbackFull的periodKey动态化，不再写死NATURAL_MONTH
     */
    fun markAchieved(activityId: String, periodKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingProgress = progressRepo.getProgressByActivityIdAndPeriodSync(activityId, periodKey)
            val newProgress = (existingProgress ?: ActivityProgress(activityId, periodKey)).copy(
                isAchieved = true,
                updatedAt = System.currentTimeMillis()
            )
            progressRepo.saveProgress(newProgress)
            refreshActivities()
        }
    }

    fun markCashbackFull(activityId: String, periodKey: String, daily: Boolean = false, monthly: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingProgress = progressRepo.getProgressByActivityIdAndPeriodSync(activityId, periodKey)
            var newProgress = existingProgress ?: ActivityProgress(activityId, periodKey)
            if (daily) newProgress = newProgress.copy(todayCashback = 9999.0)
            if (monthly) newProgress = newProgress.copy(
                currentCashback = 9999.0,
                isAchieved = true,
                updatedAt = System.currentTimeMillis()
            )
            progressRepo.saveProgress(newProgress)
            refreshActivities()
        }
    }

    private suspend fun createClaimReminder(activity: Activity) {
        val reminder = Reminder(
            id = IdGenerator.generateReminderId(),
            sourceType = SourceType.ACTIVITY,
            sourceId = activity.id,
            title = "领取奖励: ${activity.name}",
            remindTimes = listOf(ReminderTime(
                offsetDays = 0,
                timeOfDay = activity.reward.claimTime ?: "10:00"
            )),
            enabled = true
        )
        reminderRepo.saveReminder(reminder)
    }
}

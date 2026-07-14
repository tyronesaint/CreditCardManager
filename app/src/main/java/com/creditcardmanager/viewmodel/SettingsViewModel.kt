package com.creditcardmanager.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creditcardmanager.data.repository.*
import com.creditcardmanager.model.ActivityProgress
import com.creditcardmanager.model.enums.ActivityLevel
import com.creditcardmanager.model.enums.PeriodType
import com.creditcardmanager.utils.ActivityCalculator
import com.creditcardmanager.utils.DateUtils
import com.creditcardmanager.utils.ExportImportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val bankRepo: BankRepository,
    private val cardRepo: CardRepository,
    private val tagRepo: TagRepository,
    private val activityRepo: ActivityRepository,
    private val reminderRepo: ReminderRepository,
    private val transactionRepo: TransactionRepository,
    private val progressRepo: ActivityProgressRepository,
    private val exportManager: ExportImportManager
) : ViewModel() {
    private val _exportResult = MutableSharedFlow<String>()
    val exportResult: SharedFlow<String> = _exportResult.asSharedFlow()
    private val _importResult = MutableSharedFlow<Boolean>()
    val importResult: SharedFlow<Boolean> = _importResult.asSharedFlow()

    fun exportData(includeTransactions: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val banks = bankRepo.getAllBanks().first()
            val tags = tagRepo.getAllTags().first()
            val cards = cardRepo.getAllCards().first()
            val activities = activityRepo.getAllActiveActivities().first()
            val reminders = reminderRepo.getAllReminders().first()
            val transactions = if (includeTransactions) transactionRepo.getAllTransactions().first() else null
            val json = exportManager.exportToJson(banks, tags, cards, activities, reminders, transactions)
            val file = exportManager.saveExportFile(json)
            _exportResult.emit(file.absolutePath)
        }
    }

    fun importData(json: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = exportManager.importFromJson(json)
                if (data != null) {
                    bankRepo.deleteAll()
                    tagRepo.deleteAll()
                    cardRepo.deleteAll()
                    activityRepo.deleteAll()
                    reminderRepo.deleteAll()
                    transactionRepo.deleteAll()
                    bankRepo.saveBanks(data.banks)
                    tagRepo.saveTags(data.tags)
                    cardRepo.saveCards(data.cards)
                    activityRepo.saveActivities(data.activities)
                    reminderRepo.saveReminders(data.reminders)
                    data.transactions?.let { transactionRepo.saveTransactions(it) }
                    recalculateAllActivities()
                    _importResult.emit(true)
                } else {
                    Log.e("SettingsViewModel", "导入失败：JSON解析结果为空")
                    _importResult.emit(false)
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "导入失败", e)
                _importResult.emit(false)
            }
        }
    }

    private suspend fun recalculateAllActivities() {
        val activities = activityRepo.getAllActiveActivities().first()
        val allTransactions = transactionRepo.getAllTransactions().first()
        val allCards = cardRepo.getAllCards().first().associateBy { it.id }
        val today = LocalDate.now()

        for (activity in activities) {
            val (start, end) = when (activity.periodType) {
                PeriodType.BIND_STATEMENT -> {
                    val card = activity.cardId?.let { allCards[it] }
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
            val periodKey = if (activity.periodType == PeriodType.BIND_STATEMENT) {
                "${start.year}-${String.format("%02d", start.monthValue)}"
            } else {
                DateUtils.getPeriodKey(activity.periodType)
            }
            val existingProgress = progressRepo.getProgressByActivityIdAndPeriodSync(activity.id, periodKey)
            val activityTransactions = if (activity.level == ActivityLevel.BANK) {
                if (activity.bankId != null) {
                    buildList {
                        for (c in allCards.values) {
                            if (c.bankId == activity.bankId) {
                                addAll(allTransactions.filter { it.cardId == c.id && it.spendDate in start..end })
                            }
                        }
                    }
                } else emptyList()
            } else {
                activity.cardId?.let { cardId ->
                    allTransactions.filter { it.cardId == cardId && it.spendDate in start..end }
                } ?: emptyList()
            }
            val progress = ActivityCalculator.calculateProgress(
                activity = activity,
                transactions = activityTransactions,
                existingProgress = existingProgress,
                periodKeyOverride = if (activity.periodType == PeriodType.BIND_STATEMENT) periodKey else null
            )
            progressRepo.saveProgress(progress)
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepo.deleteAll()
            reminderRepo.deleteAll()
            activityRepo.deleteAll()
            cardRepo.deleteAll()
            tagRepo.deleteAll()
            bankRepo.deleteAll()
        }
    }
}

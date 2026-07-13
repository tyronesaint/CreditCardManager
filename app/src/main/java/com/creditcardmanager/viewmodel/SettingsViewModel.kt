package com.creditcardmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creditcardmanager.data.repository.*
import com.creditcardmanager.utils.ExportImportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val bankRepo: BankRepository,
    private val cardRepo: CardRepository,
    private val tagRepo: TagRepository,
    private val activityRepo: ActivityRepository,
    private val reminderRepo: ReminderRepository,
    private val transactionRepo: TransactionRepository,
    private val exportManager: ExportImportManager,
    private val transactionViewModel: TransactionViewModel // 新增：用来调全量重算
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
                    // 先删旧数据
                    bankRepo.deleteAll()
                    tagRepo.deleteAll()
                    cardRepo.deleteAll()
                    activityRepo.deleteAll()
                    reminderRepo.deleteAll()
                    transactionRepo.deleteAll()
                    // 插新数据
                    bankRepo.saveBanks(data.banks)
                    tagRepo.saveTags(data.tags)
                    cardRepo.saveCards(data.cards)
                    activityRepo.saveActivities(data.activities)
                    reminderRepo.saveReminders(data.reminders)
                    data.transactions?.let { transactionRepo.saveTransactions(it) }
                    // 新增：插完数据后全量重算进度（解决导入后进度空白的问题）
                    transactionViewModel.recalculateAllActivities()
                    _importResult.emit(true)
                } else {
                    Timber.e("导入失败：JSON解析结果为空（格式错误或字段不匹配）")
                    _importResult.emit(false)
                }
            } catch (e: Exception) {
                Timber.e(e, "导入失败：发生异常")
                _importResult.emit(false)
            }
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

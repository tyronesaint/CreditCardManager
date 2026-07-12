package com.creditcardmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creditcardmanager.data.repository.*
import com.creditcardmanager.utils.ExportImportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val bankRepo: BankRepository, private val cardRepo: CardRepository,
    private val tagRepo: TagRepository, private val activityRepo: ActivityRepository,
    private val reminderRepo: ReminderRepository, private val transactionRepo: TransactionRepository,
    private val exportManager: ExportImportManager
) : ViewModel() {
    private val _exportResult = MutableSharedFlow<String>()
    val exportResult: SharedFlow<String> = _exportResult.asSharedFlow()
    private val _importResult = MutableSharedFlow<Boolean>()
    val importResult: SharedFlow<Boolean> = _importResult.asSharedFlow()

    fun exportData(includeTransactions: Boolean = false) {
        viewModelScope.launch {
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
        viewModelScope.launch {
            val data = exportManager.importFromJson(json)
            if (data != null) {
                bankRepo.deleteAll(); tagRepo.deleteAll(); cardRepo.deleteAll(); activityRepo.deleteAll(); reminderRepo.deleteAll(); transactionRepo.deleteAll()
                bankRepo.saveBanks(data.banks); tagRepo.saveTags(data.tags); cardRepo.saveCards(data.cards)
                activityRepo.saveActivities(data.activities); reminderRepo.saveReminders(data.reminders)
                data.transactions?.let { transactionRepo.saveTransactions(it) }
                _importResult.emit(true)
            } else _importResult.emit(false)
        }
    }

    fun clearAllData() {
        viewModelScope.launch { transactionRepo.deleteAll(); reminderRepo.deleteAll(); activityRepo.deleteAll(); cardRepo.deleteAll(); tagRepo.deleteAll(); bankRepo.deleteAll() }
    }
}

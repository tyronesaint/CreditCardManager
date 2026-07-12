package com.creditcardmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creditcardmanager.data.repository.ReminderRepository
import com.creditcardmanager.model.Reminder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepo: ReminderRepository
) : ViewModel() {
    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    init {
        viewModelScope.launch {
            reminderRepo.getAllReminders().collect { _reminders.value = it }
        }
    }

    fun addReminder(reminder: Reminder) {
        viewModelScope.launch { reminderRepo.saveReminder(reminder) }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch { reminderRepo.deleteReminder(reminder) }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { reminderRepo.setEnabled(id, enabled) }
    }

    fun setCompleted(id: String, completed: Boolean) {
        viewModelScope.launch { reminderRepo.setCompleted(id, completed) }
    }
}

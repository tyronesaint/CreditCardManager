package com.creditcardmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creditcardmanager.data.repository.BankRepository
import com.creditcardmanager.model.Bank
import com.creditcardmanager.utils.IdGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BankViewModel @Inject constructor(private val bankRepo: BankRepository) : ViewModel() {
    private val _banks = MutableStateFlow<List<Bank>>(emptyList())
    val banks: StateFlow<List<Bank>> = _banks.asStateFlow()
    init { viewModelScope.launch { bankRepo.getAllBanks().collect { _banks.value = it } } }
    fun addBank(name: String, shortName: String? = null) { viewModelScope.launch { bankRepo.saveBank(Bank(id = IdGenerator.generateBankId(), name = name, shortName = shortName, sortOrder = _banks.value.size)) } }
    fun updateBank(bank: Bank) { viewModelScope.launch { bankRepo.updateBank(bank) } }
    fun deleteBank(bank: Bank) { viewModelScope.launch { bankRepo.deleteBank(bank) } }
}

package com.creditcardmanager.data.repository

import com.creditcardmanager.data.local.dao.BankDao
import com.creditcardmanager.data.local.entity.BankEntity
import com.creditcardmanager.model.Bank
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankRepository @Inject constructor(private val bankDao: BankDao) {
    fun getAllBanks(): Flow<List<Bank>> = bankDao.getAll().map { list -> list.map { it.toModel() } }
    suspend fun getBankById(id: String): Bank? = bankDao.getById(id)?.toModel()
    suspend fun saveBank(bank: Bank) = bankDao.insert(bank.toEntity())
    suspend fun saveBanks(banks: List<Bank>) = bankDao.insertAll(banks.map { it.toEntity() })
    suspend fun updateBank(bank: Bank) = bankDao.update(bank.toEntity())
    suspend fun deleteBank(bank: Bank) = bankDao.delete(bank.toEntity())
    suspend fun deleteAll() = bankDao.deleteAll()
    suspend fun getCount(): Int = bankDao.getCount()
    private fun BankEntity.toModel() = Bank(id = id, name = name, shortName = shortName, sortOrder = sortOrder, createdAt = createdAt)
    private fun Bank.toEntity() = BankEntity(id = id, name = name, shortName = shortName, sortOrder = sortOrder, createdAt = createdAt)
}

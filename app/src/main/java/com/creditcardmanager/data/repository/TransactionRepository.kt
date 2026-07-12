package com.creditcardmanager.data.repository

import com.creditcardmanager.data.local.dao.TransactionDao
import com.creditcardmanager.data.local.entity.TransactionEntity
import com.creditcardmanager.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(private val transactionDao: TransactionDao) {
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAll().map { list -> list.map { it.toModel() } }
    fun getTransactionsByCardId(cardId: String): Flow<List<Transaction>> = transactionDao.getByCardId(cardId).map { list -> list.map { it.toModel() } }
    suspend fun getTransactionsByCardAndDateRange(cardId: String, startDate: LocalDate, endDate: LocalDate): List<Transaction> =
        transactionDao.getByCardIdAndDateRange(cardId, startDate.toString(), endDate.toString()).map { it.toModel() }
    suspend fun getTransactionsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Transaction> =
        transactionDao.getByDateRange(startDate.toString(), endDate.toString()).map { it.toModel() }
    suspend fun getTransactionById(id: String): Transaction? = transactionDao.getById(id)?.toModel()
    suspend fun saveTransaction(transaction: Transaction) = transactionDao.insert(transaction.toEntity())
    suspend fun saveTransactions(transactions: List<Transaction>) = transactionDao.insertAll(transactions.map { it.toEntity() })
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.update(transaction.toEntity())
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.delete(transaction.toEntity())
    suspend fun deleteByCardId(cardId: String) = transactionDao.deleteByCardId(cardId)
    suspend fun deleteAll() = transactionDao.deleteAll()
    suspend fun getTotalAmountByCardAndDateRange(cardId: String, startDate: LocalDate, endDate: LocalDate): Double =
        transactionDao.getTotalAmountByCardAndDateRange(cardId, startDate.toString(), endDate.toString()) ?: 0.0
    suspend fun getCountByCardAndDateRange(cardId: String, startDate: LocalDate, endDate: LocalDate): Int =
        transactionDao.getCountByCardAndDateRange(cardId, startDate.toString(), endDate.toString())
    private fun TransactionEntity.toModel() = Transaction(id = id, cardId = cardId, amount = amount, spendDate = LocalDate.parse(spendDate), tagId = tagId, channel = channel, note = note, createdAt = createdAt)
    private fun Transaction.toEntity() = TransactionEntity(id = id, cardId = cardId, amount = amount, spendDate = spendDate.toString(), tagId = tagId, channel = channel, note = note, createdAt = createdAt)
}

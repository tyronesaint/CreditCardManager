package com.creditcardmanager.data.local.dao

import androidx.room.*
import com.creditcardmanager.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY spendDate DESC, createdAt DESC")
    fun getAll(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE cardId = :cardId ORDER BY spendDate DESC, createdAt DESC")
    fun getByCardId(cardId: String): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE cardId = :cardId AND spendDate >= :startDate AND spendDate <= :endDate ORDER BY spendDate DESC")
    suspend fun getByCardIdAndDateRange(cardId: String, startDate: String, endDate: String): List<TransactionEntity>
    @Query("SELECT * FROM transactions WHERE spendDate >= :startDate AND spendDate <= :endDate ORDER BY spendDate DESC")
    suspend fun getByDateRange(startDate: String, endDate: String): List<TransactionEntity>
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)
    @Update
    suspend fun update(transaction: TransactionEntity)
    @Delete
    suspend fun delete(transaction: TransactionEntity)
    @Query("DELETE FROM transactions WHERE cardId = :cardId")
    suspend fun deleteByCardId(cardId: String)
    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
    @Query("SELECT SUM(amount) FROM transactions WHERE cardId = :cardId AND spendDate >= :startDate AND spendDate <= :endDate")
    suspend fun getTotalAmountByCardAndDateRange(cardId: String, startDate: String, endDate: String): Double?
    @Query("SELECT COUNT(*) FROM transactions WHERE cardId = :cardId AND spendDate >= :startDate AND spendDate <= :endDate")
    suspend fun getCountByCardAndDateRange(cardId: String, startDate: String, endDate: String): Int
}

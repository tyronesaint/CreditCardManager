package com.creditcardmanager.data.local.dao

import androidx.room.*
import com.creditcardmanager.data.local.entity.BankEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDao {
    @Query("SELECT * FROM banks ORDER BY sortOrder ASC, createdAt ASC")
    fun getAll(): Flow<List<BankEntity>>
    @Query("SELECT * FROM banks WHERE id = :id")
    suspend fun getById(id: String): BankEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bank: BankEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(banks: List<BankEntity>)
    @Update
    suspend fun update(bank: BankEntity)
    @Delete
    suspend fun delete(bank: BankEntity)
    @Query("DELETE FROM banks")
    suspend fun deleteAll()
    @Query("SELECT COUNT(*) FROM banks")
    suspend fun getCount(): Int
}

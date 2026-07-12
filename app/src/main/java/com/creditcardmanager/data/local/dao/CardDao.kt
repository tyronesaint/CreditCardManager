package com.creditcardmanager.data.local.dao

import androidx.room.*
import com.creditcardmanager.data.local.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE status = 'ACTIVE' ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllActive(): Flow<List<CardEntity>>
    @Query("SELECT * FROM cards ORDER BY sortOrder ASC, createdAt ASC")
    fun getAll(): Flow<List<CardEntity>>
    @Query("SELECT * FROM cards WHERE bankId = :bankId ORDER BY sortOrder ASC")
    fun getByBankId(bankId: String): Flow<List<CardEntity>>
    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: String): CardEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: CardEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<CardEntity>)
    @Update
    suspend fun update(card: CardEntity)
    @Delete
    suspend fun delete(card: CardEntity)
    @Query("DELETE FROM cards")
    suspend fun deleteAll()
    @Query("SELECT COUNT(*) FROM cards WHERE status = 'ACTIVE'")
    suspend fun getActiveCount(): Int
}

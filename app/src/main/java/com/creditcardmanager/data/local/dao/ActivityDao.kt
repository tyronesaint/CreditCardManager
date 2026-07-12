package com.creditcardmanager.data.local.dao

import androidx.room.*
import com.creditcardmanager.data.local.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getAllActive(): Flow<List<ActivityEntity>>
    @Query("SELECT * FROM activities WHERE isArchived = 1 ORDER BY createdAt ASC")
    fun getAllArchived(): Flow<List<ActivityEntity>>
    @Query("SELECT * FROM activities WHERE level = 'BANK' AND isArchived = 0")
    fun getBankActivities(): Flow<List<ActivityEntity>>
    @Query("SELECT * FROM activities WHERE level = 'CARD' AND isArchived = 0")
    fun getCardActivities(): Flow<List<ActivityEntity>>
    @Query("SELECT * FROM activities WHERE bankId = :bankId AND isArchived = 0")
    fun getByBankId(bankId: String): Flow<List<ActivityEntity>>
    @Query("SELECT * FROM activities WHERE cardId = :cardId AND isArchived = 0")
    fun getByCardId(cardId: String): Flow<List<ActivityEntity>>
    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getById(id: String): ActivityEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: ActivityEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<ActivityEntity>)
    @Update
    suspend fun update(activity: ActivityEntity)
    @Delete
    suspend fun delete(activity: ActivityEntity)
    @Query("DELETE FROM activities")
    suspend fun deleteAll()
    @Query("UPDATE activities SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: String)
    @Query("UPDATE activities SET isArchived = 0 WHERE id = :id")
    suspend fun unarchive(id: String)
}

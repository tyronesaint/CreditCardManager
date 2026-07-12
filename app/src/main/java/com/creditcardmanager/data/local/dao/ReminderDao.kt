package com.creditcardmanager.data.local.dao

import androidx.room.*
import com.creditcardmanager.data.local.entity.ReminderEntity
import com.creditcardmanager.model.enums.SourceType
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY createdAt ASC")
    fun getAll(): Flow<List<ReminderEntity>>
    @Query("SELECT * FROM reminders WHERE sourceType = :sourceType")
    fun getBySourceType(sourceType: SourceType): Flow<List<ReminderEntity>>
    @Query("SELECT * FROM reminders WHERE sourceId = :sourceId")
    fun getBySourceId(sourceId: String): Flow<List<ReminderEntity>>
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: String): ReminderEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<ReminderEntity>)
    @Update
    suspend fun update(reminder: ReminderEntity)
    @Delete
    suspend fun delete(reminder: ReminderEntity)
    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
    @Query("UPDATE reminders SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean)
    @Query("UPDATE reminders SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)
}

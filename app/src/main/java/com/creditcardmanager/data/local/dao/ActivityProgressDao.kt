package com.creditcardmanager.data.local.dao

import androidx.room.*
import com.creditcardmanager.data.local.entity.ActivityProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityProgressDao {
    @Query("SELECT * FROM activity_progress WHERE activityId = :activityId")
    fun getByActivityId(activityId: String): Flow<ActivityProgressEntity?>
    @Query("SELECT * FROM activity_progress WHERE activityId = :activityId")
    suspend fun getByActivityIdSync(activityId: String): ActivityProgressEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: ActivityProgressEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(progresses: List<ActivityProgressEntity>)
    @Update
    suspend fun update(progress: ActivityProgressEntity)
    @Query("DELETE FROM activity_progress WHERE activityId = :activityId")
    suspend fun deleteByActivityId(activityId: String)
    @Query("DELETE FROM activity_progress")
    suspend fun deleteAll()
}

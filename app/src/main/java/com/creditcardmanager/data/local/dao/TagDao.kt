package com.creditcardmanager.data.local.dao

import androidx.room.*
import com.creditcardmanager.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY createdAt ASC")
    fun getAll(): Flow<List<TagEntity>>
    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: String): TagEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>)
    @Update
    suspend fun update(tag: TagEntity)
    @Delete
    suspend fun delete(tag: TagEntity)
    @Query("DELETE FROM tags")
    suspend fun deleteAll()
}

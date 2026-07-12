package com.creditcardmanager.data.repository

import com.creditcardmanager.data.local.dao.TagDao
import com.creditcardmanager.data.local.entity.TagEntity
import com.creditcardmanager.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(private val tagDao: TagDao) {
    fun getAllTags(): Flow<List<Tag>> = tagDao.getAll().map { list -> list.map { it.toModel() } }
    suspend fun getTagById(id: String): Tag? = tagDao.getById(id)?.toModel()
    suspend fun saveTag(tag: Tag) = tagDao.insert(tag.toEntity())
    suspend fun saveTags(tags: List<Tag>) = tagDao.insertAll(tags.map { it.toEntity() })
    suspend fun updateTag(tag: Tag) = tagDao.update(tag.toEntity())
    suspend fun deleteTag(tag: Tag) = tagDao.delete(tag.toEntity())
    suspend fun deleteAll() = tagDao.deleteAll()
    private fun TagEntity.toModel() = Tag(id = id, name = name, color = color, createdAt = createdAt)
    private fun Tag.toEntity() = TagEntity(id = id, name = name, color = color, createdAt = createdAt)
}

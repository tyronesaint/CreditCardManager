package com.creditcardmanager.data.repository

import com.creditcardmanager.data.local.dao.ReminderDao
import com.creditcardmanager.data.local.entity.ReminderEntity
import com.creditcardmanager.model.Reminder
import com.creditcardmanager.model.ReminderRepeatType
import com.creditcardmanager.model.ReminderTime
import com.creditcardmanager.model.enums.SourceType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(private val reminderDao: ReminderDao, private val gson: Gson) {
    fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAll().map { list -> list.map { it.toModel() } }
    fun getRemindersBySourceType(sourceType: SourceType): Flow<List<Reminder>> = reminderDao.getBySourceType(sourceType).map { list -> list.map { it.toModel() } }
    fun getRemindersBySourceId(sourceId: String): Flow<List<Reminder>> = reminderDao.getBySourceId(sourceId).map { list -> list.map { it.toModel() } }
    suspend fun getReminderById(id: String): Reminder? = reminderDao.getById(id)?.toModel()
    suspend fun saveReminder(reminder: Reminder) = reminderDao.insert(reminder.toEntity())
    suspend fun saveReminders(reminders: List<Reminder>) = reminderDao.insertAll(reminders.map { it.toEntity() })
    suspend fun updateReminder(reminder: Reminder) = reminderDao.update(reminder.toEntity())
    suspend fun deleteReminder(reminder: Reminder) = reminderDao.delete(reminder.toEntity())
    suspend fun setCompleted(id: String, completed: Boolean) = reminderDao.setCompleted(id, completed)
    suspend fun setEnabled(id: String, enabled: Boolean) = reminderDao.setEnabled(id, enabled)
    suspend fun deleteAll() = reminderDao.deleteAll()
    private fun ReminderEntity.toModel(): Reminder {
        val type = object : TypeToken<List<ReminderTime>>() {}.type
        return Reminder(
            id = id,
            sourceType = sourceType,
            sourceId = sourceId,
            title = title,
            remindTimes = gson.fromJson(remindTimesJson, type) ?: emptyList(),
            repeatType = try { ReminderRepeatType.valueOf(repeatType) } catch (_: Exception) { ReminderRepeatType.ONCE },
            repeatValue = repeatValue,
            enabled = enabled,
            completed = completed,
            createdAt = createdAt
        )
    }
    private fun Reminder.toEntity(): ReminderEntity {
        return ReminderEntity(
            id = id,
            sourceType = sourceType,
            sourceId = sourceId,
            title = title,
            remindTimesJson = gson.toJson(remindTimes),
            repeatType = repeatType.name,
            repeatValue = repeatValue,
            enabled = enabled,
            completed = completed,
            createdAt = createdAt
        )
    }
}

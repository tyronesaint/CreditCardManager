package com.creditcardmanager.data.repository

import com.creditcardmanager.data.local.dao.ActivityDao
import com.creditcardmanager.data.local.entity.ActivityEntity
import com.creditcardmanager.model.Activity
import com.creditcardmanager.model.ActivityFilter
import com.creditcardmanager.model.ActivityReward
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(private val activityDao: ActivityDao, private val gson: Gson) {
    fun getAllActiveActivities(): Flow<List<Activity>> = activityDao.getAllActive().map { list -> list.map { it.toModel() } }
    fun getAllArchivedActivities(): Flow<List<Activity>> = activityDao.getAllArchived().map { list -> list.map { it.toModel() } }
    fun getBankActivities(): Flow<List<Activity>> = activityDao.getBankActivities().map { list -> list.map { it.toModel() } }
    fun getCardActivities(): Flow<List<Activity>> = activityDao.getCardActivities().map { list -> list.map { it.toModel() } }
    fun getActivitiesByBankId(bankId: String): Flow<List<Activity>> = activityDao.getByBankId(bankId).map { list -> list.map { it.toModel() } }
    fun getActivitiesByCardId(cardId: String): Flow<List<Activity>> = activityDao.getByCardId(cardId).map { list -> list.map { it.toModel() } }
    suspend fun getActivityById(id: String): Activity? = activityDao.getById(id)?.toModel()
    suspend fun saveActivity(activity: Activity) = activityDao.insert(activity.toEntity())
    suspend fun saveActivities(activities: List<Activity>) = activityDao.insertAll(activities.map { it.toEntity() })
    suspend fun updateActivity(activity: Activity) = activityDao.update(activity.toEntity())
    suspend fun deleteActivity(activity: Activity) = activityDao.delete(activity.toEntity())
    suspend fun archiveActivity(id: String) = activityDao.archive(id)
    suspend fun unarchiveActivity(id: String) = activityDao.unarchive(id)
    suspend fun deleteAll() = activityDao.deleteAll()
    private fun ActivityEntity.toModel(): Activity {
        return Activity(id = id, name = name, level = level, bankId = bankId, cardId = cardId, type = type, periodType = periodType,
            description = description, targetAmount = targetAmount, targetCount = targetCount, minPerAmount = minPerAmount,
            cashbackRate = cashbackRate, dailyCap = dailyCap, monthlyCap = monthlyCap, withdrawThreshold = withdrawThreshold,
            requiredPeriods = requiredPeriods, innerType = innerType, innerTargetAmount = innerTargetAmount,
            innerTargetCount = innerTargetCount, innerMinPerAmount = innerMinPerAmount, minAmount = minAmount,
            filter = filterJson?.let { gson.fromJson(it, ActivityFilter::class.java) } ?: ActivityFilter(),
            reward = rewardJson?.let { gson.fromJson(it, ActivityReward::class.java) } ?: ActivityReward(),
            isArchived = isArchived, createdAt = createdAt)
    }
    private fun Activity.toEntity(): ActivityEntity {
        return ActivityEntity(id = id, name = name, level = level, bankId = bankId, cardId = cardId, type = type, periodType = periodType,
            description = description, targetAmount = targetAmount, targetCount = targetCount, minPerAmount = minPerAmount,
            cashbackRate = cashbackRate, dailyCap = dailyCap, monthlyCap = monthlyCap, withdrawThreshold = withdrawThreshold,
            requiredPeriods = requiredPeriods, innerType = innerType, innerTargetAmount = innerTargetAmount,
            innerTargetCount = innerTargetCount, innerMinPerAmount = innerMinPerAmount, minAmount = minAmount,
            filterJson = gson.toJson(filter), rewardJson = gson.toJson(reward), isArchived = isArchived, createdAt = createdAt)
    }
}

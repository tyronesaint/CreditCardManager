package com.creditcardmanager.data.repository

import com.creditcardmanager.data.local.dao.ActivityProgressDao
import com.creditcardmanager.data.local.entity.ActivityProgressEntity
import com.creditcardmanager.model.ActivityProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityProgressRepository @Inject constructor(private val progressDao: ActivityProgressDao) {
    fun getProgressByActivityId(activityId: String): Flow<ActivityProgress?> = progressDao.getByActivityId(activityId).map { it?.toModel() }
    suspend fun getProgressByActivityIdSync(activityId: String): ActivityProgress? = progressDao.getByActivityIdSync(activityId)?.toModel()
    suspend fun getProgressByActivityIdAndPeriodSync(activityId: String, periodKey: String): ActivityProgress? = progressDao.getByActivityIdAndPeriodSync(activityId, periodKey)?.toModel()
    suspend fun saveProgress(progress: ActivityProgress) = progressDao.insert(progress.toEntity())
    suspend fun saveProgresses(progresses: List<ActivityProgress>) = progressDao.insertAll(progresses.map { it.toEntity() })
    suspend fun updateProgress(progress: ActivityProgress) = progressDao.update(progress.toEntity())
    suspend fun deleteByActivityId(activityId: String) = progressDao.deleteByActivityId(activityId)
    suspend fun deleteAll() = progressDao.deleteAll()
    private fun ActivityProgressEntity.toModel() = ActivityProgress(
        activityId = activityId, periodKey = periodKey, currentAmount = currentAmount, currentCount = currentCount,
        currentCashback = currentCashback, todayCashback = todayCashback, isAchieved = isAchieved,
        continuousDone = continuousDone, manualBaseline = manualBaseline, baselineSource = baselineSource,
        manualSince = manualSince, updatedAt = updatedAt
    )
    private fun ActivityProgress.toEntity() = ActivityProgressEntity(
        activityId = activityId, periodKey = periodKey, currentAmount = currentAmount, currentCount = currentCount,
        currentCashback = currentCashback, todayCashback = todayCashback, isAchieved = isAchieved,
        continuousDone = continuousDone, manualBaseline = manualBaseline, baselineSource = baselineSource,
        manualSince = manualSince, updatedAt = updatedAt
    )
}

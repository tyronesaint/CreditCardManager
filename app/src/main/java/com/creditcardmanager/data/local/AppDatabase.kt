package com.creditcardmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.creditcardmanager.data.local.dao.*
import com.creditcardmanager.data.local.entity.*

@Database(
    entities = [
        BankEntity::class, TagEntity::class, CardEntity::class,
        TransactionEntity::class, ActivityEntity::class,
        ActivityProgressEntity::class, ReminderEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bankDao(): BankDao
    abstract fun tagDao(): TagDao
    abstract fun cardDao(): CardDao
    abstract fun transactionDao(): TransactionDao
    abstract fun activityDao(): ActivityDao
    abstract fun activityProgressDao(): ActivityProgressDao
    abstract fun reminderDao(): ReminderDao
    companion object {
        const val DATABASE_NAME = "credit_card_manager.db"
    }
}

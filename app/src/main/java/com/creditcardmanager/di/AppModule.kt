package com.creditcardmanager.di

import android.content.Context
import androidx.room.Room
import com.creditcardmanager.data.local.AppDatabase
import com.creditcardmanager.utils.AppSettings
import com.creditcardmanager.utils.ExportImportManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration().build()
    }
    @Provides @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
            .create()
    }
    @Provides @Singleton
    fun provideAppSettings(@ApplicationContext context: Context): AppSettings = AppSettings(context)
    @Provides @Singleton
    fun provideExportImportManager(@ApplicationContext context: Context): ExportImportManager = ExportImportManager(context)
    @Provides fun provideBankDao(db: AppDatabase) = db.bankDao()
    @Provides fun provideTagDao(db: AppDatabase) = db.tagDao()
    @Provides fun provideCardDao(db: AppDatabase) = db.cardDao()
    @Provides fun provideTransactionDao(db: AppDatabase) = db.transactionDao()
    @Provides fun provideActivityDao(db: AppDatabase) = db.activityDao()
    @Provides fun provideActivityProgressDao(db: AppDatabase) = db.activityProgressDao()
    @Provides fun provideReminderDao(db: AppDatabase) = db.reminderDao()
}

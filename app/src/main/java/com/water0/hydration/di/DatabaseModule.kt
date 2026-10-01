package com.water0.hydration.di

import android.content.Context
import com.water0.hydration.data.local.HydrationDatabase
import com.water0.hydration.data.repository.HydrationRepository
import com.water0.hydration.data.repository.HydrationRepositoryImpl
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.domain.usecase.CalculateRecommendationUseCase
import com.water0.hydration.domain.usecase.DeleteHydrationUseCase
import com.water0.hydration.domain.usecase.GetHistoryUseCase
import com.water0.hydration.domain.usecase.GetTodayProgressUseCase
import com.water0.hydration.domain.usecase.LogHydrationUseCase

// Simple manual DI container - no Hilt
object AppContainer {
    @Volatile
    private var database: HydrationDatabase? = null
    
    @Volatile
    private var repository: HydrationRepository? = null
    
    @Volatile
    private var recommendationEngine: RecommendationEngine? = null
    
    @Volatile
    private var logHydrationUseCase: LogHydrationUseCase? = null
    
    @Volatile
    private var getTodayProgressUseCase: GetTodayProgressUseCase? = null
    
    @Volatile
    private var calculateRecommendationUseCase: CalculateRecommendationUseCase? = null

    @Volatile
    private var deleteHydrationUseCase: DeleteHydrationUseCase? = null

    @Volatile
    private var getHistoryUseCase: GetHistoryUseCase? = null

    @Volatile
    private var notificationScheduler: com.water0.hydration.presentation.notification.NotificationScheduler? = null

    fun getDatabase(context: Context): HydrationDatabase {
        return database ?: synchronized(this) {
            database ?: HydrationDatabase.getInstance(context).also { database = it }
        }
    }

    fun getRepository(context: Context): HydrationRepository {
        return repository ?: synchronized(this) {
            repository ?: HydrationRepositoryImpl(
                entryDao = getDatabase(context).hydrationEntryDao(),
                profileDao = getDatabase(context).userProfileDao(),
                behaviorDao = getDatabase(context).userBehaviorDao(),
                appContext = context.applicationContext
            ).also { repository = it }
        }
    }

    fun getRecommendationEngine(): RecommendationEngine {
        return recommendationEngine ?: synchronized(this) {
            recommendationEngine ?: RecommendationEngine().also { recommendationEngine = it }
        }
    }

    fun getLogHydrationUseCase(context: Context): LogHydrationUseCase {
        return logHydrationUseCase ?: synchronized(this) {
            logHydrationUseCase ?: LogHydrationUseCase(getRepository(context)).also { logHydrationUseCase = it }
        }
    }

    fun getGetTodayProgressUseCase(context: Context): GetTodayProgressUseCase {
        return getTodayProgressUseCase ?: synchronized(this) {
            getTodayProgressUseCase ?: GetTodayProgressUseCase(getRepository(context), getRecommendationEngine()).also { getTodayProgressUseCase = it }
        }
    }

    fun getCalculateRecommendationUseCase(): CalculateRecommendationUseCase {
        return calculateRecommendationUseCase ?: synchronized(this) {
            calculateRecommendationUseCase ?: CalculateRecommendationUseCase(getRecommendationEngine()).also { calculateRecommendationUseCase = it }
        }
    }

    fun getDeleteHydrationUseCase(context: Context): DeleteHydrationUseCase {
        return deleteHydrationUseCase ?: synchronized(this) {
            deleteHydrationUseCase ?: DeleteHydrationUseCase(getRepository(context)).also { deleteHydrationUseCase = it }
        }
    }

    fun getGetHistoryUseCase(context: Context): GetHistoryUseCase {
        return getHistoryUseCase ?: synchronized(this) {
            getHistoryUseCase ?: GetHistoryUseCase(getRepository(context), getRecommendationEngine()).also { getHistoryUseCase = it }
        }
    }

    @Volatile
    private var exportTrainingDataUseCase: com.water0.hydration.domain.usecase.ExportTrainingDataUseCase? = null

    fun getExportTrainingDataUseCase(context: Context): com.water0.hydration.domain.usecase.ExportTrainingDataUseCase {
        return exportTrainingDataUseCase ?: synchronized(this) {
            exportTrainingDataUseCase ?: com.water0.hydration.domain.usecase.ExportTrainingDataUseCase(
                getRepository(context), getRecommendationEngine()
            ).also { exportTrainingDataUseCase = it }
        }
    }

    fun getNotificationScheduler(context: Context): com.water0.hydration.presentation.notification.NotificationScheduler {
        return notificationScheduler ?: synchronized(this) {
            notificationScheduler ?: com.water0.hydration.presentation.notification.NotificationScheduler(
                context.applicationContext
            ).also { notificationScheduler = it }
        }
    }
}
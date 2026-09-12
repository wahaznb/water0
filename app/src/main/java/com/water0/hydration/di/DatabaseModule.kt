package com.water0.hydration.di

import android.content.Context
import com.water0.hydration.data.local.HydrationDatabase
import com.water0.hydration.data.repository.HydrationRepository
import com.water0.hydration.data.repository.HydrationRepositoryImpl
import com.water0.hydration.domain.engine.RecommendationEngine
import com.water0.hydration.domain.usecase.CalculateRecommendationUseCase
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
                behaviorDao = getDatabase(context).userBehaviorDao()
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
}
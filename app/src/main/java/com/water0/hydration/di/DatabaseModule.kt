package com.water0.hydration.di

import android.content.Context
import com.water0.hydration.data.local.HydrationDatabase
import com.water0.hydration.data.repository.HydrationRepository
import com.water0.hydration.data.repository.HydrationRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): HydrationDatabase {
        return HydrationDatabase.getInstance(context)
    }

    @Provides
    fun provideHydrationEntryDao(database: HydrationDatabase) = database.hydrationEntryDao()

    @Provides
    fun provideUserProfileDao(database: HydrationDatabase) = database.userProfileDao()

    @Provides
    fun provideUserBehaviorDao(database: HydrationDatabase) = database.userBehaviorDao()
}

@InstallIn(SingletonComponent::class)
@Module
object RepositoryModule {

    @Provides
    @Singleton
    fun provideHydrationRepository(impl: HydrationRepositoryImpl): HydrationRepository = impl
}
package com.water0.hydration.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.water0.hydration.data.local.entity.UserBehavior
import kotlinx.coroutines.flow.Flow

@Dao
interface UserBehaviorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(behavior: UserBehavior)

    @Update
    suspend fun update(behavior: UserBehavior): Int

    @Query("SELECT * FROM user_behavior WHERE id = 1")
    fun getBehavior(): Flow<UserBehavior>

    @Query("SELECT * FROM user_behavior WHERE id = 1")
    suspend fun getBehaviorSuspend(): UserBehavior?
}
package com.example.calkulatorcnc.data.dao

import androidx.room.*
import com.example.calkulatorcnc.entity.SubscriptionEntity

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscription_status WHERE id = 1")
    suspend fun getStatus(): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(status: SubscriptionEntity)
}
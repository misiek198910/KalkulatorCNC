package com.example.calkulatorcnc.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.calkulatorcnc.data.dao.SubscriptionDao
import com.example.calkulatorcnc.entity.SubscriptionEntity

@Database(
    entities = [SubscriptionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Nazwa metody musi pasować do tej używanej w BillingManager
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cnc_calculator_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
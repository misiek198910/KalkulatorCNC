package com.example.calkulatorcnc.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.calkulatorcnc.data.dao.SubscriptionDao
import com.example.calkulatorcnc.data.dao.ThreadDao
import com.example.calkulatorcnc.data.dao.ToolDao
import com.example.calkulatorcnc.entity.SubscriptionEntity
import com.example.calkulatorcnc.entity.ThreadEntity
import com.example.calkulatorcnc.entity.Tool

@Database(
    entities = [SubscriptionEntity::class, ThreadEntity::class, Tool::class],
    version = 7, // Zwiększamy wersję przy kolejnej zmianie
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun threadDao(): ThreadDao
    abstract fun toolDao(): ToolDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // DEFINICJA MIGRACJI (Przykład przejścia z wersji 6 na 7)
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tutaj wpisujesz kod SQL, jeśli zmieniłeś strukturę tabel.
            }
        }
        private val MIGRATION_7_8 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tutaj wpisujesz kod SQL, jeśli zmieniłeś strukturę tabel.

            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cnc_calculator_db"
                )
                    .createFromAsset("database.db")
                     //.fallbackToDestructiveMigration() - wyłączony tryb automatyczny migracji
                    .addMigrations(MIGRATION_7_8)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
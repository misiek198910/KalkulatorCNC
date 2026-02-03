package com.example.calkulatorcnc.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.data.dao.SubscriptionDao
import com.example.calkulatorcnc.data.dao.ThreadDao
import com.example.calkulatorcnc.data.dao.ToolDao
import com.example.calkulatorcnc.data.dao.ToolNewDao
import com.example.calkulatorcnc.entity.SubscriptionEntity
import com.example.calkulatorcnc.entity.ThreadEntity
import com.example.calkulatorcnc.entity.Tool
import com.example.calkulatorcnc.entity.ToolEntity
import java.io.File

@Database(
    entities = [SubscriptionEntity::class, ThreadEntity::class, Tool::class,ToolEntity::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun threadDao(): ThreadDao
    abstract fun toolDao(): ToolDao
    abstract fun toolNewDao(): ToolNewDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {

            }
        }
        private fun MIGRATION_7_8(context: Context): Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Tworzymy tabelę
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cnc_tools` (
                `articleNumber` TEXT NOT NULL, 
                `toolCategory` TEXT NOT NULL, 
                `toolModelName` TEXT NOT NULL, 
                `materialGroup` TEXT NOT NULL, 
                `diameter` REAL NOT NULL, 
                `flutesCount` INTEGER NOT NULL, 
                `vcMin` REAL NOT NULL, 
                `vcMax` REAL NOT NULL, 
                `feedStep` REAL NOT NULL, 
                PRIMARY KEY(`articleNumber`, `materialGroup`)
            )
        """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_search` ON `cnc_tools` (`toolCategory`, `materialGroup`, `diameter`)")

                // 2. Kopiowanie danych
                try {
                    val tempDbFile = File(context.cacheDir, "temp_data.db")
                    if (tempDbFile.exists()) {
                        val tempDbPath = tempDbFile.absolutePath

                        db.execSQL("ATTACH DATABASE '$tempDbPath' AS temp_db")

                        // Kopiujemy dane
                        db.execSQL("INSERT OR REPLACE INTO cnc_tools SELECT * FROM temp_db.cnc_tools")

                        // KLUCZOWA ZMIANA: Usuwamy db.execSQL("DETACH DATABASE temp_db")
                        // Room sam zakończy transakcję i zamknie połączenie, co zwolni plik.

                        android.util.Log.d("MIGRATION", "Pomyślnie skopiowano dane.")
                    } else {
                        android.util.Log.e("MIGRATION", "Błąd: Plik temp_data.db nie istnieje!")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MIGRATION", "Błąd migracji: ${e.message}")
                    // Jeśli tu wpadnie, Room zrobi Rollback i spróbuje ponownie przy następnym starcie
                    throw e
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val dbFile = context.getDatabasePath("cnc_calculator_db")
                if (dbFile.exists()) {
                    val currentVersion = getDatabaseVersionDirectly(dbFile)
                    if (currentVersion < 8) {
                        copyAssetToTempFile(context, "database.db")
                    }
                }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cnc_calculator_db"
                )
                    .createFromAsset("database.db")
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .addMigrations(MIGRATION_7_8(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun getDatabaseVersionDirectly(dbFile: File): Int {
            return try {
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )
                val version = db.version
                db.close()
                version
            } catch (e: Exception) { 0 }
        }

        fun copyAssetToTempFile(context: Context, fileName: String): String {
            val tempFile = File(context.cacheDir, "temp_data.db")
            context.assets.open(fileName).use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return tempFile.absolutePath
        }

        fun cleanupTempDatabase(context: Context) {
            try {
                val tempFile = File(context.cacheDir, "temp_data.db")
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                android.util.Log.e("DB_CLEANUP", "Błąd usuwania: ${e.message}")
            }
        }
    }
}
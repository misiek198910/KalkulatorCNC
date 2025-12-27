package com.example.calkulatorcnc.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.calkulatorcnc.entity.ThreadDimension
import java.io.FileOutputStream

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, 1) {

    companion object {
        const val DB_NAME = "database.db"
    }

    fun copyDatabase() {
        val dbPath = context.getDatabasePath(DB_NAME)
        if (!dbPath.exists()) {
            this.readableDatabase // Tworzy pustą bazę, aby ścieżka istniała
            context.assets.open(DB_NAME).use { input ->
                FileOutputStream(dbPath).use { output ->
                    input.copyTo(output) // Kopiuje plik z assets do pamięci telefonu
                }
            }
        }
    }

    fun getThreads(catId: Int, tabId: Int): List<ThreadDimension> {
        val list = mutableListOf<ThreadDimension>()
        val dbPath = context.getDatabasePath(DB_NAME).path
        val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)

        val cursor = db.rawQuery(
            "SELECT * FROM threads WHERE category_id = ? AND table_id = ?",
            arrayOf(catId.toString(), tabId.toString())
        )

        if (cursor.moveToFirst()) {
            do {
                list.add(ThreadDimension(
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("pitch")),
                    cursor.getString(cursor.getColumnIndexOrThrow("hole_min")),
                    cursor.getString(cursor.getColumnIndexOrThrow("hole_max")),
                    cursor.getString(cursor.getColumnIndexOrThrow("hole_size"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    override fun onCreate(db: SQLiteDatabase?) {}
    override fun onUpgrade(db: SQLiteDatabase?, old: Int, new: Int) {}
}
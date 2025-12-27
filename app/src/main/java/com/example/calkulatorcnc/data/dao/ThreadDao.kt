package com.example.calkulatorcnc.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.calkulatorcnc.entity.ThreadEntity

@Dao
interface ThreadDao {
    @Query("SELECT * FROM threads WHERE category_id = :catId AND table_id = :tabId")
    fun getThreads(catId: Int, tabId: Int): List<ThreadEntity>
}
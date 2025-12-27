package com.example.calkulatorcnc.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.calkulatorcnc.entity.Tool

@Dao
interface ToolDao {
    @Query("SELECT * FROM tools")
    fun getAllTools(): LiveData<List<Tool>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTool(tool: Tool)

    @Update
    suspend fun updateTool(tool: Tool)

    @Delete
    suspend fun deleteTool(tool: Tool)
}
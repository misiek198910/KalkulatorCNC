package com.example.calkulatorcnc.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threads")
data class ThreadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "category_id") val categoryId: Int,
    @ColumnInfo(name = "table_id") val tableId: Int,
    val name: String,
    val pitch: String,
    @ColumnInfo(name = "hole_min") val holeMin: String,
    @ColumnInfo(name = "hole_max") val holeMax: String,
    @ColumnInfo(name = "hole_size") val holeSize: String
)
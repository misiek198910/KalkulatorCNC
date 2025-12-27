package com.example.calkulatorcnc.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tools")
data class Tool(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workpiece: String?,
    val name: String,
    val f: String?,
    val s: String?,
    val notes: String?
)
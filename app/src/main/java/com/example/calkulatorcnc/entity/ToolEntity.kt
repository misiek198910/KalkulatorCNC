package com.example.calkulatorcnc.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "cnc_tools",
    primaryKeys = ["articleNumber", "materialGroup"], // Dopasowanie do SQL
    indices = [
        Index(value = ["toolCategory", "materialGroup", "diameter"], name = "idx_search")
    ]
)
data class ToolEntity(
    val articleNumber: String,
    val toolCategory: String,
    val toolModelName: String,
    val materialGroup: String,
    val diameter: Double,
    val flutesCount: Int,
    val vcMin: Double,
    val vcMax: Double,
    val feedStep: Double
)
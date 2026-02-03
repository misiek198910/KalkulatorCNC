package com.example.calkulatorcnc.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.calkulatorcnc.entity.ToolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolNewDao {

    // 1. Pobieranie średnic dla wybranego materiału i kategorii (Główna kaskada)
    @Query("""
    SELECT DISTINCT diameter 
    FROM cnc_tools 
    WHERE toolCategory = :category 
    AND materialGroup = :material 
    ORDER BY diameter ASC
""")
    fun getDiametersByCategoryAndMaterial(category: String, material: String): Flow<List<Double>>

    // 2. Pobieranie konkretnych modeli dla wybranej średnicy, materiału i kategorii (Główna kaskada)
    @Query("""
    SELECT DISTINCT toolModelName 
    FROM cnc_tools 
    WHERE toolCategory = :category 
    AND materialGroup = :material 
    AND diameter BETWEEN :diameter - 0.001 AND :diameter + 0.001
    ORDER BY toolModelName ASC
""")
    fun getModelsBySpecifics(category: String, material: String, diameter: Double): Flow<List<String>>

    // Pobieranie wszystkich kategorii (używane w Step 1)
    @Query("SELECT DISTINCT toolCategory FROM cnc_tools ORDER BY toolCategory ASC")
    fun getAllCategories(): Flow<List<String>>

    // Pobieranie modeli na podstawie samej kategorii (użyteczność)
    @Query("SELECT DISTINCT toolModelName FROM cnc_tools WHERE toolCategory = :category ORDER BY toolModelName ASC")
    fun getModelsByCategory(category: String): Flow<List<String>>

    // Pobieranie dostępnych średnic dla konkretnego modelu (użyteczność)
    @Query("SELECT DISTINCT diameter FROM cnc_tools WHERE toolModelName = :modelName AND toolCategory = :category ORDER BY diameter ASC")
    fun getDiametersByModel(modelName: String, category: String): Flow<List<Double>>

    // Pobranie parametrów skrawania dla konkretnego narzędzia (Kluczowe dla obliczeń)
    @Query("""
        SELECT * FROM cnc_tools 
        WHERE toolModelName = :modelName 
        AND materialGroup = :materialGroup 
        AND diameter BETWEEN :diameter - 0.001 AND :diameter + 0.001 
        LIMIT 1
    """)
    suspend fun getParams(modelName: String, materialGroup: String, diameter: Double): ToolEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTools(tools: List<ToolEntity>)

    @Transaction
    suspend fun insertAllInTransaction(tools: List<ToolEntity>) {
        insertTools(tools)
    }
    @Query("SELECT COUNT(*) FROM cnc_tools")
    suspend fun getToolsCount(): Int
}
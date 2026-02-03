package com.example.calkulatorcnc.data.repository

import com.example.calkulatorcnc.data.dao.ToolNewDao
import com.example.calkulatorcnc.entity.ToolEntity
import kotlinx.coroutines.flow.Flow

class ToolRepository(private val toolDao: ToolNewDao) {

    val allCategories: Flow<List<String>> = toolDao.getAllCategories()

    fun getDiametersByCategoryAndMaterial(category: String, material: String): Flow<List<Double>> {
        return toolDao.getDiametersByCategoryAndMaterial(category, material)
    }

    fun getModelsBySpecifics(category: String, material: String, diameter: Double): Flow<List<String>> {
        return toolDao.getModelsBySpecifics(category, material, diameter)
    }

    suspend fun getParams(modelName: String, materialGroup: String, diameter: Double): ToolEntity? {
        return toolDao.getParams(modelName, materialGroup, diameter)
    }
}
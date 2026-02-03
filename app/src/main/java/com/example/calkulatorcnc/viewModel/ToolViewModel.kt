package com.example.calkulatorcnc.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calkulatorcnc.data.repository.ToolRepository
import com.example.calkulatorcnc.entity.ToolEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ToolViewModel(private val repository: ToolRepository) : ViewModel() {

    val selectedMaterialGroup = MutableStateFlow("P")
    val selectedCategory = MutableStateFlow<String?>(null)
    val selectedDiameter = MutableStateFlow<Double?>(null)
    val selectedModel = MutableStateFlow<String?>(null)
    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()
    init {
        viewModelScope.launch {
            repository.allCategories.collect { all ->
                _categories.value = all
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val availableDiameters: StateFlow<List<Double>> = combine(
        selectedCategory,
        selectedMaterialGroup
    ) { category, material ->
        if (category != null) {
            repository.getDiametersByCategoryAndMaterial(category, material)
        } else {
            flowOf(emptyList())
        }
    }.flatMapLatest { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val availableModels: StateFlow<List<String>> = combine(
        selectedCategory,
        selectedMaterialGroup,
        selectedDiameter
    ) { category, material, diameter ->
        if (category != null && diameter != null) {
            repository.getModelsBySpecifics(category, material, diameter)
        } else {
            flowOf(emptyList())
        }
    }.flatMapLatest { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Ostateczne Parametry (zaciągane gdy wszystko jest wybrane)
    val toolParameters: StateFlow<ToolEntity?> = combine(
        selectedModel,
        selectedMaterialGroup,
        selectedDiameter
    ) { model, material, diameter ->
        if (model != null && diameter != null) {
            repository.getParams(model, material, diameter)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setIsoGroup(iso: String) {
        selectedMaterialGroup.value = iso
        selectedDiameter.value = null
        selectedModel.value = null
    }

    fun setCategory(category: String) {
        selectedCategory.value = category
        selectedDiameter.value = null
        selectedModel.value = null
    }

    fun setDiameter(diameter: Double) {
        selectedDiameter.value = diameter
        selectedModel.value = null
    }

    fun setModel(model: String) {
        selectedModel.value = model
    }

    fun resetSelection() {
        selectedCategory.value = null
        selectedDiameter.value = null
        selectedModel.value = null
    }
    private val _availableMaterials = MutableStateFlow<List<String>>(emptyList())
}
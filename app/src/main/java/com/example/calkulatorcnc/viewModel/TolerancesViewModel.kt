package com.example.calkulatorcnc.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.calkulatorcnc.data.params.FitResult
import com.example.calkulatorcnc.data.repository.TolerancesRepository

class TolerancesViewModel : ViewModel() {

    private val repository = TolerancesRepository()

    // Stan wejściowy obserwowany dwukierunkowo lub przez Listenery w Activity
    val diameterInput = MutableLiveData<String>("")
    val selectedHole = MutableLiveData<String>("H7")
    val selectedShaft = MutableLiveData<String>("h6")

    private val _fitResult = MutableLiveData<FitResult?>(null)
    val fitResult: LiveData<FitResult?> = _fitResult

    // Listy pobierane raz przy utworzeniu ViewModelu
    val availableHoles: List<String> = repository.getHoleLabels()
    val availableShafts: List<String> = repository.getShaftLabels()

    fun calculate() {
        // Konwersja z uwzględnieniem lokalizacji (obsługa kropki i przecinka)
        val diameter = diameterInput.value?.replace(',', '.')?.toDoubleOrNull()

        // Walidacja zakresu standardu ISO 286 (zwykle do 500mm lub 3150mm zależnie od tabel)
        if (diameter == null || diameter <= 0 || diameter > 500) {
            _fitResult.value = null
            return
        }

        val hole = selectedHole.value ?: "H7"
        val shaft = selectedShaft.value ?: "h6"

        // Przekazanie obliczeń do warstwy danych
        _fitResult.value = repository.calculateFit(diameter, hole, shaft)
    }

    fun reset() {
        diameterInput.value = ""
        _fitResult.value = null
    }
}
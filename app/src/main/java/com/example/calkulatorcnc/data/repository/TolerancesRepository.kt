package com.example.calkulatorcnc.data.repository

import com.example.calkulatorcnc.data.params.*

class TolerancesRepository {

    private val allTolerances = ToleranceData.getIsoTolerances()

    fun getHoleLabels(): List<String> = allTolerances.filter { it.isHole }.map { it.label }

    fun getShaftLabels(): List<String> = allTolerances.filter { !it.isHole }.map { it.label }

    fun calculateFit(diameter: Double, holeLabel: String, shaftLabel: String): FitResult? {
        // Dodano jawne wskazanie czy szukamy otworu (true) czy wałka (false)
        val holeDevs = findDeviations(holeLabel, diameter, true) ?: return null
        val shaftDevs = findDeviations(shaftLabel, diameter, false) ?: return null

        // Wartości wejściowe: first = upperUm, second = lowerUm
        val hMax = diameter + (holeDevs.first / 1000.0)
        val hMin = diameter + (holeDevs.second / 1000.0)
        val sMax = diameter + (shaftDevs.first / 1000.0)
        val sMin = diameter + (shaftDevs.second / 1000.0)

        // Luz minimalny (H_min - S_max) i maksymalny (H_max - S_min)
        val minClearance = hMin - sMax
        val maxClearance = hMax - sMin

        val type = when {
            minClearance >= 0 -> FitType.CLEARANCE    // Zawsze występuje luz
            maxClearance <= 0 -> FitType.INTERFERENCE // Zawsze występuje wcisk
            else -> FitType.TRANSITION               // Pasowanie mieszane
        }

        return FitResult(hMin, hMax, sMin, sMax, type, maxClearance, minClearance)
    }

    private fun findDeviations(label: String, diameter: Double, isHole: Boolean): Pair<Double, Double>? {
        // Dodano filtrowanie po isHole dla bezpieczeństwa danych
        val tolerance = allTolerances.find {
            it.label.equals(label, ignoreCase = true) && it.isHole == isHole
        }
        val value = tolerance?.values?.find { diameter > it.minDia && diameter <= it.maxDia }
        return value?.let { Pair(it.upperUm, it.lowerUm) }
    }
}
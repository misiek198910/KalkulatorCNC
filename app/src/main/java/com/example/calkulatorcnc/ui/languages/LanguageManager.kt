package com.example.calkulatorcnc.ui.languages

object LanguageManager {
    val supportedLanguages = listOf(
        Language("Polski", "pl"),
        Language("English", "en"),
        Language("Deutsch", "de"),
        Language("Italiano", "it"),
        Language("Español", "es"),
        Language("Français", "fr")
    )

    fun getIsoCodeByIndex(index: Int): String = supportedLanguages.getOrNull(index)?.isoCode ?: "en"

    fun getIndexByIsoCode(isoCode: String): Int {
        val index = supportedLanguages.indexOfFirst { it.isoCode == isoCode }
        return if (index != -1) index else 1 // Domyślnie English
    }
}
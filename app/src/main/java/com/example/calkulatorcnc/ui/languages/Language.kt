package com.example.calkulatorcnc.ui.languages

data class Language(
    val name: String,     // Nazwa wyświetlana w dialogu, np. "Deutsch"
    val isoCode: String,  // Kod ISO, np. "de"
    val iconRes: Int? = null // Opcjonalnie: ikona flagi, jeśli będziesz chciał dodać w przyszłości
)
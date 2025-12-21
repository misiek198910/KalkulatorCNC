package com.example.calkulatorcnc.billing

enum class SubscriptionStatus {
    CHECKING,    // Trwa sprawdzanie statusu w Google Play
    PREMIUM,     // Użytkownik posiada aktywną subskrypcję
    NON_PREMIUM  // Użytkownik nie posiada subskrypcji
}
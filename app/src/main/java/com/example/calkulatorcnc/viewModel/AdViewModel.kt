package com.example.calkulatorcnc.viewModel

import androidx.lifecycle.ViewModel

class AdViewModel : ViewModel() {
    private var adShownOnExit = false
    var isAdLoading = false
    var isInternalNavigation = false
    fun triggerExitAd(onShowAd: () -> Unit) {
        adShownOnExit = true
        onShowAd()
    }

    fun triggerResumeAd(onShowAd: () -> Unit) {
        if (!adShownOnExit && !isInternalNavigation) {
            onShowAd()
        } else {
            // Resetujemy obie flagi
            adShownOnExit = false
            isInternalNavigation = false
        }
    }
}
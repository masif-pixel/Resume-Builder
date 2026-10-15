package com.resume.builder.presentation.main

import androidx.lifecycle.ViewModel
import com.resume.builder.domain.model.ThemeMode
import com.resume.builder.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.StateFlow

/** Activity-scoped state shared by the whole app: theme and startup routing. */
class AppViewModel(
    private val prefs: PreferencesRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
    val isOnboardingDone: Boolean get() = prefs.isOnboardingDone()
}

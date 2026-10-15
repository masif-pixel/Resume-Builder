package com.resume.builder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelforge.ads.AdsSdk
import com.pixelforge.ads.appopen.AppOpenAdManager
import com.resume.builder.presentation.main.AppViewModel
import com.resume.builder.presentation.navigation.AppNavHost
import com.resume.builder.ui.theme.ResumeBuilderTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModel()
    private val adsSdk: AdsSdk by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Block app-open before first composition / ProcessLifecycle ON_START race; Home lifts it.
        adsSdk.appOpen.setSuppressed(true, AppOpenAdManager.REASON_SPLASH)
        val startOnboarding = !appViewModel.isOnboardingDone
        setContent {
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            ResumeBuilderTheme(themeMode = themeMode) {
                AppNavHost(startOnboarding = startOnboarding)
            }
        }
    }
}

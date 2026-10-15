package com.resume.builder.presentation.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.pixelforge.ads.AdsSdk
import com.pixelforge.ads.interstitial.InterstitialAdManager
import com.resume.builder.presentation.ai.AiResumeScreen
import com.resume.builder.presentation.components.LocalOpenPdf
import com.resume.builder.presentation.coverletter.CoverLetterEditorScreen
import com.resume.builder.presentation.coverletter.CoverLetterGenerateScreen
import com.resume.builder.presentation.coverletter.CoverLetterPreviewScreen
import com.resume.builder.presentation.editor.ResumeEditorScreen
import com.resume.builder.presentation.editor.SectionEditorScreen
import com.resume.builder.presentation.main.MainActions
import com.resume.builder.presentation.main.MainScreen
import com.resume.builder.presentation.onboarding.OnboardingScreen
import com.resume.builder.presentation.pdftools.PdfToolScreen
import com.resume.builder.presentation.pdftools.PdfToolsScreen
import com.resume.builder.presentation.preview.ResumePreviewScreen
import com.resume.builder.presentation.settings.LegalScreen
import com.resume.builder.presentation.settings.SettingsScreen
import com.resume.builder.presentation.splash.SplashScreen
import com.resume.builder.presentation.templates.TemplatePickerScreen
import com.resume.builder.presentation.viewer.PdfViewerScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AppNavHost(startOnboarding: Boolean, navController: NavHostController = rememberNavController()) {
    val adsSdk: AdsSdk = koinInject()
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val openSaved: () -> Unit = {
        navController.navigate(Route.Main(MainTab.SAVED)) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }
    val openPdf: (String, String) -> Unit = remember(navController) {
        { path, title -> navController.navigate(Route.PdfViewer(path, title)) { launchSingleTop = true } }
    }
    CompositionLocalProvider(LocalOpenPdf provides openPdf) {
    NavHost(
        navController = navController,
        startDestination = Route.Splash,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) + fadeIn(tween(280)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) + fadeOut(tween(260)) },
    ) {
        composable<Route.Splash>(enterTransition = { fadeIn(tween(0)) }) {
            SplashScreen(
                onFinished = {
                    val next: Route = if (startOnboarding) Route.Onboarding else Route.Main()
                    navController.navigate(next) { popUpTo<Route.Splash> { inclusive = true } }
                },
            )
        }
        composable<Route.Onboarding> {
            var leaving by remember { mutableStateOf(false) }
            OnboardingScreen(
                onFinished = {
                    if (leaving) return@OnboardingScreen
                    leaving = true
                    scope.launch {
                        // One-shot per process: skipped when the splash interstitial already showed.
                        activity?.let { adsSdk.interstitial.showOrSkip(it, InterstitialAdManager.SCREEN_ONBOARDING) }
                        navController.navigate(Route.Main()) { popUpTo<Route.Onboarding> { inclusive = true } }
                    }
                },
                onLegal = { navController.navigate(Route.Legal(it)) },
            )
        }
        composable<Route.Main> { entry ->
            val route = entry.toRoute<Route.Main>()
            MainScreen(
                initialTab = route.tab,
                actions = MainActions(
                    openEditor = { navController.navigate(Route.Editor(it)) },
                    openPreview = { navController.navigate(Route.Preview(it)) },
                    openTemplatePicker = { navController.navigate(Route.TemplatePicker(it)) },
                    openAiResume = { navController.navigate(Route.AiResume) },
                    openCoverLetterGenerate = { navController.navigate(Route.CoverLetterGenerate(it)) },
                    openCoverLetterEditor = { letterId, resumeId -> navController.navigate(Route.CoverLetterEditor(letterId, resumeId)) },
                    openCoverLetterPreview = { navController.navigate(Route.CoverLetterPreview(it)) },
                    openPdfTools = { navController.navigate(Route.PdfTools) },
                    openSettings = { navController.navigate(Route.Settings) },
                ),
            )
        }
        composable<Route.Editor> { entry ->
            val route = entry.toRoute<Route.Editor>()
            ResumeEditorScreen(
                resumeId = route.resumeId,
                onBack = { navController.popBackStack() },
                onEditSection = { navController.navigate(Route.SectionEditor(route.resumeId, it)) },
                onPreview = { navController.navigate(Route.Preview(route.resumeId)) },
            )
        }
        composable<Route.SectionEditor> { entry ->
            val route = entry.toRoute<Route.SectionEditor>()
            SectionEditorScreen(resumeId = route.resumeId, sectionId = route.sectionId, onBack = { navController.popBackStack() })
        }
        composable<Route.Preview> { entry ->
            val route = entry.toRoute<Route.Preview>()
            ResumePreviewScreen(
                resumeId = route.resumeId,
                onBack = { navController.popBackStack() },
                onEditContent = { navController.navigate(Route.Editor(route.resumeId)) { launchSingleTop = true } },
                onOpenSaved = openSaved,
            )
        }
        composable<Route.TemplatePicker> { entry ->
            val route = entry.toRoute<Route.TemplatePicker>()
            TemplatePickerScreen(
                resumeId = route.resumeId,
                onBack = { navController.popBackStack() },
                onCreated = { id ->
                    navController.navigate(Route.Editor(id)) { popUpTo<Route.TemplatePicker> { inclusive = true } }
                },
            )
        }
        composable<Route.AiResume> {
            AiResumeScreen(
                onBack = { navController.popBackStack() },
                onCreated = { id -> navController.navigate(Route.Editor(id)) { popUpTo<Route.AiResume> { inclusive = true } } },
            )
        }
        composable<Route.CoverLetterGenerate> { entry ->
            val route = entry.toRoute<Route.CoverLetterGenerate>()
            CoverLetterGenerateScreen(
                resumeId = route.resumeId,
                onBack = { navController.popBackStack() },
                onGenerated = { letterId ->
                    navController.navigate(Route.CoverLetterEditor(letterId = letterId)) {
                        popUpTo<Route.CoverLetterGenerate> { inclusive = true }
                    }
                },
            )
        }
        composable<Route.CoverLetterEditor> { entry ->
            val route = entry.toRoute<Route.CoverLetterEditor>()
            CoverLetterEditorScreen(
                letterId = route.letterId,
                resumeId = route.resumeId,
                onBack = { navController.popBackStack() },
                onPreview = { navController.navigate(Route.CoverLetterPreview(it)) },
            )
        }
        composable<Route.CoverLetterPreview> { entry ->
            val route = entry.toRoute<Route.CoverLetterPreview>()
            CoverLetterPreviewScreen(letterId = route.letterId, onBack = { navController.popBackStack() }, onOpenSaved = openSaved)
        }
        composable<Route.PdfTools> {
            PdfToolsScreen(onBack = { navController.popBackStack() }, onTool = { navController.navigate(Route.PdfTool(it.name)) })
        }
        composable<Route.PdfTool> { entry ->
            val route = entry.toRoute<Route.PdfTool>()
            PdfToolScreen(toolName = route.tool, onBack = { navController.popBackStack() }, onOpenSaved = openSaved)
        }
        composable<Route.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLegal = { navController.navigate(Route.Legal(it)) },
            )
        }
        composable<Route.Legal> { entry ->
            LegalScreen(type = entry.toRoute<Route.Legal>().type, onBack = { navController.popBackStack() })
        }
        composable<Route.PdfViewer> { entry ->
            val route = entry.toRoute<Route.PdfViewer>()
            PdfViewerScreen(path = route.path, title = route.title, onBack = { navController.popBackStack() })
        }
    }
    }
}

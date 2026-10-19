package com.resume.builder.presentation.main

import androidx.compose.ui.res.stringResource
import com.resume.builder.R
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.annotation.StringRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelforge.ads.AdsSdk
import com.pixelforge.ads.appopen.AppOpenAdManager
import com.pixelforge.ads.common.hasValidatedNetwork
import com.pixelforge.ads.nativead.NativeAdSlot
import com.pixelforge.ads.nativead.NativeAdTemplate
import com.pixelforge.ads.remoteconfig.AdFormat
import com.resume.builder.BuildConfig
import com.resume.builder.core.update.ForceUpdateGate
import com.resume.builder.core.update.PlayForceUpdate
import com.resume.builder.domain.repository.PreferencesRepository
import com.resume.builder.presentation.components.AppLanguageDialog
import com.resume.builder.presentation.components.BrandLogo
import com.resume.builder.presentation.components.currentAppLanguageName
import com.resume.builder.presentation.components.FileActions
import com.resume.builder.presentation.coverletter.CoverLettersTab
import com.resume.builder.presentation.home.HomeTab
import com.resume.builder.presentation.navigation.MainTab
import com.resume.builder.presentation.saved.SavedFilesTab
import com.resume.builder.presentation.templates.TemplatesTab
import com.resume.builder.ui.theme.Gradients
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val TAG_AD_FREE = "TAG_AD_FREE"

/** Let the splash cross-fade into Home before the promo sheet slides up. */
private const val ADS_FREE_SHEET_DELAY_MS = 600L

/** The ads-free promo opens by itself at most once per process. */
private val autoOpenedAdsFreeSheetThisProcess = AtomicBoolean(false)

data class MainActions(
    val openEditor: (String) -> Unit,
    val openPreview: (String) -> Unit,
    val openTemplatePicker: (String?) -> Unit,
    val openAiResume: () -> Unit,
    val openCoverLetterGenerate: (String) -> Unit,
    val openCoverLetterEditor: (letterId: String?, resumeId: String?) -> Unit,
    val openCoverLetterPreview: (String) -> Unit,
    val openPdfTools: () -> Unit,
    val openSettings: () -> Unit,
)

private data class TabItem(@StringRes val label: Int, val selected: ImageVector, val unselected: ImageVector)

private val tabs = listOf(
    TabItem(R.string.tab_resumes, Icons.Rounded.Description, Icons.Outlined.Description),
    TabItem(R.string.tab_cover_letters, Icons.Rounded.Mail, Icons.Outlined.Mail),
    TabItem(R.string.tab_templates, Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    TabItem(R.string.tab_saved, Icons.Rounded.FolderOpen, Icons.Outlined.FolderOpen),
)

@Composable
fun MainScreen(
    initialTab: Int,
    actions: MainActions,
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    val activity = LocalActivity.current
    var showExit by rememberSaveable { mutableStateOf(false) }
    val adsSdk: AdsSdk = koinInject()
    val context = LocalContext.current

    val preferences: PreferencesRepository = koinInject()
    val adFreeUntil by preferences.adFreeUntilEpochMs.collectAsStateWithLifecycle()
    var adFreeActive by remember { mutableStateOf(adFreeUntil > System.currentTimeMillis()) }
    // Keep the AdsSdk gate in sync with the persisted window, and lift it again the moment the
    // window runs out so app-open and the home interstitial come back without a relaunch.
    LaunchedEffect(adFreeUntil) {
        adsSdk.syncAdFreeUntil(adFreeUntil)
        adFreeActive = adFreeUntil > System.currentTimeMillis()
        if (adFreeActive) {
            delay(adFreeUntil - System.currentTimeMillis())
            adsSdk.syncAdFreeUntil(adFreeUntil)
            adFreeActive = false
        }
    }

    // Home live: app-open may show on the next warm start.
    LaunchedEffect(Unit) {
        adsSdk.appOpen.setSuppressed(false, AppOpenAdManager.REASON_SPLASH)
        adsSdk.appOpen.ensurePreloaded()
    }
    // Home interstitial preload + cooldown, only while ads are on.
    LaunchedEffect(adFreeActive) {
        if (!adFreeActive) adsSdk.prepareBottomNavWhenReady()
    }

    // Play force update, asked once Home is on screen. The flow waits until the activity is
    // resumed, so it never starts under the splash interstitial; cancelling Play's UI brings it
    // straight back until the update is installed.
    var forceUpdateGate by remember {
        mutableStateOf(if (forceUpdateClearedThisProcess) ForceUpdateGate.Clear else ForceUpdateGate.Checking)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        if (forceUpdateClearedThisProcess || activity == null) return@LaunchedEffect
        val forceUpdate = PlayForceUpdate(activity)
        if (!forceUpdate.isImmediateUpdateRequired()) {
            forceUpdateGate = ForceUpdateGate.Clear
            forceUpdateClearedThisProcess = true
            return@LaunchedEffect
        }
        forceUpdateGate = ForceUpdateGate.Required
        adsSdk.appOpen.setSuppressed(true, REASON_FORCE_UPDATE)
        try {
            forceUpdate.blockUntilUpdated(lifecycle)
            forceUpdateGate = ForceUpdateGate.Clear
            forceUpdateClearedThisProcess = true
        } finally {
            adsSdk.appOpen.setSuppressed(false, REASON_FORCE_UPDATE)
        }
    }

    var showAdsFreeSheet by remember { mutableStateOf(false) }
    val adsFreeLoading = remember { mutableStateOf(false) }
    val adsFreeSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { new -> !(adsFreeLoading.value && new == SheetValue.Hidden) },
    )
    // A mandatory update owns the screen: the ads-free sheet must not sit on top of Play's prompt.
    val currentForceUpdateGate by rememberUpdatedState(forceUpdateGate)
    LaunchedEffect(forceUpdateGate) {
        if (forceUpdateGate == ForceUpdateGate.Required) {
            adsFreeLoading.value = false
            showAdsFreeSheet = false
        }
    }
    // Once per process, after splash: offer 2h ads-free unless already active.
    LaunchedEffect(Unit) {
        if (autoOpenedAdsFreeSheetThisProcess.get()) return@LaunchedEffect
        delay(ADS_FREE_SHEET_DELAY_MS)
        adsSdk.initialized.first { it }
        // Not on top of a mandatory update; left un-marked so a later Home can still offer it.
        snapshotFlow { currentForceUpdateGate }.first { it != ForceUpdateGate.Checking }
        if (currentForceUpdateGate == ForceUpdateGate.Required) {
            Log.i(TAG_AD_FREE, "ads_free_sheet_skip_force_update")
            return@LaunchedEffect
        }
        if (!isActive || autoOpenedAdsFreeSheetThisProcess.get()) return@LaunchedEffect
        // Offline: a rewarded ad cannot load, so do not offer one. Left un-marked so the next
        // Home visit with a connection can still offer it.
        if (!context.hasValidatedNetwork()) {
            Log.i(TAG_AD_FREE, "ads_free_sheet_skip_offline")
            return@LaunchedEffect
        }
        autoOpenedAdsFreeSheetThisProcess.set(true)
        when {
            !adsSdk.isAdsMasterOn() -> Log.i(TAG_AD_FREE, "ads_free_sheet_skip_master_off")
            !adsSdk.canShow(AdFormat.REWARDED_INTERSTITIAL) -> Log.i(TAG_AD_FREE, "ads_free_sheet_skip_rewarded_off")
            preferences.adFreeUntilEpochMs.value > System.currentTimeMillis() ->
                Log.i(TAG_AD_FREE, "ads_free_sheet_skip_active")
            else -> {
                showAdsFreeSheet = true
                Log.i(TAG_AD_FREE, "ads_free_sheet_auto_open")
            }
        }
    }

    if (showAdsFreeSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!adsFreeLoading.value) {
                    showAdsFreeSheet = false
                    adsFreeLoading.value = false
                }
            },
            sheetState = adsFreeSheetState,
            sheetGesturesEnabled = !adsFreeLoading.value,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            AdsFreeBottomSheet(
                onDismiss = {
                    adsFreeLoading.value = false
                    showAdsFreeSheet = false
                },
                onLoadingChange = { adsFreeLoading.value = it },
            )
        }
    }

    // Back: close the drawer, then return to the Resumes tab, then ask before leaving the app.
    BackHandler {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            tab != MainTab.RESUMES -> tab = MainTab.RESUMES
            else -> showExit = true
        }
    }

    if (showExit) {
        ExitAppSheet(
            onStay = { showExit = false },
            onExit = {
                showExit = false
                // Wipe every ad cache and end the process so the next launch starts clean.
                activity?.let { adsSdk.shutdownForExit(it) }
            },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                drawerState = drawerState,
                actions = actions,
                adFreeUntil = adFreeUntil,
                showAdFreeEntry = adsSdk.isAdsMasterOn(),
                onOpenAdFree = { showAdsFreeSheet = true },
            )
        },
    ) {
        Scaffold(
            bottomBar = {
                Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                NativeAdSlot(
                    placement = AdFormat.NATIVE_HOME,
                    template = NativeAdTemplate.SMALL,
                    screen = "Home",
                    // Loaded once; leaving Home for another screen and coming back reuses it.
                    retain = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 6.dp),
                )
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    tabs.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = {
                                if (tab == index) return@NavigationBarItem
                                scope.launch {
                                    // Home interstitial: Remote Config cooldown decides if it is due.
                                    activity?.let { adsSdk.interstitial.showBottomNavOrSkip(it) }
                                    tab = index
                                }
                            },
                            icon = { Icon(if (tab == index) item.selected else item.unselected, contentDescription = stringResource(item.label)) },
                            label = { Text(stringResource(item.label), maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
                }
            },
            snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
                AnimatedContent(targetState = tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tabs") { current ->
                    when (current) {
                        0 -> HomeTab(actions = actions, onOpenDrawer = openDrawer, snackbar = snackbar)
                        1 -> CoverLettersTab(actions = actions, onOpenDrawer = openDrawer, snackbar = snackbar)
                        2 -> TemplatesTab(actions = actions, onOpenDrawer = openDrawer)
                        else -> SavedFilesTab(onOpenDrawer = openDrawer, snackbar = snackbar)
                    }
                }
            }
        }
    }

    // A mandatory update leaves no usable app: swallow taps (bottom bar included) and Back
    // between Play's prompts. Composed last, so this BackHandler outranks the one above.
    if (forceUpdateGate == ForceUpdateGate.Required) {
        BackHandler { /* Wait for the update. */ }
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        )
    }
}

/** App-open hold while Play's mandatory update UI owns the screen. */
private const val REASON_FORCE_UPDATE = "force_update"

/** Play said no update is required this process; re-entering Home does not ask again. */
private var forceUpdateClearedThisProcess = false

@Composable
private fun AppDrawer(
    drawerState: DrawerState,
    actions: MainActions,
    adFreeUntil: Long,
    showAdFreeEntry: Boolean,
    onOpenAdFree: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showLanguage by remember { mutableStateOf(false) }
    if (showLanguage) AppLanguageDialog(onDismiss = { showLanguage = false })
    val context = LocalContext.current
    fun closeThen(block: () -> Unit) {
        scope.launch { drawerState.close() }
        block()
    }
    // Remaining ads-free time: re-read on every drawer open, then ticks while it stays open.
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(drawerState.isOpen, adFreeUntil) {
        nowMs = System.currentTimeMillis()
        while (drawerState.isOpen && adFreeUntil > System.currentTimeMillis()) {
            delay(30_000)
            nowMs = System.currentTimeMillis()
        }
    }
    // M3 caps the sheet at 360dp, which leaves a gap on wider phones; open it full width everywhere.
    ModalDrawerSheet(modifier = Modifier.fillMaxWidth(), drawerContainerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            AppHeader()
            DrawerItem(stringResource(R.string.drawer_pdf_tools), Icons.Rounded.PictureAsPdf) { closeThen(actions.openPdfTools) }
            DrawerItem(stringResource(R.string.drawer_ai_resume), Icons.Rounded.AutoAwesome) { closeThen(actions.openAiResume) }
            DrawerItem(stringResource(R.string.drawer_settings), Icons.Rounded.Settings) { closeThen(actions.openSettings) }
            DrawerItem(stringResource(R.string.app_language), Icons.Rounded.Translate, badge = currentAppLanguageName()) { showLanguage = true }
            if (showAdFreeEntry) {
                val adFreeActive = adFreeUntil > nowMs
                NavigationDrawerItem(
                    label = {
                        Column {
                            Text(stringResource(R.string.drawer_ads_free))
                            Text(
                                if (adFreeActive) adFreeRemainingLabel(adFreeUntil - nowMs)
                                else stringResource(R.string.drawer_ads_free_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (adFreeActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    icon = { Icon(Icons.Rounded.Movie, null) },
                    selected = false,
                    onClick = { closeThen(onOpenAdFree) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            DrawerItem(stringResource(R.string.drawer_rate), Icons.Rounded.Star) { closeThen { FileActions.rateApp(context) } }
            DrawerItem(stringResource(R.string.drawer_feedback), Icons.Rounded.Feedback) { closeThen { FileActions.sendFeedback(context, BuildConfig.VERSION_NAME) } }
            DrawerItem(stringResource(R.string.drawer_share), Icons.Rounded.Share) { closeThen { FileActions.shareApp(context) } }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                BrandLogo(size = 22.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.drawer_version, stringResource(R.string.app_name), BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Box(
        Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(24.dp)).background(Gradients.hero).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                BrandLogo(size = 44.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.drawer_tagline), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.2f)) {
                    Text(
                        "v${BuildConfig.VERSION_NAME}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(label: String, icon: ImageVector, badge: String? = null, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, null) },
        selected = false,
        onClick = onClick,
        badge = badge?.let {
            {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
        },
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

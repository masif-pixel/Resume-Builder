package com.resume.builder.presentation.splash

import androidx.compose.ui.res.stringResource
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.pixelforge.ads.AdsSdk
import com.pixelforge.ads.remoteconfig.AdFormat
import com.pixelforge.ads.remoteconfig.AdsRemoteConfig
import com.resume.builder.R
import com.resume.builder.ui.theme.Gradients
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.koinInject

private const val TAG_SPLASH = "TAG_SPLASH"

/** How long the branded screen is held at minimum, even when everything is ready earlier. */
const val SPLASH_HOLD_MILLIS = 1800L

/**
 * Hard wall-clock budget for everything splash waits on — Remote Config (itself capped at 15s)
 * and the splash interstitial load, all of it. When this expires the app moves on with whatever
 * finished; nothing on splash is allowed to outlive it. The force-update check runs on Home,
 * outside this budget.
 */
const val SPLASH_MAX_MILLIS = 17_000L

/**
 * Margin kept at the end of the splash budget. A show is only started with at least this much
 * left, so an interstitial can never appear after the splash has handed over to the app.
 */
private const val SPLASH_SHOW_RESERVE_MS = 2_000L

/** Progress catch-up once the wait actually ends (ad ready, or work finished early). */
private const val PROGRESS_SNAP_MILLIS = 260

/**
 * Value of a Deferred that is known to be complete, or null if it failed or was cancelled.
 * `getCompleted()` rethrows on those, which on splash would take the whole launch down.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun <T> Deferred<T>.getCompletedOrNull(): T? =
    if (isCompleted && !isCancelled) runCatching { getCompleted() }.getOrNull() else null

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? ComponentActivity
    val adsSdk: AdsSdk = koinInject()
    val currentOnFinished by rememberUpdatedState(onFinished)
    // Adaptive launcher icons can't go through painterResource, so render the real app icon via PackageManager.
    val icon = remember {
        context.packageManager.getApplicationIcon(context.applicationInfo).toBitmap(512, 512).asImageBitmap()
    }
    // Determinate: the bar paces itself against the one budget splash is bound by, and snaps to
    // the end the moment the wait is over instead of crawling out the rest of it.
    val progress = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.85f) }
    var finishing by remember { mutableStateOf(false) }

    // Splash owns Back while it is up; the ad gate below decides when it leaves.
    BackHandler { /* Wait it out. */ }

    LaunchedEffect(Unit) {
        launch { iconScale.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
        launch { progress.animateTo(1f, tween(SPLASH_MAX_MILLIS.toInt(), easing = LinearEasing)) }
    }
    LaunchedEffect(finishing) {
        if (finishing) progress.animateTo(1f, tween(PROGRESS_SNAP_MILLIS, easing = LinearEasing))
    }

    LaunchedEffect(Unit) {
        val host = activity
        if (host == null) {
            delay(SPLASH_HOLD_MILLIS)
            currentOnFinished()
            return@LaunchedEffect
        }
        val startedAt = SystemClock.elapsedRealtime()
        fun remaining(): Long =
            (SPLASH_MAX_MILLIS - (SystemClock.elapsedRealtime() - startedAt)).coerceAtLeast(0L)

        // Load gate: Mobile Ads init, and nothing else. The Remote Config fetch runs alongside
        // this rather than in front of it; cached RC values are enough to decide whether to
        // *request* an ad. See the show gate below for the rest.
        // On the activity scope, not this composition: an ad that lands after splash has moved
        // on must keep loading so the first Home interstitial can reuse it.
        val splashLoad = host.lifecycleScope.async(Dispatchers.Default) {
            // Whichever lands first: GMA is up (load now), or bootstrap finished without it —
            // every placement off — so there is nothing to load.
            val gmaUsable = combine(
                adsSdk.mobileAdsReady,
                adsSdk.initialized
            ) { gma, bootstrapped ->
                if (gma) true else if (bootstrapped) false else null
            }.first { it != null } == true
            if (!gmaUsable) return@async false
            // Cached RC can only say "worth requesting", never "not worth it": the values are
            // the previous launch's, so a placement switched back on in the console still reads
            // off here. Before giving up, wait for this launch's fetch and decide again.
            if (!adsSdk.canShow(AdFormat.INTER_SPLASH)) {
                withTimeoutOrNull(AdsRemoteConfig.FETCH_TIMEOUT_MS) {
                    adsSdk.remoteConfigReady.first { it }
                }
                if (!adsSdk.canShow(AdFormat.INTER_SPLASH)) return@async false
            }
            adsSdk.interstitial.loadSplash()
        }
        // One budget for the whole wait, so separate stage timeouts can never stack up.
        withTimeoutOrNull(remaining()) { splashLoad.join() }

        // Whatever the ad did, nothing past splash runs on stale Remote Config: a console change
        // (ads off, a placement flipped) must decide this session, not the next.
        val rcWait = minOf(remaining(), AdsRemoteConfig.FETCH_TIMEOUT_MS)
        if (withTimeoutOrNull(rcWait) { adsSdk.remoteConfigReady.first { it } } == null) {
            Log.w(
                TAG_SPLASH,
                "Remote Config not answered after " +
                    "${SystemClock.elapsedRealtime() - startedAt}ms — using cached values"
            )
        }

        val loaded = splashLoad.getCompletedOrNull() == true
        if (!loaded) {
            // Left running on purpose — an ad that lands late is reused on the first Home
            // interstitial rather than thrown away.
            Log.w(
                TAG_SPLASH,
                "no splash ad after ${SystemClock.elapsedRealtime() - startedAt}ms " +
                    "(budget ${SPLASH_MAX_MILLIS}ms) — moving on"
            )
        }
        // Show gate. The load above was allowed to trust cached Remote Config; nothing is
        // *shown* until the fetch has actually answered, so a freshly flipped RC key still wins.
        val authorised = loaded &&
            withTimeoutOrNull(remaining()) { adsSdk.initialized.first { it } } != null &&
            adsSdk.canShow(AdFormat.INTER_SPLASH)
        // Splash backgrounded before the ad was ready: wait for the user to come back, but only
        // inside the budget, and never start a show without SPLASH_SHOW_RESERVE_MS in hand.
        val canShowNow = authorised &&
            remaining() > SPLASH_SHOW_RESERVE_MS &&
            withTimeoutOrNull(remaining() - SPLASH_SHOW_RESERVE_MS) {
                while (!host.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    delay(50)
                }
            } != null
        if (canShowNow) {
            finishing = true
            adsSdk.interstitial.showLoadedSplash(host, appearTimeoutMs = remaining())
        } else {
            adsSdk.interstitial.markSplashMissed()
        }
        finishing = true
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        if (elapsed < SPLASH_HOLD_MILLIS) {
            delay(SPLASH_HOLD_MILLIS - elapsed)
        }
        currentOnFinished()
    }

    Box(
        Modifier.fillMaxSize()
            // Follow the app's theme setting (not just the system one): dark schemes have a dark background.
            .background(if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Gradients.onboardingDark else Gradients.onboarding),
    ) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center).size(120.dp).scale(iconScale.value).clip(RoundedCornerShape(28.dp)),
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 48.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.splash_ad_notice),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

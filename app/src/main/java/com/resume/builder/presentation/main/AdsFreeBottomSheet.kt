package com.resume.builder.presentation.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelforge.ads.AdsSdk
import com.pixelforge.ads.adfree.AdFreeSession
import com.pixelforge.ads.common.AdShowResult
import com.pixelforge.ads.common.findActivity
import com.resume.builder.core.AppScope
import com.resume.builder.domain.repository.PreferencesRepository
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

private const val TAG_AD_FREE = "TAG_AD_FREE"

private enum class AdsFreeSheetPhase {
    Ready,
    Loading,
}

/** "2 hours 5 minutes left", rounded up to the next minute so it never reads "0 minutes". */
fun adFreeRemainingLabel(remainMs: Long): String {
    val totalMinutes = ((remainMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
    return "${totalMinutes / 60} hours ${totalMinutes % 60} minutes left"
}

/**
 * Home "Go Ads-Free" promo sheet: watch one rewarded interstitial, get 2 hours without ads.
 *
 * Reward grant runs on [AppScope] so closing the sheet (on show/impression) cannot cancel it.
 */
@Composable
fun AdsFreeBottomSheet(
    onDismiss: () -> Unit,
    onLoadingChange: (Boolean) -> Unit = {},
    preferences: PreferencesRepository = koinInject(),
    adsSdk: AdsSdk = koinInject(),
) {
    val colors = MaterialTheme.colorScheme
    val accent = colors.primary
    val context = LocalContext.current
    val host = remember(context) { context.findActivity() }
    var phase by remember { mutableStateOf(AdsFreeSheetPhase.Ready) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val canDismiss = phase != AdsFreeSheetPhase.Loading
    val adFreeUntil by preferences.adFreeUntilEpochMs.collectAsStateWithLifecycle()
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val adFreeActive = adFreeUntil > nowMs
    LaunchedEffect(adFreeUntil) {
        while (adFreeUntil > System.currentTimeMillis()) {
            nowMs = System.currentTimeMillis()
            delay(30_000)
        }
        nowMs = System.currentTimeMillis()
    }

    LaunchedEffect(canDismiss) {
        onLoadingChange(!canDismiss)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
        ) {
            AdsFreeHeroIllustration(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
            )
            IconButton(
                onClick = { if (canDismiss) onDismiss() },
                enabled = canDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .zIndex(2f)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = if (canDismiss) colors.onSurfaceVariant else colors.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Go Ads-Free",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Watch one short video and enjoy Resume Builder without ads for 2 hours.",
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        )

        Spacer(modifier = Modifier.height(22.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdsFreeBenefitRow("No interstitial ads")
            AdsFreeBenefitRow("Build resumes without interruptions")
            AdsFreeBenefitRow("Free for 2 hours")
        }

        Spacer(modifier = Modifier.height(28.dp))

        when (phase) {
            AdsFreeSheetPhase.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = accent,
                        trackColor = accent.copy(alpha = 0.18f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Loading your reward…",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            AdsFreeSheetPhase.Ready -> {
                if (adFreeActive) {
                    Text(
                        text = adFreeRemainingLabel(adFreeUntil - nowMs),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Close",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurfaceVariant
                        )
                    }
                } else {
                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            color = colors.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 28.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(accent)
                            .clickable {
                                val act = host
                                if (act == null) {
                                    errorMessage = "Unable to show ad"
                                    return@clickable
                                }
                                phase = AdsFreeSheetPhase.Loading
                                errorMessage = null
                                val sheetClosed = AtomicBoolean(false)
                                fun closeSheetOnce() {
                                    if (!sheetClosed.compareAndSet(false, true)) return
                                    AppScope.launch {
                                        withContext(Dispatchers.Main.immediate) { onDismiss() }
                                    }
                                }
                                // AppScope: sheet may close on show/impression before reward is granted.
                                AppScope.launch {
                                    adsSdk.rewardedInterstitial.loadAndShow(act) { result ->
                                        when (result) {
                                            is AdShowResult.Rewarded -> {
                                                Log.i(TAG_AD_FREE, "reward_earned — granting ads-free")
                                                val optimistic =
                                                    System.currentTimeMillis() + AdFreeSession.DEFAULT_DURATION_MS
                                                adsSdk.syncAdFreeUntil(optimistic)
                                                AppScope.launch {
                                                    runCatching {
                                                        val until = preferences.grantAdFree(AdFreeSession.DEFAULT_DURATION_MS)
                                                        adsSdk.syncAdFreeUntil(until)
                                                        Log.i(TAG_AD_FREE, "ads_free_granted | untilEpochMs=$until")
                                                    }.onFailure { t ->
                                                        Log.e(TAG_AD_FREE, "ads_free_grant_failed", t)
                                                    }
                                                }
                                            }
                                            AdShowResult.Impression,
                                            AdShowResult.Shown,
                                            AdShowResult.Dismissed,
                                            AdShowResult.NotReady,
                                            AdShowResult.Disabled,
                                            is AdShowResult.Failed -> closeSheetOnce()
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Watch & remove ads",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Not now",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface
                        )
                    }
                }
            }
        }
    }
}

/** Soft brand-colored hero — layered orbs + badge icons (no external asset needed). */
@Composable
private fun AdsFreeHeroIllustration(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val accent = colors.primary
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = 0.18f),
                        accent.copy(alpha = 0.06f),
                        colors.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.10f))
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f))
        )
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-70).dp, y = (-28).dp)
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Block, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 72.dp, y = 18.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Videocam, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun AdsFreeBenefitRow(label: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface
        )
    }
}

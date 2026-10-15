package com.resume.builder.presentation.onboarding

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.pixelforge.ads.consent.showConsentFormIfRequired
import com.pixelforge.ads.nativead.NativeAdSlot
import com.pixelforge.ads.nativead.NativeAdTemplate
import com.pixelforge.ads.remoteconfig.AdFormat
import com.resume.builder.domain.model.TemplateCatalog
import com.resume.builder.domain.repository.PreferencesRepository
import com.resume.builder.presentation.components.BrandLogo
import com.resume.builder.presentation.components.GradientButton
import com.resume.builder.ui.theme.Amber
import com.resume.builder.ui.theme.Gradients
import com.resume.builder.ui.theme.Teal
import com.resume.builder.ui.theme.Violet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class OnboardingViewModel(
    private val prefs: PreferencesRepository,
) : ViewModel() {
    fun finish(source: String?) = prefs.setOnboardingDone(source)
}

private data class ReferralOption(val label: String, val icon: ImageVector, val tint: Color)

private val referralOptions = listOf(
    ReferralOption("YouTube", Icons.Rounded.SmartDisplay, Color(0xFFFF0033)),
    ReferralOption("TikTok", Icons.Rounded.MusicNote, Color(0xFF111111)),
    ReferralOption("Social media", Icons.Rounded.Groups, Color(0xFF1877F2)),
    ReferralOption("Play Store", Icons.Rounded.Shop, Color(0xFF01875F)),
    ReferralOption("Google search", Icons.Rounded.Search, Color(0xFF4285F4)),
    ReferralOption("Friend or family", Icons.Rounded.People, Color(0xFFF59E0B)),
    ReferralOption("Other", Icons.Rounded.MoreHoriz, Color(0xFF6B7280)),
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onLegal: (String) -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val activity = LocalActivity.current
    // UMP privacy message on the first-run Get Started screen. Does not gate ad loading.
    LaunchedEffect(Unit) {
        activity?.showConsentFormIfRequired()
    }
    Box(
        Modifier.fillMaxSize().background(if (dark) Gradients.onboardingDark else Gradients.onboarding),
    ) {
        HorizontalPager(state = pager, userScrollEnabled = false, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> WelcomePage(onContinue = { scope.launch { pager.animateScrollToPage(1) } }, onLegal = onLegal)
                else -> ReferralPage(
                    onContinue = { source ->
                        viewModel.finish(source)
                        onFinished()
                    },
                )
            }
        }
    }
}

@Composable
private fun WelcomePage(onContinue: () -> Unit, onLegal: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val logoScale by animateFloatAsState(if (visible) 1f else 0.6f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "logo")

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.8f))
        BrandLogo(Modifier.scale(logoScale), size = 112.dp)
        Spacer(Modifier.height(28.dp))
        Text(
            "Resume Builder",
            style = MaterialTheme.typography.displaySmall.copy(brush = Gradients.brand),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Craft a standout resume in minutes — and land the interview.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(36.dp))
        val features = listOf(
            Triple(Icons.Rounded.Dashboard, "${TemplateCatalog.all.size} designer templates", "Modern, creative and ATS-friendly layouts") to MaterialTheme.colorScheme.primary,
            Triple(Icons.Rounded.AutoAwesome, "AI writing assistant", "Generate resumes & tailored cover letters") to Violet,
            Triple(Icons.Rounded.PictureAsPdf, "Pixel-perfect PDFs", "Plus merge, compress, split and sign tools") to Teal,
        )
        features.forEachIndexed { index, (feature, tint) ->
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, delayMillis = 200 + index * 120)) + slideInVertically(tween(400, delayMillis = 200 + index * 120)) { it / 2 },
            ) {
                FeatureRow(feature.first, feature.second, feature.third, tint)
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.weight(1f))
        NativeAdSlot(
            placement = AdFormat.NATIVE_ONBOARDING,
            template = NativeAdTemplate.SMALL,
            screen = "Onboarding",
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
        GradientButton("Get started", onClick = onContinue, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        LegalText(onLegal)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LegalText(onLegal: (String) -> Unit) {
    val linkStyle = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline))
    val text = buildAnnotatedString {
        append("By continuing you agree to our ")
        withLink(LinkAnnotation.Clickable("terms", linkStyle) { onLegal("terms") }) { append("Terms of Service") }
        append(" and ")
        withLink(LinkAnnotation.Clickable("privacy", linkStyle) { onLegal("privacy") }) { append("Privacy Policy") }
    }
    Text(
        text,
        style = TextStyle(textAlign = TextAlign.Center).merge(MaterialTheme.typography.bodySmall),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ReferralPage(onContinue: (String?) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onContinue(null) }) { Text("Skip") }
        }
        Spacer(Modifier.height(12.dp))
        Text("How did you find us?", style = MaterialTheme.typography.headlineLarge.copy(brush = Gradients.brand))
        Spacer(Modifier.height(8.dp))
        Text(
            "Your answer helps us reach more job seekers like you.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(referralOptions) { option ->
                val isSelected = selected == option.label
                val scale by animateFloatAsState(if (isSelected) 1.03f else 1f, label = "scale")
                Surface(
                    onClick = { selected = option.label },
                    shape = RoundedCornerShape(22.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.scale(scale),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(option.tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(option.icon, null, tint = option.tint)
                            }
                            Spacer(Modifier.weight(1f))
                            if (isSelected) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(option.label, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        NativeAdSlot(
            placement = AdFormat.NATIVE_REFERRAL,
            template = NativeAdTemplate.SMALL,
            screen = "Referral",
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
        GradientButton("Continue", onClick = { onContinue(selected) }, enabled = selected != null, modifier = Modifier.fillMaxWidth())
        Text(
            if (selected == null) "Pick an option to continue" else "Thanks! You're all set ✨",
            style = MaterialTheme.typography.bodySmall,
            color = if (selected == null) MaterialTheme.colorScheme.onSurfaceVariant else Amber,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
    }
}

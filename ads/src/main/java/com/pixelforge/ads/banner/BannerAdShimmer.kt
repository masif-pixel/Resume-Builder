package com.pixelforge.ads.banner

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Banner placeholder — height matches [style] (50dp / 100dp / 250dp). */
@Composable
fun BannerAdShimmer(
    modifier: Modifier = Modifier,
    style: BannerAdStyle = BannerAdStyle.LARGE_BANNER,
) {
    val heightDp = when (style) {
        BannerAdStyle.BANNER -> 50.dp
        BannerAdStyle.LARGE_BANNER -> 100.dp
        BannerAdStyle.MEDIUM_RECTANGLE -> 250.dp
        BannerAdStyle.ANCHORED_ADAPTIVE,
        BannerAdStyle.LARGE_ANCHORED_ADAPTIVE,
        BannerAdStyle.COLLAPSIBLE_BOTTOM,
        BannerAdStyle.COLLAPSIBLE_TOP -> 100.dp
    }
    val transition = rememberInfiniteTransition(label = "banner_shimmer")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "banner_shimmer_shift"
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE0E0E0),
            Color(0xFFF7F7F7),
            Color(0xFFE0E0E0),
        ),
        start = Offset(shift - 280f, 0f),
        end = Offset(shift + 80f, 120f),
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(4.dp))
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Ad",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF9E9E9E),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

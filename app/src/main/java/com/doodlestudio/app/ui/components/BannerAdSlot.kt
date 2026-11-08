package com.doodlestudio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.doodlestudio.app.ui.theme.NeonBannerDenim

/**
 * Top banner area that shows a real AdMob adaptive banner.
 * The denim-blue background stays visible while the ad loads so layout doesn't jump.
 *
 * !! BEFORE PUBLISHING: set AdConfig.BANNER_EDITOR_TOP to your real Ad Unit ID.
 */
@Composable
fun BannerAdSlot(
    adUnitId: String = AdConfig.BANNER_EDITOR_TOP,
    modifier: Modifier = Modifier,
    height: Dp = 100.dp
) {
    val context = LocalContext.current

    // Create and cache the AdView — do not recreate on recomposition
    val adView = remember(adUnitId) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
            loadAd(AdRequest.Builder().build())
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(NeonBannerDenim)
            .drawBehind {
                // Denim diagonal weave texture — always visible behind/around the ad
                val stripeColor = Color(0xFF163E52)
                val stripeSpacing = 8.dp.toPx()
                val strokeW = 1.5.dp.toPx()
                var x = -size.height
                while (x < size.width + size.height) {
                    drawLine(
                        color = stripeColor,
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = strokeW
                    )
                    x += stripeSpacing
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

package com.doodlestudio.app.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlestudio.app.ui.components.NeonGlowBox
import com.doodlestudio.app.ui.components.NeonText
import com.doodlestudio.app.ui.screens.home.NewDrawingGraphic
import com.doodlestudio.app.ui.theme.NeonCyan
import com.doodlestudio.app.ui.theme.NeonPink
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit
) {
    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 600)
        )
        delay(400)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            NeonGlowBox(
                glowColor = NeonCyan,
                cornerRadius = 24.dp,
                modifier = Modifier.size(110.dp)
            ) {
                NewDrawingGraphic(modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.height(24.dp))
            NeonText(
                text = "DoodleStudio",
                fontSize = 32.sp,
                glowColor = NeonPink
            )
        }
    }
}

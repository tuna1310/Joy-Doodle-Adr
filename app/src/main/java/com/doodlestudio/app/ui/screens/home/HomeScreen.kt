package com.doodlestudio.app.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.doodlestudio.app.R
import com.doodlestudio.app.ui.components.NeonGlowBox
import com.doodlestudio.app.ui.components.NeonText
import com.doodlestudio.app.ui.components.PromoAppIcon
import com.doodlestudio.app.ui.components.PromoAppsList
import com.doodlestudio.app.ui.components.openPlayStore
import com.doodlestudio.app.ui.theme.NeonBackgroundDark
import com.doodlestudio.app.ui.theme.NeonCyan
import com.doodlestudio.app.ui.theme.NeonPink
import com.doodlestudio.app.util.SoundEffectManager
import org.koin.compose.koinInject

@Composable
fun HomeScreen(
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToGallery: () -> Unit,
    soundEffectManager: SoundEffectManager = koinInject()
) {
    var showAdFreeDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonBackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Main Vertically Centered Content: 2 Big Glowing Icons
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. NEW Drawing Item
            HomeMenuItem(
                glowColor = NeonCyan,
                label = "New",
                labelGlow = NeonPink,
                onClick = {
                    soundEffectManager.play(SoundEffectManager.Sound.CLICK)
                    onNavigateToEditor(null)
                }
            ) {
                NewDrawingGraphic(modifier = Modifier.fillMaxSize())
            }

            Spacer(modifier = Modifier.height(44.dp))

            // 2. GALLERY Item
            HomeMenuItem(
                glowColor = NeonPink,
                label = "GALLERY",
                labelGlow = NeonCyan,
                onClick = {
                    soundEffectManager.play(SoundEffectManager.Sound.CLICK)
                    onNavigateToGallery()
                }
            ) {
                GalleryNotebookGraphic(modifier = Modifier.fillMaxSize())
            }
        }

        // Bottom Section: Ad Free Badge (right) & Row of 6 Promo Tiles (bottom)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            // "Ad Free" circular button floating above promo tiles on the right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 6.dp, bottom = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                AdFreeBadge(
                    onClick = {
                        soundEffectManager.play(SoundEffectManager.Sound.CLICK)
                        showAdFreeDialog = true
                    }
                )
            }

            // Row of 6 More Apps Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val context = LocalContext.current
                PromoAppsList.forEach { app ->
                    PromoAppIcon(
                        iconType = app.iconType,
                        modifier = Modifier.size(50.dp),
                        onClick = { openPlayStore(context, app.packageName) }
                    )
                }
            }
        }

        // Stub dialog for Remove Ads
        if (showAdFreeDialog) {
            AlertDialog(
                onDismissRequest = { showAdFreeDialog = false },
                title = { Text(text = "DoodleStudio Ad-Free") },
                text = { Text(text = stringResource(R.string.ad_free_description)) },
                confirmButton = {
                    TextButton(onClick = { showAdFreeDialog = false }) {
                        Text(text = "OK", color = NeonCyan)
                    }
                },
                containerColor = Color(0xFF1E1E1E),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFCCCCCC)
            )
        }
    }
}

@Composable
private fun HomeMenuItem(
    glowColor: Color,
    label: String,
    labelGlow: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "HomeItemScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        NeonGlowBox(
            glowColor = glowColor,
            cornerRadius = 28.dp,
            borderWidth = 3.5.dp,
            glowRadius = 14.dp,
            modifier = Modifier.size(136.dp)
        ) {
            content()
        }

        Spacer(modifier = Modifier.height(10.dp))

        NeonText(
            text = label,
            fontSize = 30.sp,
            glowColor = labelGlow
        )
    }
}

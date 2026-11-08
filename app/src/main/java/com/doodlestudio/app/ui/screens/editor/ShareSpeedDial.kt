package com.doodlestudio.app.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.doodlestudio.app.ui.theme.NeonCoralButton
import com.doodlestudio.app.ui.theme.NeonCyan
import com.doodlestudio.app.ui.theme.NeonPink

/**
 * Vertical Speed-Dial popup expanding upwards above the Share paper-plane button:
 * 1. Camera snapshot
 * 2. Timelapse video
 * 3. Photo background
 * 4. Android system share
 */
@Composable
fun ShareSpeedDial(
    onCameraClick: () -> Unit,
    onTimelapseClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Camera Snapshot (Teal/Cyan circle)
        SpeedDialCircleButton(
            backgroundColor = NeonCyan,
            iconTint = Color.Black,
            onClick = onCameraClick
        ) {
            // Camera icon
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(2.dp, Color.Black, CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Video Record / Timelapse (White ring with inner red circle)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, NeonPink, CircleShape)
                .clickable(onClick = onTimelapseClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(NeonPink)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Photo Background (Coral circle)
        SpeedDialCircleButton(
            backgroundColor = NeonCoralButton,
            iconTint = Color.White,
            onClick = onPhotoClick
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(2.dp, Color.White)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Android System Share (Coral circle with share icon)
        SpeedDialCircleButton(
            backgroundColor = NeonCoralButton,
            iconTint = Color.White,
            onClick = onShareClick
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SpeedDialCircleButton(
    backgroundColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

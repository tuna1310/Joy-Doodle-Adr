package com.doodlestudio.app.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlestudio.app.ui.theme.NeonPink
import com.doodlestudio.app.ui.theme.NeonPlayButton

/**
 * Replay playback bar matching video frame 42 & 48:
 * - Speed selector: 1x, 2x, 4x, 8x
 * - Scrubber slider with pink thumb
 * - Record circle
 * - Play / Pause button
 * - Stop button
 */
@Composable
fun ReplayPlaybackBar(
    isPlaying: Boolean,
    progress: Float,
    speedMultiplier: Int,
    onSpeedClick: () -> Unit,
    onScrub: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color.Black)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Speed selector
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSpeedClick)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${speedMultiplier}x",
                color = NeonPink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Timeline Scrubber Slider
        Slider(
            value = progress,
            onValueChange = onScrub,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = NeonPink,
                activeTrackColor = NeonPink,
                inactiveTrackColor = Color(0xFF333333)
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        // Action Buttons: Record dot, Play/Pause, Stop
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // White circle with pink inner dot
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(NeonPink)
                )
            }

            // Play / Pause Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(NeonPlayButton)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    // Pause icon (2 vertical bars)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(width = 3.dp, height = 14.dp).background(Color.White))
                        Box(modifier = Modifier.size(width = 3.dp, height = 14.dp).background(Color.White))
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Stop Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(NeonPlayButton)
                    .clickable(onClick = onStop),
                contentAlignment = Alignment.Center
            ) {
                // Square stop icon
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(Color.White)
                )
            }
        }
    }
}

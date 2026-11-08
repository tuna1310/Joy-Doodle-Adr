package com.doodlestudio.app.ui.screens.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.doodlestudio.app.ui.theme.NeonCoralButton

/**
 * Bottom options panel for Eraser:
 * - Eraser size slider (12f to 120f)
 * - 4 Quick size presets (Nhỏ, Vừa, To, Rất to)
 * - Live circular preview of the eraser diameter
 */
@Composable
fun EraserOptionsPanel(
    eraserSize: Float,
    onEraserSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color(0xFF141414))
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header: Title + Live Preview Circle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🧹", fontSize = 20.sp)
                Text(
                    text = "Cỡ tẩy (Eraser Size)",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Live visual circle preview representing the actual eraser tip
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E2E))
                    .border(1.dp, Color(0xFF333344), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val previewRadius = (eraserSize / 120f * 22f).coerceIn(4f, 24f)
                Box(
                    modifier = Modifier
                        .size((previewRadius * 2).dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5252))
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4 Preset Buttons: Nhỏ, Vừa, To, Rất to
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                "Nhỏ" to 18f,
                "Vừa" to 36f,
                "To" to 64f,
                "Rất to" to 96f
            )

            presets.forEach { (label, size) ->
                val isSelected = kotlin.math.abs(eraserSize - size) < 4f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NeonCoralButton else Color(0xFF22222A))
                        .border(
                            1.dp,
                            if (isSelected) Color.White else Color(0xFF333344),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onEraserSizeChange(size) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Eraser Size Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "12",
                color = Color.Gray,
                fontSize = 11.sp
            )

            Slider(
                value = eraserSize,
                onValueChange = onEraserSizeChange,
                valueRange = 12f..120f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCoralButton,
                    activeTrackColor = NeonCoralButton,
                    inactiveTrackColor = Color(0xFF333333)
                ),
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${eraserSize.toInt()}",
                color = NeonCoralButton,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp)
            )
        }
    }
}


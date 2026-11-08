package com.doodlestudio.app.ui.screens.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlestudio.app.domain.canvas.BrushTool
import com.doodlestudio.app.domain.canvas.brush.BrushEffect
import com.doodlestudio.app.domain.canvas.brush.BrushPreviewCache
import com.doodlestudio.app.domain.canvas.brush.BrushRegistry
import com.doodlestudio.app.ui.theme.NeonCyan
import com.doodlestudio.app.ui.theme.NeonPalette

/**
 * Brush options bottom panel:
 * - Horizontal scrolling row of 21 brush preview cards (ordered exactly as video)
 * - 8 Neon palette colors
 * - Brush size slider
 */
@Composable
fun BrushOptionsPanel(
    selectedTool: BrushTool,
    onToolSelected: (BrushTool) -> Unit,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    brushSize: Float,
    onBrushSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomColorDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color(0xFF141414))
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(vertical = 12.dp)
    ) {
        // Horizontal list of 21 Brushes
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(BrushRegistry.allBrushes, key = { it.id }) { brush ->
                BrushCard(
                    brush = brush,
                    isSelected = selectedTool.toolId == brush.id || selectedTool.name == brush.id,
                    onClick = {
                        onToolSelected(BrushTool.fromString(brush.id))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 28 Rich Neon & Glow Palette Colors (2 rows of 14, horizontally scrollable)
        val half = NeonPalette.size / 2
        val row1 = NeonPalette.take(half)
        val row2 = NeonPalette.drop(half)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Warm Neons (Reds, Pinks, Oranges, Yellows)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row1.forEach { color ->
                    ColorCircleButton(
                        color = color,
                        isSelected = selectedColor == color,
                        onClick = { onColorSelected(color) }
                    )
                }
            }

            // Row 2: Cool Neons (Greens, Teals, Cyans, Blues, Purples, White) + Rainbow Custom Picker
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row2.forEach { color ->
                    ColorCircleButton(
                        color = color,
                        isSelected = selectedColor == color,
                        onClick = { onColorSelected(color) }
                    )
                }

                // Rainbow custom color picker button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color.Red, Color.Yellow, Color.Green,
                                    Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                )
                            )
                        )
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable { showCustomColorDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🌈", fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Brush Size Slider + Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Size:",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Slider(
                value = brushSize,
                onValueChange = onBrushSizeChange,
                valueRange = 4f..48f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = Color(0xFF333333)
                ),
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${brushSize.toInt()}",
                color = NeonCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp)
            )
        }
    }

    if (showCustomColorDialog) {
        CustomColorPickerDialog(
            initialColor = selectedColor,
            onDismiss = { showCustomColorDialog = false },
            onColorChosen = { newColor ->
                showCustomColorDialog = false
                onColorSelected(newColor)
            }
        )
    }
}

@Composable
private fun ColorCircleButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.White else Color(0x55FFFFFF),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun CustomColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorChosen: (Color) -> Unit
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var lightness by remember { mutableFloatStateOf(0.5f) }

    val currentColor = remember(hue, lightness) {
        val hsv = floatArrayOf(hue, 1.0f, lightness.coerceIn(0.1f, 1.0f))
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Chọn màu tùy chỉnh", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Color Preview Box
                Box(
                    modifier = Modifier
                        .size(width = 140.dp, height = 60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentColor)
                        .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                )

                // Rainbow Hue Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Sắc màu (Hue): ${hue.toInt()}°", color = Color.LightGray, fontSize = 12.sp)
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = currentColor,
                            inactiveTrackColor = Color(0xFF333333)
                        )
                    )
                }

                // Brightness Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Độ sáng: ${(lightness * 100).toInt()}%", color = Color.LightGray, fontSize = 12.sp)
                    Slider(
                        value = lightness,
                        onValueChange = { lightness = it },
                        valueRange = 0.2f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = currentColor,
                            inactiveTrackColor = Color(0xFF333333)
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorChosen(currentColor) }) {
                Text(text = "Chọn màu này", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Hủy", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1E1E2E),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun BrushCard(
    brush: BrushEffect,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val previewBitmap = BrushPreviewCache.getPreview(brush)
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(shape)
            .background(Color.Black)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) NeonCyan else Color(0xFF333333),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(if (isSelected) 2.dp else 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = previewBitmap,
            contentDescription = brush.displayNameVi,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}

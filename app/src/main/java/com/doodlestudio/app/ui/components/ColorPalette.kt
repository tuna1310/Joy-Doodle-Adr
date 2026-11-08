package com.doodlestudio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val DEFAULT_PALETTE_COLORS = listOf(
    Color(0xFFFF5277), // Coral Pink
    Color(0xFFFF4081), // Pink
    Color(0xFFE040FB), // Purple
    Color(0xFF7C4DFF), // Deep Purple
    Color(0xFF536DFE), // Indigo
    Color(0xFF448AFF), // Blue
    Color(0xFF00B0FF), // Light Blue
    Color(0xFF00E5FF), // Cyan
    Color(0xFF1DE9B6), // Teal
    Color(0xFF00E676), // Green
    Color(0xFF76FF03), // Light Green
    Color(0xFFFFEA00), // Yellow
    Color(0xFFFFC400), // Amber
    Color(0xFFFF9100), // Orange
    Color(0xFFFF3D00), // Deep Orange
    Color(0xFF8D6E63), // Brown
    Color(0xFF78909C), // Blue Grey
    Color(0xFF212121), // Near Black
    Color(0xFFFFFFFF)  // White
)

@Composable
fun ColorPalette(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = DEFAULT_PALETTE_COLORS
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(colors) { color ->
            val isSelected = color == selectedColor
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onColorSelected(color) }
                    .then(
                        if (isSelected) {
                            Modifier.border(3.dp, Color.Black, CircleShape)
                        } else if (color == Color.White) {
                            Modifier.border(1.dp, Color.LightGray, CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(if (color == Color.White) Color.Black else Color.White, CircleShape)
                    )
                }
            }
        }
    }
}


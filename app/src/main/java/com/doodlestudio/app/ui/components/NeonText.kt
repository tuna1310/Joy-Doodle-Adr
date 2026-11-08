package com.doodlestudio.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlestudio.app.ui.theme.NeonCyan
import com.doodlestudio.app.ui.theme.NeonPink
import com.doodlestudio.app.ui.theme.NeonRainbowColors
import com.doodlestudio.app.ui.theme.SnigletFontFamily

/**
 * NeonText renders a handwritten neon-tube display label with
 * a rainbow linear-gradient and layered luminous glow.
 */
@Composable
fun NeonText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    fontFamily: FontFamily = SnigletFontFamily,
    glowColor: Color = NeonPink,
    textAlign: TextAlign = TextAlign.Center
) {
    val rainbowBrush = Brush.linearGradient(
        colors = NeonRainbowColors,
        start = Offset(0f, 0f),
        end = Offset(300f, 0f)
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 1. Soft diffused outer glow behind
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            textAlign = textAlign,
            style = TextStyle(
                color = glowColor.copy(alpha = 0.6f),
                shadow = Shadow(
                    color = glowColor,
                    offset = Offset.Zero,
                    blurRadius = 18f
                )
            )
        )

        // 2. Focused cyan/white second glow layer for intense electric neon effect
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            textAlign = textAlign,
            style = TextStyle(
                color = NeonCyan.copy(alpha = 0.4f),
                shadow = Shadow(
                    color = Color.White,
                    offset = Offset.Zero,
                    blurRadius = 8f
                )
            )
        )

        // 3. Foreground rainbow gradient lettering
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            textAlign = textAlign,
            style = TextStyle(
                brush = rainbowBrush
            )
        )
    }
}


package com.doodlestudio.app.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * NeonGlowBox renders a container with a multi-layered neon glow border.
 * Features an outer blurred halo (using native BlurMaskFilter) and a crisp inner neon line.
 */
@Composable
fun NeonGlowBox(
    glowColor: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 3.dp,
    glowRadius: Dp = 12.dp,
    backgroundColor: Color = Color.Black,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .drawBehind {
                val crPx = cornerRadius.toPx()
                val glowPx = glowRadius.toPx()
                val borderPx = borderWidth.toPx()

                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas

                    // 1. Wide diffused outer neon aura
                    val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = glowColor.toArgb()
                        alpha = (0.35f * 255).toInt()
                        style = Paint.Style.STROKE
                        strokeWidth = borderPx + glowPx * 1.5f
                        maskFilter = BlurMaskFilter(glowPx * 1.2f, BlurMaskFilter.Blur.NORMAL)
                    }

                    // 2. Focused vibrant inner glow
                    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = glowColor.toArgb()
                        alpha = (0.75f * 255).toInt()
                        style = Paint.Style.STROKE
                        strokeWidth = borderPx + glowPx * 0.4f
                        maskFilter = BlurMaskFilter(glowPx * 0.5f, BlurMaskFilter.Blur.NORMAL)
                    }

                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = 0f,
                                top = 0f,
                                right = size.width,
                                bottom = size.height,
                                cornerRadius = CornerRadius(crPx, crPx)
                            )
                        )
                    }.asAndroidPath()

                    nativeCanvas.drawPath(path, auraPaint)
                    nativeCanvas.drawPath(path, glowPaint)
                }
            }
            .clip(shape)
            .border(width = borderWidth, color = glowColor, shape = shape)
            .background(backgroundColor, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = glowColor),
                        onClick = onClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}


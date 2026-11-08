package com.doodlestudio.app.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlestudio.app.ui.theme.NeonCyan
import com.doodlestudio.app.ui.theme.NeonGreen
import com.doodlestudio.app.ui.theme.NeonOrange
import com.doodlestudio.app.ui.theme.NeonPink
import com.doodlestudio.app.ui.theme.NeonPurple
import com.doodlestudio.app.ui.theme.NeonYellow

/**
 * Custom Canvas-rendered New Drawing Icon:
 * Palette + paintbrush resting on a vertical rainbow watercolor backdrop.
 */
@Composable
fun NewDrawingGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Vertical watercolor rainbow stripes
        val rainbowColors = listOf(
            Color(0xFF4A90E2), // Blue
            Color(0xFF50E3C2), // Teal
            Color(0xFFB8E986), // Lime
            Color(0xFFF8E71C), // Yellow
            Color(0xFFF5A623), // Orange
            Color(0xFFFF5252)  // Coral/Red
        )
        val stripeW = size.width / rainbowColors.size
        rainbowColors.forEachIndexed { i, c ->
            drawRect(
                color = c,
                topLeft = Offset(i * stripeW, 0f),
                size = Size(stripeW, size.height)
            )
        }

        // Painter's Palette (Warm golden yellow bean shape)
        val palettePath = Path().apply {
            moveTo(size.width * 0.35f, size.height * 0.35f)
            cubicTo(
                size.width * 0.15f, size.height * 0.45f,
                size.width * 0.2f, size.height * 0.85f,
                size.width * 0.55f, size.height * 0.88f
            )
            cubicTo(
                size.width * 0.85f, size.height * 0.88f,
                size.width * 0.95f, size.height * 0.65f,
                size.width * 0.85f, size.height * 0.42f
            )
            cubicTo(
                size.width * 0.72f, size.height * 0.22f,
                size.width * 0.45f, size.height * 0.22f,
                size.width * 0.35f, size.height * 0.35f
            )
            close()
        }
        drawPath(palettePath, color = Color(0xFFFFD54F))

        // Thumb Hole on palette
        drawCircle(
            color = Color(0xFFF5A623),
            radius = size.width * 0.055f,
            center = Offset(size.width * 0.75f, size.height * 0.58f)
        )

        // Paint color blobs on palette
        val blobColors = listOf(
            Color(0xFF00E5FF) to Offset(size.width * 0.4f, size.height * 0.42f),
            Color(0xFF76FF03) to Offset(size.width * 0.35f, size.height * 0.55f),
            Color(0xFFFF3D00) to Offset(size.width * 0.42f, size.height * 0.72f),
            Color(0xFFFF4081) to Offset(size.width * 0.6f, size.height * 0.78f)
        )
        blobColors.forEach { (color, center) ->
            drawCircle(color = color, radius = size.width * 0.045f, center = center)
        }

        // Paintbrush crossing diagonally
        // Handle (wood tone)
        drawLine(
            color = Color(0xFFE65100),
            start = Offset(size.width * 0.72f, size.height * 0.68f),
            end = Offset(size.width * 0.52f, size.height * 0.42f),
            strokeWidth = size.width * 0.045f,
            cap = StrokeCap.Round
        )
        // Silver ferrule
        drawLine(
            color = Color(0xFFECEFF1),
            start = Offset(size.width * 0.52f, size.height * 0.42f),
            end = Offset(size.width * 0.48f, size.height * 0.37f),
            strokeWidth = size.width * 0.048f,
            cap = StrokeCap.Butt
        )
        // Cyan paintbrush bristle tip
        val bristle = Path().apply {
            moveTo(size.width * 0.48f, size.height * 0.37f)
            quadraticTo(
                size.width * 0.44f, size.height * 0.3f,
                size.width * 0.38f, size.height * 0.22f
            )
            quadraticTo(
                size.width * 0.48f, size.height * 0.28f,
                size.width * 0.53f, size.height * 0.36f
            )
            close()
        }
        drawPath(bristle, color = Color(0xFF00B0FF))
    }
}

/**
 * Custom Canvas-rendered Gallery Icon:
 * Spiral notebook with a sunflower and stem on cyan cover.
 */
@Composable
fun GalleryNotebookGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Black backing frame
        drawRect(color = Color.Black)

        // Notebook Cover (Sky Blue)
        drawRect(
            color = Color(0xFF29B6F6),
            topLeft = Offset(size.width * 0.12f, size.height * 0.12f),
            size = Size(size.width * 0.76f, size.height * 0.76f)
        )

        // Red/Orange Left Spine
        drawRect(
            color = Color(0xFFFF5722),
            topLeft = Offset(size.width * 0.12f, size.height * 0.12f),
            size = Size(size.width * 0.18f, size.height * 0.76f)
        )

        // Spiral Wire Rings on spine (4 horizontal pills)
        val ringYPositions = listOf(0.24f, 0.40f, 0.56f, 0.72f)
        ringYPositions.forEach { yRatio ->
            drawLine(
                color = Color(0xFFFFCCBC),
                start = Offset(size.width * 0.08f, size.height * yRatio),
                end = Offset(size.width * 0.26f, size.height * yRatio),
                strokeWidth = size.width * 0.045f,
                cap = StrokeCap.Round
            )
        }

        // Green Flower Stem
        drawLine(
            color = Color(0xFF66BB6A),
            start = Offset(size.width * 0.56f, size.height * 0.54f),
            end = Offset(size.width * 0.56f, size.height * 0.78f),
            strokeWidth = size.width * 0.035f,
            cap = StrokeCap.Round
        )

        // Green Leaves
        val leftLeaf = Path().apply {
            moveTo(size.width * 0.56f, size.height * 0.66f)
            quadraticTo(
                size.width * 0.42f, size.height * 0.6f,
                size.width * 0.44f, size.height * 0.72f
            )
            close()
        }
        drawPath(leftLeaf, color = Color(0xFF43A047))

        val rightLeaf = Path().apply {
            moveTo(size.width * 0.56f, size.height * 0.64f)
            quadraticTo(
                size.width * 0.7f, size.height * 0.58f,
                size.width * 0.68f, size.height * 0.7f
            )
            close()
        }
        drawPath(rightLeaf, color = Color(0xFF43A047))

        // Sunflower Petals (Golden Yellow)
        val flowerCenter = Offset(size.width * 0.56f, size.height * 0.42f)
        val petalRadius = size.width * 0.075f
        val numPetals = 8
        for (i in 0 until numPetals) {
            val angle = (i * (360.0 / numPetals)) * (Math.PI / 180.0)
            val px = flowerCenter.x + (size.width * 0.12f * Math.cos(angle)).toFloat()
            val py = flowerCenter.y + (size.width * 0.12f * Math.sin(angle)).toFloat()
            drawCircle(
                color = Color(0xFFFFCA28),
                radius = petalRadius,
                center = Offset(px, py)
            )
        }

        // Flower Center (Orange / Amber)
        drawCircle(
            color = Color(0xFFF57C00),
            radius = size.width * 0.085f,
            center = flowerCenter
        )
    }
}

/**
 * Small circular "Ad Free" badge with a neon pink ring at bottom right.
 */
@Composable
fun AdFreeBadge(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .border(2.dp, NeonPink, CircleShape)
            .background(Color.Black)
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ad",
                color = NeonGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 11.sp
            )
            Text(
                text = "Free",
                color = NeonPink,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 9.sp
            )
        }
    }
}


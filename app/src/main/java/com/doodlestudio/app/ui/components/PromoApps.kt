package com.doodlestudio.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlestudio.app.ui.theme.NeonBlue
import com.doodlestudio.app.ui.theme.NeonCyan
import com.doodlestudio.app.ui.theme.NeonGreen
import com.doodlestudio.app.ui.theme.NeonOrange
import com.doodlestudio.app.ui.theme.NeonPink
import com.doodlestudio.app.ui.theme.NeonPurple
import com.doodlestudio.app.ui.theme.NeonYellow

enum class PromoIconType {
    KALEIDOSCOPE,
    PENCILS,
    PRINCESS,
    DOLPHIN,
    GLOW_FLOWER,
    SPARKLE_NIGHT
}

data class PromoApp(
    val id: String,
    val title: String,
    val iconType: PromoIconType,
    /** Google Play package name. Set to null if unknown — tapping will do nothing. */
    val packageName: String? = null
)

val PromoAppsList = listOf(
    PromoApp(
        id = "promo_kaleido",
        title = "Kaleidoscope Drawing",
        iconType = PromoIconType.KALEIDOSCOPE,
        packageName = "com.tcstudio.paintkids"
    ),
    PromoApp(
        id = "promo_pencils",
        title = "Kids Coloring",
        iconType = PromoIconType.PENCILS,
        packageName = "com.mark.paintforkids"
    ),
    PromoApp(
        id = "promo_princess",
        title = "Draw Princess",
        iconType = PromoIconType.PRINCESS,
        packageName = "com.LifeIsGame.ArtCanvas"
    ),
    PromoApp(
        id = "promo_dolphin",
        title = "Where's Color in Zoo",
        iconType = PromoIconType.DOLPHIN,
        packageName = "com.ar.drawing.sketch.trace.paint.photo"
    ),
    PromoApp(
        id = "promo_paintjoy",
        title = "Paint Joy",
        iconType = PromoIconType.GLOW_FLOWER,
        packageName = "jp.ne.ibis.ibispaintx.app"
    ),
    PromoApp(
        id = "promo_sparkle",
        title = "Glow Sparkle",
        iconType = PromoIconType.SPARKLE_NIGHT,
        packageName = "com.tucapps.sketchbook"
    )
)

/**
 * Renders a crisp graphic placeholder icon for a promo app.
 */
@Composable
fun PromoAppIcon(
    iconType: PromoIconType,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "PromoScale"
    )

    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .border(1.dp, Color(0xFFE0E0E0), shape)
            .then(
                if (onClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { onClick() }
                        )
                    }
                } else Modifier
            )
    ) {
        when (iconType) {
            PromoIconType.KALEIDOSCOPE -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Sunburst / radial gradient background
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF5252), Color(0xFFFF1744)),
                            center = center,
                            radius = size.width
                        )
                    )
                    // Kaleidoscope tube representation
                    val tubePath = Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.25f)
                        lineTo(size.width * 0.75f, size.height * 0.75f)
                    }
                    drawLine(
                        color = Color(0xFF7C4DFF),
                        start = Offset(size.width * 0.25f, size.height * 0.25f),
                        end = Offset(size.width * 0.7f, size.height * 0.7f),
                        strokeWidth = size.width * 0.24f,
                        cap = StrokeCap.Round
                    )
                    // Flare wheel on end
                    drawCircle(
                        color = Color(0xFFFFD600),
                        radius = size.width * 0.18f,
                        center = Offset(size.width * 0.72f, size.height * 0.72f)
                    )
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = size.width * 0.08f,
                        center = Offset(size.width * 0.72f, size.height * 0.72f)
                    )
                }
            }

            PromoIconType.PENCILS -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Sky blue ray background
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF40C4FF), Color(0xFFE1F5FE))
                        )
                    )
                    val colors = listOf(
                        Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFFFFEA00),
                        Color(0xFF00E676), Color(0xFF00B0FF), Color(0xFF651FFF), Color(0xFFD500F9)
                    )
                    val pencilW = size.width / (colors.size + 1)
                    colors.forEachIndexed { i, c ->
                        val x = pencilW * (i + 0.5f)
                        val h = size.height * (0.45f + (i % 3) * 0.12f)
                        drawRect(
                            color = c,
                            topLeft = Offset(x, size.height - h),
                            size = Size(pencilW * 0.85f, h)
                        )
                        val tip = Path().apply {
                            moveTo(x, size.height - h)
                            lineTo(x + pencilW * 0.425f, size.height - h - pencilW * 0.7f)
                            lineTo(x + pencilW * 0.85f, size.height - h)
                            close()
                        }
                        drawPath(tip, color = Color(0xFFFFE0B2))
                    }
                }
            }

            PromoIconType.PRINCESS -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFB2DFDB), Color(0xFFE0F2F1))
                        )
                    )
                    // Stylized fairy/princess crown + silhouette
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = size.width * 0.22f,
                        center = Offset(center.x, size.height * 0.38f)
                    )
                    // Gown / ribbon
                    val gown = Path().apply {
                        moveTo(center.x, size.height * 0.45f)
                        lineTo(size.width * 0.85f, size.height)
                        lineTo(size.width * 0.15f, size.height)
                        close()
                    }
                    drawPath(gown, color = Color(0xFF7E57C2))
                }
            }

            PromoIconType.DOLPHIN -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF2979FF), Color(0xFF00E5FF))
                        )
                    )
                    // Pink playful dolphin arc
                    val dolphin = Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.55f)
                        quadraticTo(
                            size.width * 0.45f, size.height * 0.2f,
                            size.width * 0.85f, size.height * 0.45f
                        )
                        quadraticTo(
                            size.width * 0.55f, size.height * 0.55f,
                            size.width * 0.2f, size.height * 0.55f
                        )
                        close()
                    }
                    drawPath(dolphin, color = Color(0xFFFF4081))
                }
            }

            PromoIconType.GLOW_FLOWER -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = Color.Black)
                    val petalColors = listOf(
                        NeonPink, NeonOrange, NeonYellow, NeonGreen, NeonCyan, NeonPurple
                    )
                    val petalRadius = size.width * 0.22f
                    val r = size.width * 0.26f
                    for (i in 0 until 6) {
                        val angle = (i * 60.0) * (Math.PI / 180.0)
                        val px = center.x + (r * Math.cos(angle)).toFloat()
                        val py = center.y + (r * Math.sin(angle)).toFloat()
                        drawCircle(
                            color = petalColors[i].copy(alpha = 0.5f),
                            radius = petalRadius,
                            center = Offset(px, py),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = petalRadius * 0.35f,
                            center = Offset(px, py),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    drawCircle(
                        color = Color(0xFFFFD600),
                        radius = size.width * 0.12f,
                        center = center
                    )
                }
            }

            PromoIconType.SPARKLE_NIGHT -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B263B))
                        )
                    )
                    // Festive snowman / stars
                    drawCircle(
                        color = Color.White,
                        radius = size.width * 0.16f,
                        center = Offset(size.width * 0.45f, size.height * 0.65f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = size.width * 0.11f,
                        center = Offset(size.width * 0.45f, size.height * 0.44f)
                    )
                    // Fireworks burst
                    for (i in 0 until 8) {
                        val angle = (i * 45.0) * (Math.PI / 180.0)
                        val ex = size.width * 0.72f + (size.width * 0.18f * Math.cos(angle)).toFloat()
                        val ey = size.height * 0.25f + (size.width * 0.18f * Math.sin(angle)).toFloat()
                        drawLine(
                            color = NeonYellow,
                            start = Offset(size.width * 0.72f, size.height * 0.25f),
                            end = Offset(ex, ey),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

/**
 * Gallery 3-column promo card (Aspect ~ 3:4, white card, thin inner gray frame).
 */
@Composable
fun PromoGalleryCard(
    app: PromoApp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PromoAppIcon(
                    iconType = app.iconType,
                    modifier = Modifier
                        .size(68.dp)
                        .aspectRatio(1f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = app.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

/**
 * Opens the app's Google Play Store page.
 * - Tries market:// intent first (opens Play Store app)
 * - Falls back to https:// in browser
 * - Does nothing if packageName is null/blank (TODO placeholders)
 */
fun openPlayStore(context: android.content.Context, packageName: String?) {
    if (packageName.isNullOrBlank()) return
    try {
        val marketIntent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("market://details?id=$packageName")
        )
        marketIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(marketIntent)
    } catch (e: android.content.ActivityNotFoundException) {
        // Play Store not installed — open browser
        try {
            val webIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
            webIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
        } catch (_: Exception) {}
    }
}

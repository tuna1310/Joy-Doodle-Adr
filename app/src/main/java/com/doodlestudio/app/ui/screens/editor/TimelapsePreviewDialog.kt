package com.doodlestudio.app.ui.screens.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.doodlestudio.app.domain.export.ExportResult
import com.doodlestudio.app.ui.theme.NeonCoralButton
import com.doodlestudio.app.ui.theme.NeonCyan
import com.doodlestudio.app.ui.theme.NeonPink

/**
 * Timelapse Video Preview screen matching video frame 65:
 * - Starry deep violet / navy gradient background
 * - Top-left coral Back button
 * - Center rounded preview thumbnail
 * - "Video Length: N seconds"
 * - "1x" speed indicator
 * - Bottom export / replay buttons
 */
@Composable
fun TimelapsePreviewDialog(
    thumbnail: Bitmap?,
    durationSeconds: Int,
    isExporting: Boolean = false,
    exportProgress: Float = 0f,
    exportResult: ExportResult? = null,
    onDismiss: () -> Unit,
    onExportVideo: () -> Unit,
    onPlayExportedVideo: ((android.net.Uri) -> Unit)? = null,
    onShareExportedVideo: ((android.net.Uri) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = {
            if (!isExporting) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F0C29),
                            Color(0xFF1B1A4A),
                            Color(0xFF24243E)
                        )
                    )
                )
        ) {
            // Draw subtle twinkling stars on background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val starPositions = listOf(
                    Offset(size.width * 0.15f, size.height * 0.12f),
                    Offset(size.width * 0.85f, size.height * 0.08f),
                    Offset(size.width * 0.25f, size.height * 0.35f),
                    Offset(size.width * 0.78f, size.height * 0.42f),
                    Offset(size.width * 0.12f, size.height * 0.75f),
                    Offset(size.width * 0.88f, size.height * 0.82f),
                    Offset(size.width * 0.45f, size.height * 0.92f)
                )
                starPositions.forEach { pos ->
                    drawCircle(color = Color.White.copy(alpha = 0.7f), radius = 2f, center = pos)
                }
            }

            // Top-left coral back button
            if (!isExporting) {
                Box(
                    modifier = Modifier
                        .padding(top = 40.dp, start = 20.dp)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(NeonCoralButton)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "❮", color = Color.White, fontSize = 20.sp)
                }
            }

            // Center Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Preview thumbnail card
                Box(
                    modifier = Modifier
                        .size(width = 220.dp, height = 300.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black)
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnail != null && !thumbnail.isRecycled) {
                        Image(
                            bitmap = thumbnail.asImageBitmap(),
                            contentDescription = "Timelapse Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(text = "🎨", fontSize = 48.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isExporting) {
                    // Exporting in progress
                    Text(
                        text = "Đang quay video tranh vẽ...",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { exportProgress },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NeonCyan,
                        trackColor = Color(0xFF333344)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(exportProgress * 100).toInt()}%",
                        color = NeonCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (exportResult != null) {
                    // Export Result view
                    when (exportResult) {
                        is ExportResult.SuccessVideo -> {
                            Text(
                                text = "🎉 Đã quay và lưu video!",
                                color = Color(0xFF00E676),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Đã lưu vào Thư viện video của bạn",
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                exportResult.uri?.let { uri ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(NeonCyan)
                                            .clickable { onPlayExportedVideo?.invoke(uri) }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text(text = "▶ Xem video", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(NeonPink)
                                            .clickable { onShareExportedVideo?.invoke(uri) }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text(text = "📤 Chia sẻ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF333344))
                                        .clickable(onClick = onDismiss)
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(text = "Đóng", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                        is ExportResult.FallbackImage -> {
                            Text(
                                text = "✨ Đã lưu ảnh tác phẩm!",
                                color = Color(0xFF00E676),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Đã lưu vào Thư viện ảnh (DoodleStudio)",
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF333344))
                                    .clickable(onClick = onDismiss)
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Text(text = "Đóng", color = Color.White, fontSize = 14.sp)
                            }
                        }
                        is ExportResult.Error -> {
                            Text(
                                text = "⚠️ Không thể xuất video",
                                color = Color(0xFFFF5252),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = exportResult.message,
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF333344))
                                    .clickable(onClick = onDismiss)
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Text(text = "Đóng", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    // Ready to record state
                    Text(
                        text = "Video Length: $durationSeconds seconds",
                        color = Color.White,
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // "1x" speed pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "1x",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Bottom 3 action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left green circular button
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "↺", color = Color.White, fontSize = 24.sp)
                        }

                        // Center big white button with inner red record dot
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable(onClick = onExportVideo),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(NeonPink)
                            )
                        }

                        // Right green circular button
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                                .clickable(onClick = onExportVideo),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "↻", color = Color.White, fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

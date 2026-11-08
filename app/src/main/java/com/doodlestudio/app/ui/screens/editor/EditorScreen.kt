package com.doodlestudio.app.ui.screens.editor

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.doodlestudio.app.R
import com.doodlestudio.app.domain.canvas.ActionLogPlayer
import com.doodlestudio.app.domain.canvas.BrushConfig
import com.doodlestudio.app.domain.canvas.BrushTool
import com.doodlestudio.app.domain.export.ExportResult
import com.doodlestudio.app.domain.model.EditAction
import com.doodlestudio.app.ui.components.BannerAdSlot
import com.doodlestudio.app.ui.components.FreeDrawCanvas
import com.doodlestudio.app.ui.theme.NeonCoralButton
import com.doodlestudio.app.ui.theme.NeonCyan
import com.doodlestudio.app.ui.theme.NeonPlayButton
import com.doodlestudio.app.ui.theme.NeonShareButton
import com.doodlestudio.app.ui.theme.NeonToolButtonBg
import com.doodlestudio.app.ui.theme.NeonUndoButton
import com.doodlestudio.app.util.SoundEffectManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File
import java.io.FileOutputStream

@Composable
fun EditorScreen(
    templateId: String?,
    artworkId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToGallery: () -> Unit,
    viewModel: EditorViewModel = koinViewModel(),
    soundEffectManager: SoundEffectManager = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showBrushPanel by remember { mutableStateOf(false) }
    var showEraserPanel by remember { mutableStateOf(false) }
    var showShareSpeedDial by remember { mutableStateOf(false) }
    var showTimelapseDialog by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    val templates by viewModel.templates.collectAsState(initial = emptyList())
    var previewThumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }

    // Replay Mode State
    var isReplayMode by remember { mutableStateOf(false) }
    var isReplayPlaying by remember { mutableStateOf(false) }
    var replayProgress by remember { mutableFloatStateOf(0f) }
    var replaySpeed by remember { mutableIntStateOf(1) }
    var replayActions by remember { mutableStateOf<List<EditAction>>(emptyList()) }

    var canvasSize by remember { mutableStateOf(IntSize(1080, 1920)) }

    val currentOnStrokeStart by rememberUpdatedState(viewModel::onStrokeStart)
    val currentOnStrokeMove by rememberUpdatedState(viewModel::onStrokeMove)
    val currentOnStrokeEnd by rememberUpdatedState(viewModel::onStrokeEnd)

    LaunchedEffect(templateId, artworkId) {
        viewModel.loadArtwork(templateId, artworkId)
    }

    // Auto-save artwork on exit
    val handleBackWithSave: () -> Unit = {
        if (isReplayMode) {
            isReplayMode = false
            isReplayPlaying = false
        } else if (viewModel.freeDrawActions.isNotEmpty()) {
            showExitDialog = true
        } else {
            // Canvas empty — just go back
            onNavigateBack()
        }
    }

    BackHandler {
        handleBackWithSave()
    }

    // Replay loop coroutine
    LaunchedEffect(isReplayPlaying, replaySpeed) {
        if (isReplayPlaying) {
            val fullActions = viewModel.freeDrawActions.toList()
            if (fullActions.isEmpty()) {
                isReplayPlaying = false
                return@LaunchedEffect
            }
            val totalSteps = fullActions.size
            var currentStep = (replayProgress * totalSteps).toInt().coerceIn(0, totalSteps)

            while (isActive && isReplayPlaying && currentStep < totalSteps) {
                currentStep++
                replayProgress = currentStep.toFloat() / totalSteps
                replayActions = fullActions.take(currentStep)
                delay((300 / replaySpeed).toLong())
            }

            if (currentStep >= totalSteps) {
                isReplayPlaying = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Reserved Banner Ad Area (#1E5068)
            BannerAdSlot(height = 92.dp)

            // 2. Full-Screen Black Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .onGloballyPositioned { coordinates ->
                        if (coordinates.size.width > 0 && coordinates.size.height > 0) {
                            canvasSize = coordinates.size
                        }
                    }
                    .then(
                        if (!isReplayMode) {
                            Modifier.pointerInput(isReplayMode, uiState.mode, uiState.template) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()
                                    showBrushPanel = false
                                    showEraserPanel = false
                                    showShareSpeedDial = false

                                    val startTime = System.currentTimeMillis()
                                    val startPos = down.position
                                    var maxDist = 0f
                                    var isDrawing = (uiState.mode == EditorMode.DRAW || uiState.template == null)

                                    if (isDrawing) {
                                        currentOnStrokeStart(down.position.x, down.position.y)
                                    }

                                    val pointerId = down.id
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointerId }
                                        if (change == null || !change.pressed) {
                                            val elapsed = System.currentTimeMillis() - startTime
                                            if (maxDist < 16f && elapsed < 350L && uiState.template != null) {
                                                // Quick tap on template -> Fill region!
                                                val tx = if (canvasSize.width > 0) ((startPos.x / canvasSize.width) * 1024f).toInt().coerceIn(0, 1023) else 0
                                                val ty = if (canvasSize.height > 0) ((startPos.y / canvasSize.height) * 1024f).toInt().coerceIn(0, 1023) else 0
                                                viewModel.onCanvasTap(tx, ty)
                                            }
                                            if (isDrawing) {
                                                currentOnStrokeEnd()
                                            }
                                            break
                                        }
                                        change.consume()
                                        val dist = kotlin.math.hypot(change.position.x - startPos.x, change.position.y - startPos.y)
                                        if (dist > maxDist) maxDist = dist

                                        if (!isDrawing && maxDist >= 16f) {
                                            isDrawing = true
                                            currentOnStrokeStart(startPos.x, startPos.y)
                                        }

                                        if (isDrawing) {
                                            currentOnStrokeMove(change.position.x, change.position.y)
                                        }
                                    }
                                }
                            }
                        } else Modifier
                    )
            ) {
                FreeDrawCanvas(
                    actions = if (isReplayMode) replayActions else viewModel.freeDrawActions.toList(),
                    activePoints = if (isReplayMode) emptyList() else viewModel.activeStrokePoints.toList(),
                    brushConfig = BrushConfig(
                        tool = uiState.brushTool,
                        size = if (uiState.brushTool == BrushTool.ERASER) uiState.eraserSize else uiState.brushSize,
                        color = uiState.selectedColor,
                        alpha = uiState.brushAlpha
                    ),
                    strokeRevision = uiState.strokeRevision,
                    activeSeed = viewModel.currentStrokeSeed,
                    backgroundColor = Color.Black,
                    canvasWidth = canvasSize.width,
                    canvasHeight = canvasSize.height,
                    fillBitmap = viewModel.fillBitmap,
                    fillBitmapRevision = uiState.fillBitmapRevision,
                    templateBitmap = viewModel.templateBitmap,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 3. Bottom Bar: Either Normal 5 Buttons or Replay Playback Bar
            if (isReplayMode) {
                ReplayPlaybackBar(
                    isPlaying = isReplayPlaying,
                    progress = replayProgress,
                    speedMultiplier = replaySpeed,
                    onSpeedClick = {
                        val speeds = listOf(1, 2, 4, 8)
                        val nextIdx = (speeds.indexOf(replaySpeed) + 1) % speeds.size
                        replaySpeed = speeds[nextIdx]
                    },
                    onScrub = { progress ->
                        replayProgress = progress
                        isReplayPlaying = false
                        val count = (progress * viewModel.freeDrawActions.size).toInt()
                        replayActions = viewModel.freeDrawActions.take(count)
                    },
                    onPlayPause = {
                        if (replayProgress >= 1f) {
                            replayProgress = 0f
                            replayActions = emptyList()
                        }
                        isReplayPlaying = !isReplayPlaying
                    },
                    onStop = {
                        isReplayPlaying = false
                        replayProgress = 0f
                        replayActions = emptyList()
                    }
                )
            } else {
                NormalBottomToolbar(
                    isEraserActive = uiState.brushTool == BrushTool.ERASER,
                    onBrushClick = {
                        showEraserPanel = false
                        showBrushPanel = !showBrushPanel
                        showShareSpeedDial = false
                        if (uiState.brushTool == BrushTool.ERASER) {
                            viewModel.setBrushTool(viewModel.lastBrushTool)
                        }
                    },
                    onEraserClick = {
                        showBrushPanel = false
                        showShareSpeedDial = false
                        if (uiState.brushTool == BrushTool.ERASER) {
                            showEraserPanel = !showEraserPanel
                        } else {
                            viewModel.setBrushTool(BrushTool.ERASER)
                            showEraserPanel = true
                        }
                    },
                    onUndoClick = {
                        showBrushPanel = false
                        showEraserPanel = false
                        showShareSpeedDial = false
                        viewModel.undo()
                    },
                    onUndoLongClick = {
                        viewModel.redo()
                    },
                    onPlayClick = {
                        showBrushPanel = false
                        showEraserPanel = false
                        showShareSpeedDial = false
                        if (viewModel.freeDrawActions.isNotEmpty()) {
                            isReplayMode = true
                            isReplayPlaying = true
                            replayProgress = 0f
                            replayActions = emptyList()
                        } else {
                            android.widget.Toast.makeText(context, "Hãy vẽ một vài nét trước khi xem lại!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onShareClick = {
                        showShareSpeedDial = !showShareSpeedDial
                        showBrushPanel = false
                        showEraserPanel = false
                    }
                )
            }
        }

        // Overlay Top Floating Circular Buttons: Back (left) & Actions (right)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 104.dp, start = 16.dp, end = 16.dp),
        ) {
            // Left Coral Back Button (white chevron)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(NeonCoralButton)
                    .clickable { handleBackWithSave() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "❮", color = Color.White, fontSize = 20.sp)
            }

            // Right Action Buttons
            if (isReplayMode) {
                // Exit Replay (white X)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(NeonCoralButton)
                        .clickable {
                            isReplayMode = false
                            isReplayPlaying = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✕", color = Color.White, fontSize = 20.sp)
                }
            } else {
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Template Mode Toggle (Fill Bucket vs Neon Brush) - only visible when a template is active
                    if (uiState.template != null) {
                        val isFillMode = uiState.mode == EditorMode.FILL
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isFillMode) NeonCyan else Color(0xFF1E293B))
                                .clickable {
                                    soundEffectManager.play(SoundEffectManager.Sound.CLICK)
                                    viewModel.setEditorMode(if (isFillMode) EditorMode.DRAW else EditorMode.FILL)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = if (isFillMode) "🪣" else "🖌️", fontSize = 20.sp)
                        }
                    }

                    // 2. Template Picker Button (opens 40 templates dialog)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .clickable {
                                soundEffectManager.play(SoundEffectManager.Sound.CLICK)
                                showTemplatePicker = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎨", fontSize = 22.sp)
                    }

                    // 3. Trash Clear Canvas Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(NeonCoralButton)
                            .clickable {
                                soundEffectManager.play(SoundEffectManager.Sound.CLICK)
                                showClearConfirmDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Template Picker Dialog (40 coloring templates across 8 categories)
        if (showTemplatePicker) {
            TemplatePickerDialog(
                templates = templates,
                currentTemplateId = uiState.templateId,
                onSelectTemplate = { selected ->
                    viewModel.selectTemplate(selected)
                    showTemplatePicker = false
                },
                onSelectBlankCanvas = {
                    viewModel.clearToBlankCanvas()
                    showTemplatePicker = false
                },
                onDismiss = { showTemplatePicker = false }
            )
        }

        // Brush Options Bottom Panel
        AnimatedVisibility(
            visible = showBrushPanel && !isReplayMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        ) {
            BrushOptionsPanel(
                selectedTool = uiState.brushTool,
                onToolSelected = { tool -> viewModel.setBrushTool(tool) },
                selectedColor = uiState.selectedColor,
                onColorSelected = { color -> viewModel.selectColor(color) },
                brushSize = uiState.brushSize,
                onBrushSizeChange = { size -> viewModel.setBrushSize(size) }
            )
        }

        // Eraser Options Bottom Panel
        AnimatedVisibility(
            visible = showEraserPanel && !isReplayMode && uiState.brushTool == BrushTool.ERASER,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        ) {
            EraserOptionsPanel(
                eraserSize = uiState.eraserSize,
                onEraserSizeChange = { size -> viewModel.setEraserSize(size) }
            )
        }

        // Share Vertical Speed Dial Popup
        AnimatedVisibility(
            visible = showShareSpeedDial && !isReplayMode,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 88.dp, end = 16.dp)
        ) {
            ShareSpeedDial(
                onCameraClick = {
                    showShareSpeedDial = false
                    viewModel.saveArtwork { success ->
                        if (success) {
                            soundEffectManager.play(SoundEffectManager.Sound.CHIME)
                        }
                    }
                },
                onTimelapseClick = {
                    showShareSpeedDial = false
                    viewModel.saveArtwork { _ ->
                        previewThumbnail = viewModel.getPreviewThumbnail(360, 480)
                        exportResult = null
                        showTimelapseDialog = true
                    }
                },
                onPhotoClick = {
                    showShareSpeedDial = false
                    // Stub for photo import
                },
                onShareClick = {
                    showShareSpeedDial = false
                    viewModel.saveArtwork { success ->
                        if (success) {
                            shareArtworkImage(context, viewModel)
                        }
                    }
                }
            )
        }

        // Clear Canvas Confirmation Dialog
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text(text = stringResource(R.string.clear_canvas_title)) },
                text = { Text(text = stringResource(R.string.clear_canvas_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearConfirmDialog = false
                            soundEffectManager.play(SoundEffectManager.Sound.POP)
                            viewModel.reset()
                        }
                    ) {
                        Text(text = stringResource(R.string.btn_clear), color = NeonCoralButton)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text(text = stringResource(R.string.btn_cancel), color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1E1E1E),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFCCCCCC)
            )
        }

        // Exit Dialog: Save / Discard / Continue
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = {
                    Text(
                        text = "Bạn muốn làm gì với bức tranh?",
                        fontSize = 18.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = "What do you want to do with your drawing?",
                        fontSize = 13.sp,
                        color = Color(0xFFCCCCCC)
                    )
                },
                confirmButton = {
                    // "Lưu lại" button — coral/pink
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            viewModel.saveArtwork { _ ->
                                onNavigateBack()
                            }
                        }
                    ) {
                        Text(text = "💾 Lưu lại", color = Color(0xFFFF5B8A), fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.foundation.layout.Row {
                        // "Xóa đi" button — orange
                        TextButton(onClick = {
                            showExitDialog = false
                            // For existing artwork, just navigate back without saving new changes
                            // For new blank canvas, also just navigate back
                            onNavigateBack()
                        }) {
                            Text(text = "🗑 Xóa đi", color = Color(0xFFFF8A00), fontSize = 15.sp)
                        }
                        // "Vẽ tiếp" button — cyan
                        TextButton(onClick = { showExitDialog = false }) {
                            Text(text = "✏️ Vẽ tiếp", color = Color(0xFF00E5FF), fontSize = 15.sp)
                        }
                    }
                },
                containerColor = Color(0xFF1A1A2E),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            )
        }

        // Timelapse Video Preview Screen Dialog
        if (showTimelapseDialog) {
            TimelapsePreviewDialog(
                thumbnail = previewThumbnail,
                durationSeconds = (viewModel.freeDrawActions.size * 0.1f).toInt().coerceAtLeast(2),
                isExporting = uiState.isExporting,
                exportProgress = uiState.exportProgress,
                exportResult = exportResult,
                onDismiss = {
                    showTimelapseDialog = false
                    exportResult = null
                },
                onExportVideo = {
                    viewModel.exportTimelapseVideo { result ->
                        exportResult = result
                        if (result is ExportResult.SuccessVideo) {
                            soundEffectManager.play(SoundEffectManager.Sound.CHIME)
                        }
                    }
                },
                onPlayExportedVideo = { uri ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "video/mp4")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        android.widget.Toast.makeText(context, "Không tìm thấy trình phát video phù hợp", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onShareExportedVideo = { uri ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/mp4"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, "Chia sẻ video"))
                    } catch (_: Exception) {}
                }
            )
        }
    }
}

/**
 * 5 Pastel Round Buttons Bottom Toolbar:
 * 1. Brush (#DDF1FA)
 * 2. Eraser (#DDF1FA)
 * 3. Undo (#6FE0D0)
 * 4. Play (#3DB8F5)
 * 5. Share (#EE5060)
 */
@Composable
private fun NormalBottomToolbar(
    isEraserActive: Boolean,
    onBrushClick: () -> Unit,
    onEraserClick: () -> Unit,
    onUndoClick: () -> Unit,
    onUndoLongClick: () -> Unit,
    onPlayClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val isBrushActive = !isEraserActive
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(Color.Black)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Brush Button (bg #DDF1FA) — active when NOT eraser
        RoundToolButton(
            backgroundColor = NeonToolButtonBg,
            isActive = isBrushActive,
            onClick = onBrushClick
        ) {
            // Paintbrush icon
            Text(text = "🖌️", fontSize = 22.sp)
        }

        // 2. Eraser Button (bg #DDF1FA) — active when eraser selected
        RoundToolButton(
            backgroundColor = NeonToolButtonBg,
            isActive = isEraserActive,
            onClick = onEraserClick
        ) {
            // Proper eraser icon: orange-red body + blue-grey tip
            EraserIcon(size = 26.dp)
        }

        // 3. Undo Button (bg #6FE0D0)
        RoundToolButton(
            backgroundColor = NeonUndoButton,
            onClick = onUndoClick,
            onLongClick = onUndoLongClick
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_undo),
                contentDescription = "Undo",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }

        // 4. Play Button (bg #3DB8F5, ▶)
        RoundToolButton(
            backgroundColor = NeonPlayButton,
            onClick = onPlayClick
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // 5. Share Button (bg #EE5060, Paper-plane)
        RoundToolButton(
            backgroundColor = NeonShareButton,
            onClick = onShareClick
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Share",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun RoundToolButton(
    backgroundColor: Color,
    isActive: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    // Outer ring when active — white border, clearly visible for children
    val outerModifier = if (isActive) {
        Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(Color.White)
            .padding(3.dp)
    } else {
        Modifier.size(52.dp)
    }

    Box(
        modifier = outerModifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onLongClick?.invoke() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/** Eraser icon: orange-red rectangular body with a blue-grey cap on the left */
@Composable
private fun EraserIcon(size: androidx.compose.ui.unit.Dp) {
    Canvas(
        modifier = Modifier.size(size)
    ) {
        val w = this.size.width
        val h = this.size.height
        val capWidth = w * 0.28f

        // Orange-red body (right 72%)
        drawRoundRect(
            color = Color(0xFFE53935),
            topLeft = androidx.compose.ui.geometry.Offset(capWidth, h * 0.2f),
            size = androidx.compose.ui.geometry.Size(w - capWidth, h * 0.6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )

        // Blue-grey cap (left 28%)
        drawRoundRect(
            color = Color(0xFF78909C),
            topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.2f),
            size = androidx.compose.ui.geometry.Size(capWidth + 2f, h * 0.6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )

        // White highlight stripe on body
        drawRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = androidx.compose.ui.geometry.Offset(capWidth + 2f, h * 0.2f),
            size = androidx.compose.ui.geometry.Size(w - capWidth - 2f, h * 0.22f)
        )
    }
}

private fun shareArtworkImage(context: android.content.Context, viewModel: EditorViewModel) {
    try {
        val actionLog = viewModel.exportActionLog()
        val file = File(context.cacheDir, "doodle_artwork_${System.currentTimeMillis()}.png")
        // Use repository or export
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Drawing"))
    } catch (_: Exception) {}
}

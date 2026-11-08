package com.doodlestudio.app.ui.screens.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doodlestudio.app.domain.canvas.BrushConfig
import com.doodlestudio.app.domain.canvas.BrushTool
import com.doodlestudio.app.domain.canvas.brush.BrushRegistry
import com.doodlestudio.app.domain.canvas.HistoryManager
import com.doodlestudio.app.domain.canvas.LabelMapBuilder
import com.doodlestudio.app.domain.canvas.RegionFiller
import com.doodlestudio.app.domain.canvas.StrokeInterpolator
import com.doodlestudio.app.domain.export.ExportResult
import com.doodlestudio.app.domain.export.VideoEncoder
import com.doodlestudio.app.domain.model.ActionLog
import com.doodlestudio.app.domain.model.Artwork
import com.doodlestudio.app.domain.model.EditAction
import com.doodlestudio.app.domain.model.PointF
import com.doodlestudio.app.domain.model.Template
import com.doodlestudio.app.domain.repository.ArtworkRepository
import com.doodlestudio.app.domain.repository.TemplateRepository
import com.doodlestudio.app.ui.components.NeonBrushRenderer
import com.doodlestudio.app.ui.theme.NeonPink
import com.doodlestudio.app.ui.theme.NeonYellow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class EditorMode {
    FILL,
    DRAW
}

data class EditorUiState(
    val isLoading: Boolean = true,
    val artworkId: String? = null,
    val templateId: String? = null,
    val template: Template? = null,
    val isBlankCanvas: Boolean = true,
    val title: String = "Neon Doodle",
    val mode: EditorMode = EditorMode.DRAW,
    val selectedColor: Color = NeonPink,
    val brushTool: BrushTool = BrushTool.NEON,
    val brushSize: Float = 14f,
    val eraserSize: Float = 36f,
    val brushAlpha: Float = 1.0f,
    val backgroundColor: Color = Color.Black,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val regionCount: Int = 0,
    val fillBitmapRevision: Int = 0,
    val strokeRevision: Int = 0,
    val isSaved: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f
)

class EditorViewModel(
    private val templateRepository: TemplateRepository,
    private val artworkRepository: ArtworkRepository,
    private val context: Context,
    private val soundEffectManager: com.doodlestudio.app.util.SoundEffectManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val historyManager = HistoryManager(maxHistorySize = 50)
    private var labelMapResult: LabelMapBuilder.LabelMapResult? = null
    private var regionFiller: RegionFiller? = null
    var templateBitmap: Bitmap? = null
        private set

    val templates = templateRepository.getTemplates()

    // Mutable working bitmap for the fill layer
    var fillBitmap: Bitmap? = null
        private set

    // Completed free-hand drawing actions (strokes and erasures)
    val freeDrawActions = mutableListOf<EditAction>()

    // Current stroke points being actively drawn
    val activeStrokePoints = mutableListOf<PointF>()
    var currentStrokeSeed: Long = System.currentTimeMillis()
        private set

    fun loadArtwork(templateId: String?, artworkId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            if (artworkId != null) {
                // Resume existing artwork from Library
                loadExistingArtwork(artworkId)
            } else if (templateId == null || templateId == "blank") {
                // New blank canvas
                initBlankCanvas()
            } else {
                // New artwork from template
                initTemplateArtwork(templateId)
            }
        }
    }

    var lastBrushTool: BrushTool = BrushTool.NEON
        private set

    private fun initBlankCanvas() {
        val newArtId = "artwork_${UUID.randomUUID().toString().take(8)}"
        _uiState.update {
            it.copy(
                isLoading = false,
                artworkId = newArtId,
                templateId = null,
                template = null,
                isBlankCanvas = true,
                title = "Neon Doodle",
                mode = EditorMode.DRAW,
                backgroundColor = Color.Black,
                selectedColor = NeonPink,
                brushTool = BrushTool.NEON,
                brushSize = 14f,
                brushAlpha = 1.0f,
                canUndo = false,
                canRedo = false
            )
        }
    }

    private suspend fun initTemplateArtwork(templateId: String) {
        val template = templateRepository.getTemplateById(templateId)
        if (template != null) {
            val (decoded, lmResult) = decodeTemplateAndBuildLabelMap(template.imagePath)
            if (decoded != null && lmResult != null) {
                templateBitmap = decoded
                labelMapResult = lmResult
                regionFiller = RegionFiller(lmResult, historyManager)

                val bmp = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888)
                fillBitmap = bmp

                val newArtId = "artwork_${UUID.randomUUID().toString().take(8)}"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        artworkId = newArtId,
                        templateId = templateId,
                        template = template,
                        isBlankCanvas = false,
                        title = template.nameEn,
                        mode = EditorMode.FILL,
                        regionCount = lmResult.regionCount,
                        canUndo = false,
                        canRedo = false,
                        fillBitmapRevision = 0,
                        strokeRevision = 0
                    )
                }
                return
            }
        }
        _uiState.update { it.copy(isLoading = false) }
    }

    private suspend fun loadExistingArtwork(artId: String) {
        val artwork = artworkRepository.getArtworkById(artId)
        val actionLog = artworkRepository.loadActionLog(artId)

        if (artwork == null || actionLog == null) {
            initBlankCanvas()
            return
        }

        if (artwork.templateId != null) {
            val template = templateRepository.getTemplateById(artwork.templateId)
            if (template != null) {
                val (decoded, lmResult) = decodeTemplateAndBuildLabelMap(template.imagePath)
                if (decoded != null && lmResult != null) {
                    templateBitmap = decoded
                    labelMapResult = lmResult
                    val filler = RegionFiller(lmResult, historyManager)
                    regionFiller = filler

                    val bmp = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888)
                    fillBitmap = bmp

                    // Replay all actions from ActionLog
                    replayActions(actionLog.actions, filler, bmp, lmResult)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            artworkId = artwork.id,
                            templateId = artwork.templateId,
                            template = template,
                            isBlankCanvas = false,
                            title = artwork.title,
                            mode = EditorMode.FILL,
                            backgroundColor = Color(actionLog.backgroundColor),
                            regionCount = lmResult.regionCount,
                            canUndo = historyManager.canUndo.value,
                            canRedo = historyManager.canRedo.value,
                            fillBitmapRevision = 1,
                            strokeRevision = 1
                        )
                    }
                    return
                }
            }
        }

        // Blank canvas existing artwork
        replayActions(actionLog.actions, null, null, null)
        _uiState.update {
            it.copy(
                isLoading = false,
                artworkId = artwork.id,
                templateId = null,
                template = null,
                isBlankCanvas = true,
                title = artwork.title,
                mode = EditorMode.DRAW,
                backgroundColor = Color(actionLog.backgroundColor),
                selectedColor = NeonPink,
                brushTool = BrushTool.NEON,
                brushSize = 14f,
                brushAlpha = 1.0f,
                canUndo = historyManager.canUndo.value,
                canRedo = historyManager.canRedo.value,
                strokeRevision = 1
            )
        }
    }

    private fun replayActions(
        actions: List<EditAction>,
        filler: RegionFiller?,
        fillBmp: Bitmap?,
        lm: LabelMapBuilder.LabelMapResult?
    ) {
        historyManager.reset()
        freeDrawActions.clear()

        for (action in actions) {
            when (action) {
                is EditAction.Fill -> {
                    filler?.applyFill(action)
                    historyManager.recordAction(action)
                }
                is EditAction.Stroke, is EditAction.Erase -> {
                    freeDrawActions.add(action)
                    historyManager.recordAction(action)
                }
            }
        }

        if (filler != null && fillBmp != null && lm != null) {
            fillBmp.setPixels(filler.fillPixels, 0, lm.width, 0, 0, lm.width, lm.height)
        }
    }

    private suspend fun decodeTemplateAndBuildLabelMap(imagePath: String): Pair<Bitmap?, LabelMapBuilder.LabelMapResult?> =
        withContext(Dispatchers.IO) {
            try {
                context.assets.open(imagePath).use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream)
                    if (decoded != null) {
                        val w = decoded.width
                        val h = decoded.height
                        val pixels = IntArray(w * h)
                        decoded.getPixels(pixels, 0, w, 0, 0, w, h)
                        val lm = LabelMapBuilder.buildFromPixels(
                            pixels = pixels,
                            width = w,
                            height = h,
                            hasAlpha = decoded.hasAlpha(),
                            dilationPixels = 1
                        )
                        decoded to lm
                    } else {
                        null to null
                    }
                }
            } catch (e: Exception) {
                null to null
            }
        }

    fun selectTemplate(template: Template) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val (decoded, lmResult) = decodeTemplateAndBuildLabelMap(template.imagePath)
            if (decoded != null && lmResult != null) {
                templateBitmap = decoded
                labelMapResult = lmResult
                val filler = RegionFiller(lmResult, historyManager)
                regionFiller = filler
                val bmp = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888)
                fillBitmap = bmp
                historyManager.reset()
                freeDrawActions.clear()
                activeStrokePoints.clear()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        templateId = template.id,
                        template = template,
                        isBlankCanvas = false,
                        title = template.displayTitleVi,
                        mode = EditorMode.FILL,
                        regionCount = lmResult.regionCount,
                        canUndo = false,
                        canRedo = false,
                        fillBitmapRevision = it.fillBitmapRevision + 1,
                        strokeRevision = it.strokeRevision + 1,
                        isSaved = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearToBlankCanvas() {
        templateBitmap = null
        labelMapResult = null
        regionFiller = null
        fillBitmap = null
        historyManager.reset()
        freeDrawActions.clear()
        activeStrokePoints.clear()
        _uiState.update {
            it.copy(
                templateId = null,
                template = null,
                isBlankCanvas = true,
                title = "Neon Doodle",
                mode = EditorMode.DRAW,
                regionCount = 0,
                canUndo = false,
                canRedo = false,
                fillBitmapRevision = it.fillBitmapRevision + 1,
                strokeRevision = it.strokeRevision + 1,
                isSaved = false
            )
        }
    }

    fun setEditorMode(mode: EditorMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun setBrushTool(tool: BrushTool) {
        if (tool != BrushTool.ERASER) {
            lastBrushTool = tool
        }
        _uiState.update { it.copy(brushTool = tool) }
    }

    fun setBrushSize(size: Float) {
        _uiState.update { it.copy(brushSize = size) }
    }

    fun setEraserSize(size: Float) {
        _uiState.update { it.copy(eraserSize = size) }
    }

    fun setBrushAlpha(alpha: Float) {
        _uiState.update { it.copy(brushAlpha = alpha.coerceIn(0.1f, 1.0f)) }
    }

    fun setBackgroundColor(color: Color) {
        _uiState.update { it.copy(backgroundColor = color) }
    }

    fun selectColor(color: Color) {
        _uiState.update { it.copy(selectedColor = color) }
    }

    fun onCanvasTap(canvasX: Int, canvasY: Int) {
        val lm = labelMapResult ?: return
        val filler = regionFiller ?: return
        val bmp = fillBitmap ?: return

        val regionId = lm.findNearestRegionId(canvasX, canvasY).toInt()
        if (regionId > 0) {
            val colorInt = _uiState.value.selectedColor.toArgb()
            val action = filler.fill(
                regionId = regionId,
                color = colorInt,
                normalizedX = canvasX.toFloat() / lm.width,
                normalizedY = canvasY.toFloat() / lm.height
            )

            if (action != null) {
                soundEffectManager?.play(com.doodlestudio.app.util.SoundEffectManager.Sound.POP)
                bmp.setPixels(filler.fillPixels, 0, lm.width, 0, 0, lm.width, lm.height)
                _uiState.update {
                    it.copy(
                        canUndo = historyManager.canUndo.value,
                        canRedo = historyManager.canRedo.value,
                        fillBitmapRevision = it.fillBitmapRevision + 1,
                        isSaved = false
                    )
                }
            }
        }
    }

    fun onStrokeStart(x: Float, y: Float) {
        currentStrokeSeed = System.currentTimeMillis() xor (java.lang.Float.floatToRawIntBits(x).toLong() shl 16) xor java.lang.Float.floatToRawIntBits(y).toLong()
        activeStrokePoints.clear()
        activeStrokePoints.add(PointF(x, y))
        _uiState.update { it.copy(strokeRevision = it.strokeRevision + 1) }
    }

    fun onStrokeMove(x: Float, y: Float) {
        if (activeStrokePoints.isNotEmpty()) {
            val last = activeStrokePoints.last()
            val dx = x - last.x
            val dy = y - last.y
            if (dx * dx + dy * dy >= 1.0f) { // Min 1.0px movement for instant responsiveness
                activeStrokePoints.add(PointF(x, y))
                _uiState.update { it.copy(strokeRevision = it.strokeRevision + 1) }
            }
        } else {
            activeStrokePoints.add(PointF(x, y))
            _uiState.update { it.copy(strokeRevision = it.strokeRevision + 1) }
        }
    }

    fun onStrokeEnd() {
        if (activeStrokePoints.isEmpty()) return

        // Single points (taps) and 2-point micro-strokes are preserved directly without dropping
        val smoothed = if (activeStrokePoints.size <= 2) {
            activeStrokePoints.toList()
        } else {
            StrokeInterpolator.downsamplePoints(activeStrokePoints, minDistance = 1.5f)
        }
        activeStrokePoints.clear()

        val state = _uiState.value
        val config = BrushConfig(
            tool = state.brushTool,
            size = state.brushSize,
            color = state.selectedColor,
            alpha = state.brushAlpha
        )

        val action: EditAction = if (config.tool == BrushTool.ERASER) {
            EditAction.Erase(
                points = smoothed,
                strokeWidth = state.eraserSize,
                timestampMs = System.currentTimeMillis()
            )
        } else {
            EditAction.Stroke(
                points = smoothed,
                color = config.effectiveArgb,
                strokeWidth = config.size,
                alpha = config.alpha,
                toolType = config.tool.legacyOrName,
                seed = currentStrokeSeed,
                timestampMs = System.currentTimeMillis()
            )
        }

        freeDrawActions.add(action)
        historyManager.recordAction(action)

        _uiState.update {
            it.copy(
                canUndo = historyManager.canUndo.value,
                canRedo = historyManager.canRedo.value,
                strokeRevision = it.strokeRevision + 1,
                isSaved = false
            )
        }
    }

    fun undo() {
        val action = historyManager.undo() ?: return
        soundEffectManager?.play(com.doodlestudio.app.util.SoundEffectManager.Sound.CLICK)
        when (action) {
            is EditAction.Fill -> {
                regionFiller?.revertFill(action)
                syncFillBitmap()
            }
            is EditAction.Stroke, is EditAction.Erase -> {
                freeDrawActions.remove(action)
            }
        }
        _uiState.update {
            it.copy(
                canUndo = historyManager.canUndo.value,
                canRedo = historyManager.canRedo.value,
                fillBitmapRevision = it.fillBitmapRevision + 1,
                strokeRevision = it.strokeRevision + 1,
                isSaved = false
            )
        }
    }

    fun redo() {
        val action = historyManager.redo() ?: return
        soundEffectManager?.play(com.doodlestudio.app.util.SoundEffectManager.Sound.CLICK)
        when (action) {
            is EditAction.Fill -> {
                regionFiller?.applyFill(action)
                syncFillBitmap()
            }
            is EditAction.Stroke, is EditAction.Erase -> {
                freeDrawActions.add(action)
            }
        }
        _uiState.update {
            it.copy(
                canUndo = historyManager.canUndo.value,
                canRedo = historyManager.canRedo.value,
                fillBitmapRevision = it.fillBitmapRevision + 1,
                strokeRevision = it.strokeRevision + 1,
                isSaved = false
            )
        }
    }

    fun reset() {
        historyManager.reset()
        regionFiller?.reset()
        freeDrawActions.clear()
        activeStrokePoints.clear()
        fillBitmap?.eraseColor(android.graphics.Color.TRANSPARENT)
        syncFillBitmap()

        _uiState.update {
            it.copy(
                canUndo = false,
                canRedo = false,
                fillBitmapRevision = it.fillBitmapRevision + 1,
                strokeRevision = it.strokeRevision + 1,
                isSaved = false
            )
        }
    }

    fun getPreviewThumbnail(width: Int = 360, height: Int = 480): Bitmap {
        val state = _uiState.value
        val fullW = labelMapResult?.width ?: 1024
        val fullH = labelMapResult?.height ?: 1024
        val composite = renderCompositeBitmap(
            width = fullW,
            height = fullH,
            backgroundColor = state.backgroundColor.toArgb(),
            fillBitmap = fillBitmap,
            freeDrawActions = freeDrawActions.toList(),
            templateBitmap = templateBitmap
        )
        return Bitmap.createScaledBitmap(composite, width, height, true)
    }

    fun saveArtwork(onSaved: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val artId = state.artworkId ?: "artwork_${UUID.randomUUID().toString().take(8)}"
                val actionLog = exportActionLog()

                // Render composite full bitmap
                val fullW = labelMapResult?.width ?: 1024
                val fullH = labelMapResult?.height ?: 1024
                val composite = renderCompositeBitmap(
                    width = fullW,
                    height = fullH,
                    backgroundColor = state.backgroundColor.toArgb(),
                    fillBitmap = fillBitmap,
                    freeDrawActions = freeDrawActions,
                    templateBitmap = templateBitmap
                )

                // Render 512x512 thumbnail with black background
                val thumb = Bitmap.createScaledBitmap(composite, 512, 512, true)

                val artwork = Artwork(
                    id = artId,
                    title = state.title,
                    templateId = state.templateId,
                    isBlankCanvas = state.isBlankCanvas,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                artworkRepository.saveArtwork(artwork, actionLog, thumb, composite)
                soundEffectManager?.play(com.doodlestudio.app.util.SoundEffectManager.Sound.CHIME)

                _uiState.update { it.copy(artworkId = artId, isSaved = true) }
                onSaved(true)
            } catch (e: Exception) {
                onSaved(false)
            }
        }
    }

    fun exportTimelapseVideo(onComplete: (ExportResult) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportProgress = 0f) }
            val actionLog = exportActionLog()
            val encoder = VideoEncoder(context)
            val result = encoder.encodeTimelapse(
                actionLog = actionLog,
                templateBitmap = templateBitmap,
                onProgress = { p -> _uiState.update { it.copy(exportProgress = p) } }
            )
            _uiState.update { it.copy(isExporting = false) }
            onComplete(result)
        }
    }

    fun exportActionLog(): ActionLog {
        val state = _uiState.value
        val actions = historyManager.getActionLog()
        val duration = if (actions.size > 1) {
            actions.last().timestampMs - actions.first().timestampMs
        } else {
            0L
        }

        return ActionLog(
            version = 1,
            artworkId = state.artworkId ?: "art_${System.currentTimeMillis()}",
            templateId = state.templateId,
            isBlankCanvas = state.isBlankCanvas,
            canvasWidth = labelMapResult?.width ?: 1024,
            canvasHeight = labelMapResult?.height ?: 1024,
            backgroundColor = state.backgroundColor.toArgb(),
            actions = actions,
            totalDurationMs = duration
        )
    }

    private fun renderCompositeBitmap(
        width: Int,
        height: Int,
        backgroundColor: Int,
        fillBitmap: Bitmap?,
        freeDrawActions: List<EditAction>,
        templateBitmap: Bitmap?
    ): Bitmap {
        val composite = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(composite)

        // 1. Background (Black)
        canvas.drawColor(backgroundColor)

        // 2. Fill Layer
        if (fillBitmap != null) {
            canvas.drawBitmap(fillBitmap, 0f, 0f, null)
        }

        // 3. Free Draw Layer
        for (action in freeDrawActions) {
            when (action) {
                is EditAction.Stroke -> {
                    BrushRegistry.get(action.toolType).drawCommitted(
                        canvas = canvas,
                        points = action.points,
                        color = action.color,
                        strokeWidth = action.strokeWidth,
                        alpha = action.alpha,
                        scaleX = 1f,
                        scaleY = 1f,
                        seed = action.seed
                    )
                }
                is EditAction.Erase -> {
                    NeonBrushRenderer.drawNeonStroke(
                        canvas = canvas,
                        points = action.points,
                        color = android.graphics.Color.TRANSPARENT,
                        strokeWidth = action.strokeWidth,
                        alpha = 1.0f,
                        toolType = "ERASER",
                        scaleX = 1f,
                        scaleY = 1f
                    )
                }
                is EditAction.Fill -> {}
            }
        }

        // 4. Template Layer on top (if present)
        if (templateBitmap != null) {
            canvas.drawBitmap(templateBitmap, 0f, 0f, null)
        }

        return composite
    }

    private fun syncFillBitmap() {
        val filler = regionFiller ?: return
        val lm = labelMapResult ?: return
        val bmp = fillBitmap ?: return
        bmp.setPixels(filler.fillPixels, 0, lm.width, 0, 0, lm.width, lm.height)
    }
}

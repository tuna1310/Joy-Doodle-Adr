package com.doodlestudio.app.domain.canvas

import com.doodlestudio.app.domain.model.EditAction

/**
 * Region Coloring Engine based on LabelMap.
 * Renders region colors into an ARGB pixel buffer instantaneously.
 */
class RegionFiller(
    val labelMapResult: LabelMapBuilder.LabelMapResult,
    private val historyManager: HistoryManager = HistoryManager()
) {
    val width = labelMapResult.width
    val height = labelMapResult.height
    val regionCount = labelMapResult.regionCount

    // Region color lookup table (1..regionCount). 0 is transparent/uncolored.
    private val regionColors = IntArray(regionCount + 1) { 0 }

    // Working ARGB_8888 pixel buffer for the fill layer
    val fillPixels = IntArray(width * height) { 0 }

    val canUndo = historyManager.canUndo
    val canRedo = historyManager.canRedo

    /**
     * Fills a region with a new color.
     * Returns the created EditAction.Fill, or null if the region already has this color or is invalid.
     */
    fun fill(regionId: Int, color: Int, normalizedX: Float = 0f, normalizedY: Float = 0f): EditAction.Fill? {
        if (regionId <= 0 || regionId > regionCount) return null
        val prevColor = regionColors[regionId]
        if (prevColor == color) return null // Skip redundant action

        regionColors[regionId] = color
        recolorRegion(regionId.toShort(), color)

        val action = EditAction.Fill(
            regionId = regionId,
            color = color,
            prevColor = prevColor,
            normalizedX = normalizedX,
            normalizedY = normalizedY,
            timestampMs = System.currentTimeMillis()
        )
        historyManager.recordAction(action)
        return action
    }

    /**
     * Undoes the last coloring action.
     */
    fun undo(): EditAction.Fill? {
        val action = historyManager.undo() as? EditAction.Fill ?: return null
        revertFill(action)
        return action
    }

    /**
     * Redoes the last undone coloring action.
     */
    fun redo(): EditAction.Fill? {
        val action = historyManager.redo() as? EditAction.Fill ?: return null
        applyFill(action)
        return action
    }

    fun applyFill(action: EditAction.Fill) {
        regionColors[action.regionId] = action.color
        recolorRegion(action.regionId.toShort(), action.color)
    }

    fun revertFill(action: EditAction.Fill) {
        regionColors[action.regionId] = action.prevColor
        recolorRegion(action.regionId.toShort(), action.prevColor)
    }

    fun getRegionColor(regionId: Int): Int {
        return if (regionId in 1..regionCount) regionColors[regionId] else 0
    }

    fun reset() {
        historyManager.reset()
        for (i in 1..regionCount) {
            regionColors[i] = 0
        }
        for (i in fillPixels.indices) {
            fillPixels[i] = 0
        }
    }

    fun getActionLog(): List<EditAction.Fill> = historyManager.getActionLog().filterIsInstance<EditAction.Fill>()

    private fun recolorRegion(targetRegionId: Short, newColor: Int) {
        val labels = labelMapResult.labelMap
        val size = width * height
        for (i in 0 until size) {
            if (labels[i] == targetRegionId) {
                fillPixels[i] = newColor
            }
        }
    }
}

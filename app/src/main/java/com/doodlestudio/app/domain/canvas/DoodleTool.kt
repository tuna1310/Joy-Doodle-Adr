package com.doodlestudio.app.domain.canvas

import androidx.compose.ui.graphics.Color
import com.doodlestudio.app.ui.theme.NeonYellow

enum class BrushTool(val toolId: String, val displayNameEn: String, val displayNameVi: String) {
    CLASSIC_NEON("CLASSIC_NEON", "Classic Neon", "Classic Neon"),
    RAINBOW_GLOW("RAINBOW_GLOW", "Rainbow Glow", "Rainbow Glow"),
    CALLIGRAPHY_RIBBON("CALLIGRAPHY_RIBBON", "Ribbon", "Ribbon"),
    DOUBLE_STROKE("DOUBLE_STROKE", "Double Line", "Double Line"),
    THIN_NEON_PEN("THIN_NEON_PEN", "Thin Neon", "Thin Neon"),
    CRAYON_CHALK("CRAYON_CHALK", "Crayon", "Crayon"),
    SOLID_MARKER("SOLID_MARKER", "Solid Marker", "Solid Marker"),
    DOTTED_PEARL("DOTTED_PEARL", "Dotted Pearl", "Dotted Pearl"),
    SOFT_BLUR("SOFT_BLUR", "Soft Blur", "Soft Blur"),
    CONCENTRIC_RINGS("CONCENTRIC_RINGS", "Rings", "Rings"),
    SPARKLER("SPARKLER", "Sparkler", "Sparkler"),
    FIREWORKS("FIREWORKS", "Fireworks", "Fireworks"),
    NEON_STARS("NEON_STARS", "Stars", "Stars"),
    CROSS_SPARKLES("CROSS_SPARKLES", "Sparkles", "Sparkles"),
    BOKEH_DOTS("BOKEH_DOTS", "Bokeh", "Bokeh"),
    BIG_CROSS_STAR("BIG_CROSS_STAR", "Cross Star", "Cross Star"),
    PURPLE_SPARKLES("PURPLE_SPARKLES", "Purple Sparkle", "Purple Sparkle"),
    FAIRY_DUST("FAIRY_DUST", "Fairy Dust", "Fairy Dust"),
    SNOWFLAKES("SNOWFLAKES", "Snowflakes", "Snowflakes"),
    HEART_BUBBLES("HEART_BUBBLES", "Heart Bubbles", "Heart Bubbles"),
    SOAP_BUBBLES("SOAP_BUBBLES", "Soap Bubbles", "Soap Bubbles"),

    ERASER("ERASER", "Eraser", "Eraser");

    val legacyOrName: String
        get() = when (this) {
            CLASSIC_NEON -> "NEON"
            THIN_NEON_PEN -> "PEN"
            RAINBOW_GLOW -> "RAINBOW"
            CROSS_SPARKLES -> "SPARKLE"
            NEON_STARS -> "STARS"
            SOLID_MARKER -> "MARKER"
            else -> this.name
        }

    companion object {
        val NEON get() = CLASSIC_NEON
        val RAINBOW get() = RAINBOW_GLOW
        val SPARKLE get() = CROSS_SPARKLES
        val STARS get() = NEON_STARS
        val PEN get() = THIN_NEON_PEN
        val MARKER get() = SOLID_MARKER

        fun fromString(id: String): BrushTool = when (id.uppercase()) {
            "NEON", "CLASSIC_NEON" -> CLASSIC_NEON
            "PEN", "THIN_NEON_PEN" -> THIN_NEON_PEN
            "RAINBOW", "RAINBOW_GLOW" -> RAINBOW_GLOW
            "CALLIGRAPHY_RIBBON" -> CALLIGRAPHY_RIBBON
            "DOUBLE_STROKE" -> DOUBLE_STROKE
            "CRAYON_CHALK" -> CRAYON_CHALK
            "SOLID_MARKER", "MARKER" -> SOLID_MARKER
            "THIN_NEON_PEN" -> THIN_NEON_PEN
            "DOTTED_PEARL" -> DOTTED_PEARL
            "SOFT_BLUR" -> SOFT_BLUR
            "CONCENTRIC_RINGS" -> CONCENTRIC_RINGS
            "SPARKLER" -> SPARKLER
            "FIREWORKS" -> FIREWORKS
            "NEON_STARS", "STARS" -> NEON_STARS
            "CROSS_SPARKLES", "SPARKLE" -> CROSS_SPARKLES
            "BOKEH_DOTS" -> BOKEH_DOTS
            "BIG_CROSS_STAR" -> BIG_CROSS_STAR
            "PURPLE_SPARKLES" -> PURPLE_SPARKLES
            "FAIRY_DUST" -> FAIRY_DUST
            "SNOWFLAKES" -> SNOWFLAKES
            "HEART_BUBBLES" -> HEART_BUBBLES
            "SOAP_BUBBLES" -> SOAP_BUBBLES
            "ERASER" -> ERASER
            else -> CLASSIC_NEON
        }
    }
}

data class BrushConfig(
    val tool: BrushTool = BrushTool.NEON,
    val size: Float = 12f,
    val color: Color = NeonYellow,
    val alpha: Float = 1.0f
) {
    val effectiveArgb: Int
        get() {
            val a = ((color.alpha * alpha) * 255).toInt().coerceIn(0, 255)
            val r = (color.red * 255).toInt().coerceIn(0, 255)
            val g = (color.green * 255).toInt().coerceIn(0, 255)
            val b = (color.blue * 255).toInt().coerceIn(0, 255)
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }

    companion object {
        val SIZE_PRESETS = listOf(
            4f to "Thin",
            12f to "Medium",
            24f to "Thick",
            36f to "Extra"
        )
    }
}

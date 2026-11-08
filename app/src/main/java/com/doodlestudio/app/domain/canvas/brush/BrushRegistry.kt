package com.doodlestudio.app.domain.canvas.brush

/**
 * Global registry of all 21 brush effects available in DoodleStudio.
 * Ordered exactly as seen in the reference application horizontal toolbar.
 */
object BrushRegistry {
    val allBrushes: List<BrushEffect> = listOf(
        ClassicNeonBrush(),
        RainbowGlowBrush(),
        CalligraphyRibbonBrush(),
        DoubleStrokeBrush(),
        ThinNeonPenBrush(),
        CrayonChalkBrush(),
        SolidMarkerBrush(),
        DottedPearlBrush(),
        SoftBlurBrush(),
        ConcentricRingsBrush(),
        SparklerBrush(),
        FireworksBrush(),
        NeonStarsBrush(),
        CrossSparklesBrush(),
        BokehDotsBrush(),
        BigCrossStarBrush(),
        PurpleSparklesBrush(),
        FairyDustBrush(),
        SnowflakesBrush(),
        HeartBubblesBrush(),
        SoapBubblesBrush()
    )

    private val brushMap: Map<String, BrushEffect> = allBrushes.associateBy { it.id.uppercase() }

    val defaultBrush: BrushEffect = allBrushes[0]

    fun get(id: String?): BrushEffect {
        if (id == null) return defaultBrush
        val upper = id.uppercase()
        return brushMap[upper] ?: when (upper) {
            "NEON" -> brushMap["CLASSIC_NEON"]
            "PEN" -> brushMap["THIN_NEON_PEN"]
            "RAINBOW" -> brushMap["RAINBOW_GLOW"]
            "SPARKLE" -> brushMap["CROSS_SPARKLES"]
            "STARS" -> brushMap["NEON_STARS"]
            "MARKER" -> brushMap["SOLID_MARKER"]
            else -> defaultBrush
        } ?: defaultBrush
    }
}

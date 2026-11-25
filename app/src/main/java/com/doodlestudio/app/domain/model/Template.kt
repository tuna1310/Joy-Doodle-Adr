package com.doodlestudio.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Template(
    val id: String,
    val nameEn: String = "",
    val nameVi: String = "",
    val category: String = "",
    val difficulty: String = "easy",
    val imagePath: String = "",
    val regionCountHint: Int = 0,
    val titleEn: String = "",
    val titleVi: String = "",
    val categoryId: String = "",
    val artist: String = "",
    val license: String = "CC0 1.0 Universal",
    val fillableAreaPct: String = "",
    val significantRegions: Int = 0,
    val description: String = ""
) {
    val displayTitleEn: String get() = if (titleEn.isNotBlank()) titleEn else nameEn
    val displayTitleVi: String get() = if (titleVi.isNotBlank()) titleVi else nameVi
}

enum class TemplateCategory(val id: String, val displayNameEn: String, val displayNameVi: String) {
    ALL("all", "All", "All"),
    ANIMALS("animals", "Animals", "Animals"),
    VEHICLES("vehicles", "Vehicles", "Vehicles"),
    SEA("sea", "Sea", "Sea"),
    FLOWERS("flowers", "Flowers", "Flowers"),
    DINOSAURS("dinosaurs", "Dinosaurs", "Dinosaurs"),
    FOOD("food", "Food", "Food"),
    SCENERY("scenery", "Scenery", "Scenery"),
    LETTERS("letters", "Letters", "Letters");

    companion object {
        fun fromId(id: String?): TemplateCategory = entries.firstOrNull {
            it.id.equals(id, ignoreCase = true) || it.displayNameVi.equals(id, ignoreCase = true)
        } ?: ALL
    }
}

enum class TemplateDifficulty(val id: String, val displayNameEn: String, val displayNameVi: String) {
    ALL("all", "All", "All"),
    EASY("easy", "Easy", "Easy"),
    MEDIUM("medium", "Medium", "Medium"),
    HARD("hard", "Detailed", "Detailed");

    companion object {
        fun fromId(id: String?): TemplateDifficulty = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ALL
    }
}

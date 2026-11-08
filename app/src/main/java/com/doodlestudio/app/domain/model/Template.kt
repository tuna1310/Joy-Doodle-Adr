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
    ALL("all", "All", "Tất cả"),
    ANIMALS("animals", "Animals", "Động vật"),
    VEHICLES("vehicles", "Vehicles", "Phương tiện"),
    SEA("sea", "Sea", "Biển"),
    FLOWERS("flowers", "Flowers", "Hoa lá"),
    DINOSAURS("dinosaurs", "Dinosaurs", "Khủng long"),
    FOOD("food", "Food", "Trái cây & Đồ ăn"),
    SCENERY("scenery", "Scenery", "Phong cảnh"),
    LETTERS("letters", "Letters", "Chữ cái & Ký hiệu");

    companion object {
        fun fromId(id: String?): TemplateCategory = entries.firstOrNull {
            it.id.equals(id, ignoreCase = true) || it.displayNameVi.equals(id, ignoreCase = true)
        } ?: ALL
    }
}

enum class TemplateDifficulty(val id: String, val displayNameEn: String, val displayNameVi: String) {
    ALL("all", "All", "Tất cả"),
    EASY("easy", "Easy", "Dễ"),
    MEDIUM("medium", "Medium", "Vừa"),
    HARD("hard", "Detailed", "Chi tiết");

    companion object {
        fun fromId(id: String?): TemplateDifficulty = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ALL
    }
}

package com.doodlestudio.app.domain.repository

import com.doodlestudio.app.domain.model.Template
import com.doodlestudio.app.domain.model.TemplateCategory
import com.doodlestudio.app.domain.model.TemplateDifficulty
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun getTemplates(): Flow<List<Template>>
    suspend fun getTemplateById(id: String): Template?
    suspend fun getTemplatesByCategory(category: TemplateCategory): List<Template>
    suspend fun getTemplatesByDifficulty(difficulty: TemplateDifficulty): List<Template>
}


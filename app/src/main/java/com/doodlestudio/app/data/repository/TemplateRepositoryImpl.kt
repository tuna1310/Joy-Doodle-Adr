package com.doodlestudio.app.data.repository

import android.content.Context
import com.doodlestudio.app.domain.model.Template
import com.doodlestudio.app.domain.model.TemplateCategory
import com.doodlestudio.app.domain.model.TemplateDifficulty
import com.doodlestudio.app.domain.repository.TemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class TemplateRepositoryImpl(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : TemplateRepository {

    private var cachedTemplates: List<Template>? = null

    private suspend fun loadTemplatesInternal(): List<Template> = withContext(Dispatchers.IO) {
        cachedTemplates?.let { return@withContext it }

        val jsonString = try {
            context.assets.open("templates/index.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "[]"
        }

        val parsed = try {
            json.decodeFromString<List<Template>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }

        cachedTemplates = parsed
        parsed
    }

    override fun getTemplates(): Flow<List<Template>> = flow {
        emit(loadTemplatesInternal())
    }.flowOn(Dispatchers.IO)

    override suspend fun getTemplateById(id: String): Template? {
        return loadTemplatesInternal().firstOrNull { it.id == id }
    }

    override suspend fun getTemplatesByCategory(category: TemplateCategory): List<Template> {
        val all = loadTemplatesInternal()
        return if (category == TemplateCategory.ALL) {
            all
        } else {
            all.filter {
                it.categoryId.equals(category.id, ignoreCase = true) ||
                it.category.equals(category.displayNameVi, ignoreCase = true) ||
                it.category.equals(category.displayNameEn, ignoreCase = true) ||
                it.category.equals(category.id, ignoreCase = true)
            }
        }
    }

    override suspend fun getTemplatesByDifficulty(difficulty: TemplateDifficulty): List<Template> {
        return loadTemplatesInternal().filter { it.difficulty.equals(difficulty.id, ignoreCase = true) }
    }
}


package com.doodlestudio.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doodlestudio.app.domain.model.Template
import com.doodlestudio.app.domain.model.TemplateCategory
import com.doodlestudio.app.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val featuredTemplates: List<Template> = emptyList(),
    val categories: List<TemplateCategory> = TemplateCategory.entries,
    val selectedCategory: TemplateCategory = TemplateCategory.ALL,
    val filteredTemplates: List<Template> = emptyList()
)

class HomeViewModel(
    private val templateRepository: TemplateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
    }

    fun loadTemplates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            templateRepository.getTemplates().collect { list ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        featuredTemplates = list.take(4),
                        filteredTemplates = list
                    )
                }
            }
        }
    }

    fun selectCategory(category: TemplateCategory) {
        viewModelScope.launch {
            val filtered = templateRepository.getTemplatesByCategory(category)
            _uiState.update {
                it.copy(
                    selectedCategory = category,
                    filteredTemplates = filtered
                )
            }
        }
    }
}


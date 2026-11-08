package com.doodlestudio.app.ui.screens.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doodlestudio.app.domain.model.Artwork
import com.doodlestudio.app.domain.repository.ArtworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GalleryUiState(
    val isLoading: Boolean = true,
    val artworks: List<Artwork> = emptyList(),
    val artworkCount: Int = 0
)

class GalleryViewModel(
    private val artworkRepository: ArtworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadArtworks()
    }

    fun loadArtworks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            artworkRepository.getAllArtworks().collect { list ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        artworks = list,
                        artworkCount = list.size
                    )
                }
            }
        }
    }

    fun deleteArtwork(artworkId: String) {
        viewModelScope.launch {
            artworkRepository.deleteArtwork(artworkId)
        }
    }
}

package com.doodlestudio.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doodlestudio.app.domain.model.Artwork
import com.doodlestudio.app.domain.repository.ArtworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val isLoading: Boolean = false,
    val artworks: List<Artwork> = emptyList(),
    val isFavoritesOnly: Boolean = false
)

class LibraryViewModel(
    private val artworkRepository: ArtworkRepository
) : ViewModel() {

    private val _isFavoritesOnly = MutableStateFlow(false)

    val uiState: StateFlow<LibraryUiState> = combine(
        artworkRepository.getAllArtworks(),
        artworkRepository.getFavoriteArtworks(),
        _isFavoritesOnly
    ) { all, favorites, favOnly ->
        val displayed = if (favOnly) favorites else all
        LibraryUiState(
            isLoading = false,
            artworks = displayed,
            isFavoritesOnly = favOnly
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState(isLoading = true)
    )

    fun setFavoritesOnly(favoritesOnly: Boolean) {
        _isFavoritesOnly.value = favoritesOnly
    }

    fun toggleFavorite(artworkId: String) {
        viewModelScope.launch {
            artworkRepository.toggleFavorite(artworkId)
        }
    }

    fun deleteArtwork(artworkId: String) {
        viewModelScope.launch {
            artworkRepository.deleteArtwork(artworkId)
        }
    }
}


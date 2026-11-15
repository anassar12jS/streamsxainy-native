package com.example.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.StreamsxainyApp
import com.example.data.model.Episode
import com.example.data.model.MediaType
import com.example.data.model.TitleDetail
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Success(val detail: TitleDetail) : DetailsUiState
    data object Error : DetailsUiState
}

class DetailsViewModel(
    private val tmdbId: Int,
    private val mediaType: MediaType,
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    val inWatchlist: StateFlow<Boolean> = repository.observeIsInWatchlist(tmdbId, mediaType)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = false
        )

    private val _selectedSeason = MutableStateFlow<Int?>(null)
    val selectedSeason: StateFlow<Int?> = _selectedSeason.asStateFlow()

    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes: StateFlow<List<Episode>> = _episodes.asStateFlow()

    private val _episodesLoading = MutableStateFlow(false)
    val episodesLoading: StateFlow<Boolean> = _episodesLoading.asStateFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.value = DetailsUiState.Loading
            runCatching { repository.details(tmdbId, mediaType) }
                .onSuccess { detail ->
                    _uiState.value = DetailsUiState.Success(detail)
                    if (mediaType == MediaType.TV && detail.seasons.isNotEmpty()) {
                        val initial = detail.seasons.firstOrNull { it.seasonNumber > 0 }
                            ?: detail.seasons.first()
                        selectSeason(initial.seasonNumber)
                    }
                }
                .onFailure {
                    _uiState.value = DetailsUiState.Error
                }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        if (_selectedSeason.value == seasonNumber && _episodes.value.isNotEmpty()) return
        _selectedSeason.value = seasonNumber
        viewModelScope.launch {
            _episodesLoading.value = true
            val eps = runCatching { repository.season(tmdbId, seasonNumber) }
                .getOrDefault(emptyList())
            _episodes.value = eps
            _episodesLoading.value = false
        }
    }

    fun toggleWatchlist() {
        val current = (_uiState.value as? DetailsUiState.Success)?.detail ?: return
        viewModelScope.launch {
            repository.toggleWatchlist(current.toTmdbItem(mediaType))
        }
    }

    companion object {
        fun factory(id: Int, mediaType: MediaType): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as StreamsxainyApp
                    return DetailsViewModel(id, mediaType, app.repository) as T
                }
            }
    }
}

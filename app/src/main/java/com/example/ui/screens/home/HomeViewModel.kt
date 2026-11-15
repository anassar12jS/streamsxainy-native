package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.StreamsxainyApp
import com.example.data.model.TmdbItem
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class HomeFeed(
    val trending: List<TmdbItem> = emptyList(),
    val nowPlaying: List<TmdbItem> = emptyList(),
    val popularMovies: List<TmdbItem> = emptyList(),
    val popularTv: List<TmdbItem> = emptyList(),
    val topRatedMovies: List<TmdbItem> = emptyList(),
    val topRatedTv: List<TmdbItem> = emptyList(),
    val upcoming: List<TmdbItem> = emptyList(),
    val airingToday: List<TmdbItem> = emptyList()
) {
    val heroItems: List<TmdbItem>
        get() = trending.filter { !it.backdropPath.isNullOrBlank() }.take(6)

    val isEmpty: Boolean
        get() = trending.isEmpty() && popularMovies.isEmpty() && popularTv.isEmpty()
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val feed: HomeFeed) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val repository: MediaRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val feed = supervisorScope {
                    val trending = async { runCatching { repository.trending() }.getOrDefault(emptyList()) }
                    val nowPlaying = async { runCatching { repository.nowPlaying() }.getOrDefault(emptyList()) }
                    val popularMovies = async { runCatching { repository.popularMovies() }.getOrDefault(emptyList()) }
                    val popularTv = async { runCatching { repository.popularTv() }.getOrDefault(emptyList()) }
                    val topMovies = async { runCatching { repository.topRatedMovies() }.getOrDefault(emptyList()) }
                    val topTv = async { runCatching { repository.topRatedTv() }.getOrDefault(emptyList()) }
                    val upcoming = async { runCatching { repository.upcoming() }.getOrDefault(emptyList()) }
                    val airing = async { runCatching { repository.airingToday() }.getOrDefault(emptyList()) }

                    HomeFeed(
                        trending = trending.await(),
                        nowPlaying = nowPlaying.await(),
                        popularMovies = popularMovies.await(),
                        popularTv = popularTv.await(),
                        topRatedMovies = topMovies.await(),
                        topRatedTv = topTv.await(),
                        upcoming = upcoming.await(),
                        airingToday = airing.await()
                    )
                }
                _uiState.value = if (feed.isEmpty) {
                    HomeUiState.Error("Unable to load catalog. Check your connection.")
                } else {
                    HomeUiState.Success(feed)
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unable to load catalog.")
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as StreamsxainyApp
                return HomeViewModel(app.repository) as T
            }
        }
    }
}

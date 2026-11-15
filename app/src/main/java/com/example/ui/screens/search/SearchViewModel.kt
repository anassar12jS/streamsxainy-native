package com.example.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.StreamsxainyApp
import com.example.data.model.Genre
import com.example.data.model.MediaType
import com.example.data.model.TmdbItem
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

enum class SearchFilter {
    ALL, MOVIES, SERIES
}

data class SearchUiState(
    val query: String = "",
    val results: List<TmdbItem> = emptyList(),
    val loading: Boolean = false,
    val searchedQuery: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val movieGenres: List<Genre> = emptyList(),
    val trendingSuggestions: List<TmdbItem> = emptyList()
) {
    val filteredResults: List<TmdbItem>
        get() = when (filter) {
            SearchFilter.ALL -> results
            SearchFilter.MOVIES -> results.filter { it.resolvedMediaType == MediaType.MOVIE }
            SearchFilter.SERIES -> results.filter { it.resolvedMediaType == MediaType.TV }
        }
}

@OptIn(FlowPreview::class)
class SearchViewModel(private val repository: MediaRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadDiscoveryDefaults()
        viewModelScope.launch {
            _query
                .debounce(350L)
                .distinctUntilChanged()
                .collectLatest { raw ->
                    val trimmed = raw.trim()
                    if (trimmed.length < 2) {
                        _uiState.value = _uiState.value.copy(
                            results = emptyList(),
                            loading = false,
                            searchedQuery = ""
                        )
                        return@collectLatest
                    }
                    _uiState.value = _uiState.value.copy(loading = true)
                    val items = runCatching { repository.search(trimmed) }.getOrDefault(emptyList())
                    _uiState.value = _uiState.value.copy(
                        results = items,
                        loading = false,
                        searchedQuery = trimmed
                    )
                }
        }
    }

    private fun loadDiscoveryDefaults() {
        viewModelScope.launch {
            supervisorScope {
                val genresTask = async { runCatching { repository.genres(MediaType.MOVIE) }.getOrDefault(emptyList()) }
                val trendingTask = async { runCatching { repository.trending() }.getOrDefault(emptyList()) }
                _uiState.value = _uiState.value.copy(
                    movieGenres = genresTask.await(),
                    trendingSuggestions = trendingTask.await().take(12)
                )
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        _uiState.value = _uiState.value.copy(query = newQuery)
    }

    fun setFilter(filter: SearchFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun clearQuery() {
        onQueryChange("")
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as StreamsxainyApp
                return SearchViewModel(app.repository) as T
            }
        }
    }
}

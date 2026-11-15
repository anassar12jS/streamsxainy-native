package com.example.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.StreamsxainyApp
import com.example.data.model.MediaType
import com.example.data.model.ProgressItem
import com.example.data.model.TmdbItem
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class WatchlistFilter { ALL, MOVIES, SERIES }
enum class WatchlistSort { RECENT, RATING, TITLE }

data class ListUiState(
    val watchlist: List<TmdbItem> = emptyList(),
    val progress: List<ProgressItem> = emptyList(),
    val filter: WatchlistFilter = WatchlistFilter.ALL,
    val sort: WatchlistSort = WatchlistSort.RECENT,
    val totalWatchlistCount: Int = 0
)

class ListViewModel(private val repository: MediaRepository) : ViewModel() {
    private val _filter = MutableStateFlow(WatchlistFilter.ALL)
    private val _sort = MutableStateFlow(WatchlistSort.RECENT)

    val uiState: StateFlow<ListUiState> = combine(
        repository.watchlistFlow,
        repository.progressFlow,
        _filter,
        _sort
    ) { watchlist, progress, filter, sort ->
        val filtered = when (filter) {
            WatchlistFilter.ALL -> watchlist
            WatchlistFilter.MOVIES -> watchlist.filter { it.resolvedMediaType == MediaType.MOVIE }
            WatchlistFilter.SERIES -> watchlist.filter { it.resolvedMediaType == MediaType.TV }
        }
        val sorted = when (sort) {
            WatchlistSort.RECENT -> filtered
            WatchlistSort.RATING -> filtered.sortedByDescending { it.voteAverage ?: 0.0 }
            WatchlistSort.TITLE -> filtered.sortedBy { it.displayTitle.lowercase() }
        }
        ListUiState(
            watchlist = sorted,
            progress = progress,
            filter = filter,
            sort = sort,
            totalWatchlistCount = watchlist.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ListUiState()
    )

    fun setFilter(filter: WatchlistFilter) {
        _filter.value = filter
    }

    fun setSort(sort: WatchlistSort) {
        _sort.value = sort
    }

    fun removeProgress(id: Int, mediaType: MediaType) {
        viewModelScope.launch {
            repository.removeProgress(id, mediaType)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as StreamsxainyApp
                return ListViewModel(app.repository) as T
            }
        }
    }
}

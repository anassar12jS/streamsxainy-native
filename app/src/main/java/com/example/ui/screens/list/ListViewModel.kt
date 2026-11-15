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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListUiState(
    val watchlist: List<TmdbItem> = emptyList(),
    val progress: List<ProgressItem> = emptyList()
)

class ListViewModel(private val repository: MediaRepository) : ViewModel() {
    val uiState: StateFlow<ListUiState> = combine(
        repository.watchlistFlow,
        repository.progressFlow
    ) { watchlist, progress ->
        ListUiState(watchlist = watchlist, progress = progress)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ListUiState()
    )

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

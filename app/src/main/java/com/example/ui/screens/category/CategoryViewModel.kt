package com.example.ui.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.StreamsxainyApp
import com.example.data.model.CatalogCategory
import com.example.data.model.MediaType
import com.example.data.model.TmdbItem
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoryUiState(
    val items: List<TmdbItem> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 1,
    val initialLoading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null
) {
    val canLoadMore: Boolean
        get() = page < totalPages && !loadingMore && !initialLoading
}

class CategoryViewModel(
    private val categoryKey: String?,
    private val genreId: Int?,
    private val mediaType: MediaType,
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        loadNextPage(reset = true)
    }

    fun loadNextPage(reset: Boolean = false) {
        val current = _uiState.value
        if (!reset && (current.loadingMore || current.page >= current.totalPages)) return

        val nextPage = if (reset) 1 else current.page + 1
        viewModelScope.launch {
            _uiState.value = if (reset) {
                CategoryUiState(initialLoading = true)
            } else {
                current.copy(loadingMore = true, error = null)
            }

            runCatching {
                val category = categoryKey?.let { CatalogCategory.fromKey(it) }
                when {
                    category != null -> repository.categoryPage(category, nextPage)
                    genreId != null -> repository.discoverByGenre(mediaType, genreId, nextPage)
                    else -> throw IllegalArgumentException("Missing category or genre")
                }
            }.onSuccess { paged ->
                val existing = if (reset) emptyList() else _uiState.value.items
                val merged = (existing + paged.results).distinctBy { "${it.resolvedMediaType.path}_${it.id}" }
                _uiState.value = CategoryUiState(
                    items = merged,
                    page = paged.page,
                    totalPages = paged.totalPages,
                    initialLoading = false,
                    loadingMore = false,
                    error = null
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    initialLoading = false,
                    loadingMore = false,
                    error = err.message ?: "Unable to load titles"
                )
            }
        }
    }

    companion object {
        fun factory(
            categoryKey: String?,
            genreId: Int?,
            mediaType: MediaType
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as StreamsxainyApp
                return CategoryViewModel(categoryKey, genreId, mediaType, app.repository) as T
            }
        }
    }
}

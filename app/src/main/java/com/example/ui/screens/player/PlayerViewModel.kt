package com.example.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.StreamsxainyApp
import com.example.data.model.MediaType
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: MediaRepository
) : ViewModel() {

    private var lastSavedAtMs: Long = 0L

    fun recordPlaybackProgress(
        tmdbId: Int,
        mediaType: MediaType,
        title: String,
        posterPath: String?,
        season: Int?,
        episode: Int?,
        positionSeconds: Int,
        durationSeconds: Int
    ) {
        if (tmdbId <= 0 || durationSeconds <= 0) return
        val now = System.currentTimeMillis()
        if (now - lastSavedAtMs < 15_000L) return
        lastSavedAtMs = now

        viewModelScope.launch {
            repository.saveProgress(
                tmdbId = tmdbId,
                mediaType = mediaType,
                title = title,
                posterPath = posterPath,
                season = if (mediaType == MediaType.TV) season else null,
                episode = if (mediaType == MediaType.TV) episode else null,
                position = positionSeconds,
                duration = durationSeconds
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as StreamsxainyApp
                return PlayerViewModel(app.repository) as T
            }
        }
    }
}

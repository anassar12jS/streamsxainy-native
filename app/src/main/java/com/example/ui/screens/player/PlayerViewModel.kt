package com.example.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.StreamsxainyApp
import com.example.data.local.PlayerPreferences
import com.example.data.model.Episode
import com.example.data.model.MediaType
import com.example.data.model.StreamProvider
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: MediaRepository,
    private val preferences: PlayerPreferences
) : ViewModel() {

    private var lastSavedAtMs: Long = 0L

    private val _providerIdx = MutableStateFlow(resolveInitialProviderIndex())
    val providerIdx: StateFlow<Int> = _providerIdx.asStateFlow()

    private val _seasonEpisodes = MutableStateFlow<List<Episode>>(emptyList())
    val seasonEpisodes: StateFlow<List<Episode>> = _seasonEpisodes.asStateFlow()

    private fun resolveInitialProviderIndex(): Int {
        val preferredId = preferences.getPreferredProviderId() ?: return 0
        val idx = StreamProvider.ALL.indexOfFirst { it.id == preferredId }
        return if (idx >= 0) idx else 0
    }

    fun selectProvider(index: Int) {
        val safeIdx = ((index % StreamProvider.ALL.size) + StreamProvider.ALL.size) % StreamProvider.ALL.size
        _providerIdx.value = safeIdx
        preferences.setPreferredProviderId(StreamProvider.ALL[safeIdx].id)
    }

    fun loadSeasonEpisodes(tmdbId: Int, mediaType: MediaType, season: Int) {
        if (mediaType != MediaType.TV || tmdbId <= 0) return
        viewModelScope.launch {
            val eps = runCatching { repository.season(tmdbId, season) }.getOrDefault(emptyList())
            _seasonEpisodes.value = eps
        }
    }

    fun markPlaybackStarted(
        tmdbId: Int,
        mediaType: MediaType,
        title: String,
        posterPath: String?,
        season: Int,
        episode: Int
    ) {
        if (tmdbId <= 0 || title.isBlank()) return
        viewModelScope.launch {
            repository.ensureProgressStarted(
                tmdbId = tmdbId,
                mediaType = mediaType,
                title = title,
                posterPath = posterPath,
                season = if (mediaType == MediaType.TV) season else null,
                episode = if (mediaType == MediaType.TV) episode else null
            )
        }
    }

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
                return PlayerViewModel(app.repository, app.playerPreferences) as T
            }
        }
    }
}

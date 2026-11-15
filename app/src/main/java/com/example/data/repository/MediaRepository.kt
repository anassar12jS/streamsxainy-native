package com.example.data.repository

import com.example.data.local.ProgressDao
import com.example.data.local.ProgressEntity
import com.example.data.local.WatchlistDao
import com.example.data.local.WatchlistEntity
import com.example.data.model.CatalogCategory
import com.example.data.model.Episode
import com.example.data.model.Genre
import com.example.data.model.MediaType
import com.example.data.model.ProgressItem
import com.example.data.model.TitleDetail
import com.example.data.model.TmdbItem
import com.example.data.model.TmdbPagedResponse
import com.example.data.remote.TmdbApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MediaRepository(
    private val api: TmdbApiService,
    private val watchlistDao: WatchlistDao,
    private val progressDao: ProgressDao
) {
    val watchlistFlow: Flow<List<TmdbItem>> =
        watchlistDao.observeWatchlist().map { list -> list.map { it.toTmdbItem() } }

    val progressFlow: Flow<List<ProgressItem>> =
        progressDao.observeRecentProgress().map { list -> list.map { it.toDomain() } }

    fun observeIsInWatchlist(tmdbId: Int, mediaType: MediaType): Flow<Boolean> =
        watchlistDao.observeIsInWatchlist(tmdbId, mediaType.path)

    fun observeLatestProgressForTitle(tmdbId: Int, mediaType: MediaType): Flow<ProgressItem?> =
        progressDao.observeLatestForTitle(tmdbId, mediaType.path).map { it?.toDomain() }

    suspend fun toggleWatchlist(item: TmdbItem): Boolean {
        val type = item.resolvedMediaType.path
        val exists = watchlistDao.isInWatchlist(item.id, type)
        return if (exists) {
            watchlistDao.delete(item.id, type)
            false
        } else {
            watchlistDao.upsert(WatchlistEntity.fromTmdbItem(item))
            true
        }
    }

    suspend fun saveProgress(
        tmdbId: Int,
        mediaType: MediaType,
        title: String,
        posterPath: String?,
        season: Int?,
        episode: Int?,
        position: Int,
        duration: Int
    ) {
        val typePath = mediaType.path
        progressDao.upsert(
            ProgressEntity(
                key = ProgressEntity.keyFor(tmdbId, typePath, season, episode),
                tmdbId = tmdbId,
                mediaType = typePath,
                title = title,
                posterPath = posterPath,
                season = season,
                episode = episode,
                position = position,
                duration = duration
            )
        )
        progressDao.trimOldProgress()
    }

    suspend fun ensureProgressStarted(
        tmdbId: Int,
        mediaType: MediaType,
        title: String,
        posterPath: String?,
        season: Int?,
        episode: Int?
    ) {
        if (title.isBlank()) return
        val typePath = mediaType.path
        progressDao.insertIfAbsent(
            ProgressEntity(
                key = ProgressEntity.keyFor(tmdbId, typePath, season, episode),
                tmdbId = tmdbId,
                mediaType = typePath,
                title = title,
                posterPath = posterPath,
                season = season,
                episode = episode,
                position = 30,
                duration = 3600
            )
        )
        progressDao.trimOldProgress()
    }

    suspend fun removeProgress(tmdbId: Int, mediaType: MediaType) {
        progressDao.deleteByTitle(tmdbId, mediaType.path)
    }

    suspend fun trending(): List<TmdbItem> = api.trending()
    suspend fun popularMovies(): List<TmdbItem> = api.popularMovies()
    suspend fun popularTv(): List<TmdbItem> = api.popularTv()
    suspend fun topRatedMovies(): List<TmdbItem> = api.topRatedMovies()
    suspend fun topRatedTv(): List<TmdbItem> = api.topRatedTv()
    suspend fun nowPlaying(): List<TmdbItem> = api.nowPlaying()
    suspend fun upcoming(): List<TmdbItem> = api.upcoming()
    suspend fun airingToday(): List<TmdbItem> = api.airingToday()
    suspend fun categoryPage(category: CatalogCategory, page: Int): TmdbPagedResponse =
        api.categoryPage(category, page)
    suspend fun genres(type: MediaType): List<Genre> = api.genres(type)
    suspend fun discoverByGenre(type: MediaType, genreId: Int, page: Int): TmdbPagedResponse =
        api.discoverByGenre(type, genreId, page)
    suspend fun search(query: String): List<TmdbItem> = api.search(query)
    suspend fun details(id: Int, type: MediaType): TitleDetail = api.details(id, type)
    suspend fun season(id: Int, seasonNumber: Int): List<Episode> = api.season(id, seasonNumber)
}

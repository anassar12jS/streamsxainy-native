package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.MediaType
import com.example.data.model.ProgressItem
import com.example.data.model.TmdbItem

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val key: String,
    val tmdbId: Int,
    val mediaType: String,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val voteAverage: Double?,
    val overview: String?,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toTmdbItem(): TmdbItem = TmdbItem(
        id = tmdbId,
        mediaType = mediaType,
        title = if (mediaType == MediaType.MOVIE.path) title else null,
        name = if (mediaType == MediaType.TV.path) title else null,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = if (mediaType == MediaType.MOVIE.path) releaseDate else null,
        firstAirDate = if (mediaType == MediaType.TV.path) releaseDate else null,
        voteAverage = voteAverage,
        overview = overview
    )

    companion object {
        fun keyFor(tmdbId: Int, mediaType: String): String = "${mediaType}_$tmdbId"

        fun fromTmdbItem(item: TmdbItem): WatchlistEntity {
            val type = item.resolvedMediaType.path
            return WatchlistEntity(
                key = keyFor(item.id, type),
                tmdbId = item.id,
                mediaType = type,
                title = item.displayTitle,
                posterPath = item.posterPath,
                backdropPath = item.backdropPath,
                releaseDate = item.releaseDate ?: item.firstAirDate,
                voteAverage = item.voteAverage,
                overview = item.overview
            )
        }
    }
}

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val key: String,
    val tmdbId: Int,
    val mediaType: String,
    val title: String,
    val posterPath: String?,
    val season: Int?,
    val episode: Int?,
    val position: Int,
    val duration: Int,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ProgressItem = ProgressItem(
        id = tmdbId,
        mediaType = MediaType.from(mediaType),
        title = title,
        posterPath = posterPath,
        season = season,
        episode = episode,
        position = position,
        duration = duration,
        updatedAt = updatedAt
    )

    companion object {
        fun keyFor(tmdbId: Int, mediaType: String, season: Int?, episode: Int?): String =
            "${mediaType}_${tmdbId}_${season ?: 0}_${episode ?: 0}"
    }
}

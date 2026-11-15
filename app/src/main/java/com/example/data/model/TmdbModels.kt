package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URI
import java.net.URLEncoder

enum class MediaType(val path: String, val label: String) {
    MOVIE("movie", "Movie"),
    TV("tv", "Series");

    companion object {
        fun from(value: String?): MediaType =
            if (value.equals("tv", ignoreCase = true)) TV else MOVIE
    }
}

@Serializable
data class TmdbPagedResponse(
    val page: Int = 1,
    val results: List<TmdbItem> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0
)

@Serializable
data class TmdbItem(
    val id: Int,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    val overview: String? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList()
) {
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: name.orEmpty()

    val year: String
        get() = (releaseDate?.takeIf { it.isNotBlank() } ?: firstAirDate.orEmpty()).take(4)

    val resolvedMediaType: MediaType
        get() = when {
            mediaType == "tv" -> MediaType.TV
            mediaType == "movie" -> MediaType.MOVIE
            title.isNullOrBlank() && !name.isNullOrBlank() -> MediaType.TV
            else -> MediaType.MOVIE
        }
}

@Serializable
data class Genre(
    val id: Int,
    val name: String
)

@Serializable
data class GenreListResponse(
    val genres: List<Genre> = emptyList()
)

@Serializable
data class CastMember(
    val id: Int,
    val name: String = "",
    val character: String = "",
    @SerialName("profile_path") val profilePath: String? = null
)

@Serializable
data class Credits(
    val cast: List<CastMember> = emptyList()
)

@Serializable
data class VideoItem(
    val id: String = "",
    val key: String = "",
    val name: String = "",
    val site: String = "",
    val type: String = "",
    val official: Boolean = false
)

@Serializable
data class VideosResponse(
    val results: List<VideoItem> = emptyList()
)

@Serializable
data class Episode(
    @SerialName("episode_number") val episodeNumber: Int,
    val name: String = "",
    val overview: String = "",
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    val runtime: Int? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0
)

@Serializable
data class Season(
    @SerialName("season_number") val seasonNumber: Int,
    val name: String = "",
    @SerialName("episode_count") val episodeCount: Int = 0,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("air_date") val airDate: String? = null
)

@Serializable
data class TitleDetail(
    val id: Int,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    val overview: String? = null,
    val genres: List<Genre> = emptyList(),
    val tagline: String? = null,
    val status: String? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    val seasons: List<Season> = emptyList(),
    val runtime: Int? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    val credits: Credits? = null,
    val videos: VideosResponse? = null,
    val recommendations: TmdbPagedResponse? = null
) {
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: name.orEmpty()

    val year: String
        get() = (releaseDate?.takeIf { it.isNotBlank() } ?: firstAirDate.orEmpty()).take(4)

    val displayRuntime: Int?
        get() = runtime ?: episodeRunTime.firstOrNull()

    val primaryTrailer: VideoItem?
        get() = videos?.results
            ?.filter { it.site.equals("YouTube", ignoreCase = true) && it.key.isNotBlank() }
            ?.sortedByDescending {
                (if (it.type.equals("Trailer", ignoreCase = true)) 10 else 0) +
                    (if (it.official) 5 else 0)
            }
            ?.firstOrNull()

    fun toTmdbItem(type: MediaType): TmdbItem = TmdbItem(
        id = id,
        mediaType = type.path,
        title = title,
        name = name,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        firstAirDate = firstAirDate,
        voteAverage = voteAverage,
        overview = overview
    )
}

@Serializable
data class SeasonDetailResponse(
    val episodes: List<Episode> = emptyList()
)

data class ProgressItem(
    val id: Int,
    val mediaType: MediaType,
    val title: String,
    val posterPath: String?,
    val season: Int? = null,
    val episode: Int? = null,
    val position: Int,
    val duration: Int,
    val updatedAt: Long
) {
    val progressFraction: Float
        get() = (position.toFloat() / duration.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    fun toTmdbItem(): TmdbItem = TmdbItem(
        id = id,
        mediaType = mediaType.path,
        title = if (mediaType == MediaType.MOVIE) title else null,
        name = if (mediaType == MediaType.TV) title else null,
        posterPath = posterPath
    )
}

enum class CatalogCategory(val key: String, val title: String) {
    TRENDING("trending", "Trending This Week"),
    NOW_PLAYING("now_playing", "Now Playing in Cinemas"),
    POPULAR_MOVIES("popular_movies", "Popular Movies"),
    POPULAR_TV("popular_tv", "Popular Series"),
    TOP_MOVIES("top_movies", "Top Rated Movies"),
    TOP_TV("top_tv", "Top Rated Series"),
    UPCOMING("upcoming", "Coming Soon"),
    AIRING_TODAY("airing_today", "Airing Today");

    companion object {
        fun fromKey(key: String): CatalogCategory? = entries.firstOrNull { it.key == key }
    }
}

object TmdbImages {
    fun poster(path: String?, size: String = "w342"): String? =
        path?.takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/$size$it" }

    fun backdrop(path: String?, size: String = "w1280"): String? =
        path?.takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/$size$it" }

    fun profile(path: String?, size: String = "w185"): String? =
        path?.takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/$size$it" }

    fun still(path: String?, size: String = "w300"): String? =
        path?.takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/$size$it" }
}

data class StreamParams(
    val type: MediaType,
    val id: Int,
    val season: Int = 1,
    val episode: Int = 1,
    val position: Int = 0
)

data class StreamProvider(
    val id: String,
    val name: String,
    val buildUrl: (StreamParams) -> String
) {
    companion object {
        fun hostOf(url: String): String = try {
            URI(url).host?.lowercase().orEmpty()
        } catch (_: Exception) {
            ""
        }

        fun isUrlAllowed(embedHost: String, targetUrl: String): Boolean {
            if (embedHost.isBlank()) return true
            val targetHost = hostOf(targetUrl)
            if (targetHost.isBlank()) return true
            return targetHost == embedHost || targetHost.endsWith(".$embedHost")
        }

        private fun encode(v: String): String = URLEncoder.encode(v, "UTF-8")

        val ALL: List<StreamProvider> = listOf(
            StreamProvider("cinemaos", "CinemaOS") { p ->
                val epPath = if (p.type == MediaType.TV) "/${p.season}/${p.episode}" else ""
                val progress = if (p.position > 0) "&progress=${p.position}" else ""
                "https://cinemaos.tech/player/${p.id}$epPath?theme=7c3aed$progress"
            },
            StreamProvider("vidapi", "VidAPI") { p ->
                val epPath = if (p.type == MediaType.TV) "/${p.season}/${p.episode}" else ""
                "https://vidapi.xyz/embed/${p.type.path}/${p.id}$epPath"
            },
            StreamProvider("ishowstream", "iShowStream") { p ->
                val epPath = if (p.type == MediaType.TV) "/${p.season}/${p.episode}" else ""
                val progress = if (p.position > 0) "&progress=${p.position}" else ""
                "https://ishowstream.cc/embed/${p.type.path}/${p.id}$epPath?branding=${encode("streamsxainy")}&loading=3&subtitle=English&color=7c3aed$progress"
            },
            StreamProvider("vidy", "Vidy Prime") { p ->
                val epPath = if (p.type == MediaType.TV) "/${p.season}/${p.episode}" else ""
                val tvParams = if (p.type == MediaType.TV) "&nextEpisode=true&episodeSelector=true&autoplayNextEpisode=true" else ""
                val progress = if (p.position > 0) "&progress=${p.position}" else ""
                "https://vidy.st/${p.type.path}/${p.id}$epPath?color=7c3aed&autoplay=true$tvParams$progress"
            },
            StreamProvider("vidlink", "VidLink Pro") { p ->
                val epPath = if (p.type == MediaType.TV) "/${p.season}/${p.episode}" else ""
                val tvParams = if (p.type == MediaType.TV) "&nextbutton=true" else ""
                "https://vidlink.pro/${p.type.path}/${p.id}$epPath?primaryColor=7c3aed&secondaryColor=0a0a0b&autoplay=true$tvParams"
            },
            StreamProvider("vidsrc-sh", "VidSrc") { p ->
                val epPath = if (p.type == MediaType.TV) "/${p.season}/${p.episode}" else ""
                val startAt = if (p.position > 0) "&startAt=${p.position}" else ""
                val autoNext = if (p.type == MediaType.TV) "&autonext=1" else ""
                "https://vidsrc.sh/embed/${p.type.path}/${p.id}$epPath?autoplay=1$startAt$autoNext"
            },
            StreamProvider("videasy", "Videasy") { p ->
                val epPath = if (p.type == MediaType.TV) "/${p.season}/${p.episode}" else ""
                "https://videasy.xyz/embed/${p.type.path}/${p.id}$epPath"
            },
            StreamProvider("vidfast", "VidFast") { p ->
                val epPath = if (p.type == MediaType.TV) "/${p.season}/${p.episode}" else ""
                "https://vidfast.pro/${p.type.path}/${p.id}$epPath?autoPlay=true"
            }
        )
    }
}

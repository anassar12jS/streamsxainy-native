package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.CatalogCategory
import com.example.data.model.Episode
import com.example.data.model.Genre
import com.example.data.model.GenreListResponse
import com.example.data.model.MediaType
import com.example.data.model.SeasonDetailResponse
import com.example.data.model.TitleDetail
import com.example.data.model.TmdbItem
import com.example.data.model.TmdbPagedResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class TmdbApiService {
    private val apiKey: String = BuildConfig.TMDB_API_KEY
    private val baseUrl: String = "https://api.themoviedb.org/3"

    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                }
            )
        }
    }

    private suspend inline fun <reified T> get(
        path: String,
        params: Map<String, String> = emptyMap()
    ): T {
        if (apiKey.isBlank() || apiKey == "TMDB_API_KEY_DEFAULT_VALUE") {
            throw IllegalStateException("TMDB_API_KEY is not configured")
        }
        return client.get("$baseUrl$path") {
            parameter("api_key", apiKey)
            params.forEach { (key, value) -> parameter(key, value) }
        }.body()
    }

    private fun List<TmdbItem>.withMediaType(forced: MediaType? = null): List<TmdbItem> =
        map { item ->
            val resolved = forced?.path ?: if (item.mediaType == "tv") "tv" else "movie"
            item.copy(mediaType = resolved)
        }

    suspend fun categoryPage(category: CatalogCategory, page: Int = 1): TmdbPagedResponse {
        val (path, forcedType) = when (category) {
            CatalogCategory.TRENDING -> "/trending/all/week" to null
            CatalogCategory.NOW_PLAYING -> "/movie/now_playing" to MediaType.MOVIE
            CatalogCategory.POPULAR_MOVIES -> "/movie/popular" to MediaType.MOVIE
            CatalogCategory.POPULAR_TV -> "/tv/popular" to MediaType.TV
            CatalogCategory.TOP_MOVIES -> "/movie/top_rated" to MediaType.MOVIE
            CatalogCategory.TOP_TV -> "/tv/top_rated" to MediaType.TV
            CatalogCategory.UPCOMING -> "/movie/upcoming" to MediaType.MOVIE
            CatalogCategory.AIRING_TODAY -> "/tv/airing_today" to MediaType.TV
        }
        val response = get<TmdbPagedResponse>(path, mapOf("page" to page.toString()))
        return response.copy(
            results = response.results
                .filter { forcedType != null || it.mediaType == "movie" || it.mediaType == "tv" }
                .withMediaType(forcedType)
        )
    }

    suspend fun trending(page: Int = 1): List<TmdbItem> =
        categoryPage(CatalogCategory.TRENDING, page).results

    suspend fun popularMovies(page: Int = 1): List<TmdbItem> =
        categoryPage(CatalogCategory.POPULAR_MOVIES, page).results

    suspend fun popularTv(page: Int = 1): List<TmdbItem> =
        categoryPage(CatalogCategory.POPULAR_TV, page).results

    suspend fun topRatedMovies(page: Int = 1): List<TmdbItem> =
        categoryPage(CatalogCategory.TOP_MOVIES, page).results

    suspend fun topRatedTv(page: Int = 1): List<TmdbItem> =
        categoryPage(CatalogCategory.TOP_TV, page).results

    suspend fun nowPlaying(page: Int = 1): List<TmdbItem> =
        categoryPage(CatalogCategory.NOW_PLAYING, page).results

    suspend fun upcoming(page: Int = 1): List<TmdbItem> =
        categoryPage(CatalogCategory.UPCOMING, page).results

    suspend fun airingToday(page: Int = 1): List<TmdbItem> =
        categoryPage(CatalogCategory.AIRING_TODAY, page).results

    suspend fun genres(type: MediaType): List<Genre> =
        get<GenreListResponse>("/genre/${type.path}/list").genres

    suspend fun discoverByGenre(type: MediaType, genreId: Int, page: Int = 1): TmdbPagedResponse {
        val response = get<TmdbPagedResponse>(
            "/discover/${type.path}",
            mapOf(
                "with_genres" to genreId.toString(),
                "sort_by" to "popularity.desc",
                "page" to page.toString()
            )
        )
        return response.copy(results = response.results.withMediaType(type))
    }

    suspend fun search(query: String): List<TmdbItem> =
        get<TmdbPagedResponse>(
            "/search/multi",
            mapOf("query" to query, "include_adult" to "false")
        ).results
            .filter { it.mediaType == "movie" || it.mediaType == "tv" }
            .withMediaType()

    suspend fun details(id: Int, type: MediaType): TitleDetail {
        val raw = get<TitleDetail>(
            "/${type.path}/$id",
            mapOf("append_to_response" to "credits,videos,recommendations")
        )
        val recs = raw.recommendations?.let { paged ->
            paged.copy(
                results = paged.results.withMediaType(type)
            )
        }
        return raw.copy(recommendations = recs)
    }

    suspend fun season(id: Int, seasonNumber: Int): List<Episode> =
        get<SeasonDetailResponse>("/tv/$id/season/$seasonNumber").episodes
}

package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.Episode
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

    suspend fun trending(): List<TmdbItem> =
        get<TmdbPagedResponse>("/trending/all/week").results.withMediaType()

    suspend fun popularMovies(): List<TmdbItem> =
        get<TmdbPagedResponse>("/movie/popular").results.withMediaType(MediaType.MOVIE)

    suspend fun popularTv(): List<TmdbItem> =
        get<TmdbPagedResponse>("/tv/popular").results.withMediaType(MediaType.TV)

    suspend fun topRatedMovies(): List<TmdbItem> =
        get<TmdbPagedResponse>("/movie/top_rated").results.withMediaType(MediaType.MOVIE)

    suspend fun topRatedTv(): List<TmdbItem> =
        get<TmdbPagedResponse>("/tv/top_rated").results.withMediaType(MediaType.TV)

    suspend fun nowPlaying(): List<TmdbItem> =
        get<TmdbPagedResponse>("/movie/now_playing").results.withMediaType(MediaType.MOVIE)

    suspend fun upcoming(): List<TmdbItem> =
        get<TmdbPagedResponse>("/movie/upcoming").results.withMediaType(MediaType.MOVIE)

    suspend fun airingToday(): List<TmdbItem> =
        get<TmdbPagedResponse>("/tv/airing_today").results.withMediaType(MediaType.TV)

    suspend fun search(query: String): List<TmdbItem> =
        get<TmdbPagedResponse>(
            "/search/multi",
            mapOf("query" to query, "include_adult" to "false")
        ).results
            .filter { it.mediaType == "movie" || it.mediaType == "tv" }
            .withMediaType()

    suspend fun details(id: Int, type: MediaType): TitleDetail =
        get("/${type.path}/$id")

    suspend fun season(id: Int, seasonNumber: Int): List<Episode> =
        get<SeasonDetailResponse>("/tv/$id/season/$seasonNumber").episodes
}

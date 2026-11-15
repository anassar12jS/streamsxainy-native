package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.PlayerPreferences
import com.example.data.remote.TmdbApiService
import com.example.data.repository.MediaRepository

class StreamsxainyApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val apiService: TmdbApiService by lazy { TmdbApiService() }
    val playerPreferences: PlayerPreferences by lazy { PlayerPreferences(this) }
    val repository: MediaRepository by lazy {
        MediaRepository(
            api = apiService,
            watchlistDao = database.watchlistDao(),
            progressDao = database.progressDao()
        )
    }
}

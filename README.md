# Streamsxainy (Android)

A native Android movie and TV series discovery, watchlist, and streaming companion built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Room**, **Ktor**, and **Navigation Compose**.

## Features

- **Home Catalog**: Auto-rotating Trending Hero carousel paired with 8 horizontal TMDB catalog rows (Trending, Now Playing, Popular Movies/Series, Top Rated Movies/Series, Coming Soon, Airing Today).
- **Search**: Debounced TMDB multi-search across movies and series with an adaptive poster grid.
- **My List**: Room-backed **Continue Watching** resume cards (with progress bars and episode badges) and saved **Watchlist** grid.
- **Details & Episodes**: Backdrop hero, poster, runtime/rating/genres, watchlist toggle, and TV season selector with episode playback.
- **Cinema Player**: Landscape-locked immersive `WebView` player with domain-whitelist redirect protection, DOM cleanup, playback progress bridge, and 8 switchable streaming servers.

## Setup

1. Configure `TMDB_API_KEY` in AI Studio's **Secrets** panel (or `.env`, see `.env.example`).
2. Build with Gradle:
   ```bash
   gradle :app:assembleDebug
   ```

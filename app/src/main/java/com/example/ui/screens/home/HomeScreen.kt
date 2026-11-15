package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.TmdbImages
import com.example.data.model.TmdbItem
import com.example.ui.components.MediaRow
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenDetails: (TmdbItem) -> Unit,
    onPlayNow: (TmdbItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HomeHeader()

        when (val current = state) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = current.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.loadHome() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("home_retry_button")
                        ) {
                            Text(text = stringResource(R.string.player_retry))
                        }
                    }
                }
            }

            is HomeUiState.Success -> {
                val feed = current.feed
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    val heroes = feed.heroItems
                    if (heroes.isNotEmpty()) {
                        item(key = "hero_carousel") {
                            HeroCarousel(
                                items = heroes,
                                onOpenDetails = onOpenDetails,
                                onPlayNow = onPlayNow
                            )
                        }
                    }

                    item(key = "row_trending") {
                        MediaRow(
                            title = "Trending This Week",
                            items = feed.trending,
                            onItemClick = onOpenDetails
                        )
                    }
                    item(key = "row_now_playing") {
                        MediaRow(
                            title = "Now Playing in Cinemas",
                            items = feed.nowPlaying,
                            onItemClick = onOpenDetails
                        )
                    }
                    item(key = "row_popular_movies") {
                        MediaRow(
                            title = "Popular Movies",
                            items = feed.popularMovies,
                            onItemClick = onOpenDetails
                        )
                    }
                    item(key = "row_popular_tv") {
                        MediaRow(
                            title = "Popular Series",
                            items = feed.popularTv,
                            onItemClick = onOpenDetails
                        )
                    }
                    item(key = "row_top_movies") {
                        MediaRow(
                            title = "Top Rated Movies",
                            items = feed.topRatedMovies,
                            onItemClick = onOpenDetails
                        )
                    }
                    item(key = "row_top_tv") {
                        MediaRow(
                            title = "Top Rated Series",
                            items = feed.topRatedTv,
                            onItemClick = onOpenDetails
                        )
                    }
                    item(key = "row_upcoming") {
                        MediaRow(
                            title = "Coming Soon",
                            items = feed.upcoming,
                            onItemClick = onOpenDetails
                        )
                    }
                    item(key = "row_airing") {
                        MediaRow(
                            title = "Airing Today",
                            items = feed.airingToday,
                            onItemClick = onOpenDetails
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Movie,
                contentDescription = stringResource(R.string.cd_app_logo),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = "STREAMSXAINY",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun HeroCarousel(
    items: List<TmdbItem>,
    onOpenDetails: (TmdbItem) -> Unit,
    onPlayNow: (TmdbItem) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(items.size) {
        if (items.size < 2) return@LaunchedEffect
        while (true) {
            delay(7000L)
            val next = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(next)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .height(440.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .testTag("hero_pager")
        ) { page ->
            val hero = items[page]
            HeroSlide(
                item = hero,
                index = page,
                total = items.size,
                activeIndex = pagerState.currentPage,
                onOpenDetails = { onOpenDetails(hero) },
                onPlayNow = { onPlayNow(hero) }
            )
        }
    }
}

@Composable
private fun HeroSlide(
    item: TmdbItem,
    index: Int,
    total: Int,
    activeIndex: Int,
    onOpenDetails: () -> Unit,
    onPlayNow: () -> Unit
) {
    val backdropUrl = TmdbImages.backdrop(item.backdropPath, "w1280")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        if (backdropUrl != null) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = item.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.15f),
                            0.45f to Color.Transparent,
                            1.0f to MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "TRENDING #${index + 1}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            val sub = buildString {
                append(if (item.resolvedMediaType.path == "tv") "SERIES" else "MOVIE")
                val vote = item.voteAverage
                if (vote != null && vote > 0.0) {
                    append(String.format(Locale.US, " · ★ %.1f", vote))
                }
                if (item.year.isNotEmpty()) {
                    append(" · ${item.year}")
                }
            }
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.onBackground)
                        .clickable(onClick = onOpenDetails)
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .testTag("hero_details_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.hero_details),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.background
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onPlayNow)
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .testTag("hero_watch_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.hero_watch_now),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(total) { dotIdx ->
                    val isActive = dotIdx == activeIndex
                    Box(
                        modifier = Modifier
                            .width(if (isActive) 20.dp else 6.dp)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                            )
                    )
                }
            }
        }
    }
}

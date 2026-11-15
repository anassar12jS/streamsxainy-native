package com.example.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Episode
import com.example.data.model.MediaType
import com.example.data.model.Season
import com.example.data.model.TitleDetail
import com.example.data.model.TmdbImages
import java.util.Locale

@Composable
fun DetailsScreen(
    id: Int,
    mediaType: MediaType,
    onBack: () -> Unit,
    onPlay: (season: Int, episode: Int, title: String, posterPath: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailsViewModel = viewModel(
        key = "details_${mediaType.path}_$id",
        factory = DetailsViewModel.factory(id, mediaType)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val inWatchlist by viewModel.inWatchlist.collectAsStateWithLifecycle()
    val selectedSeason by viewModel.selectedSeason.collectAsStateWithLifecycle()
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()
    val episodesLoading by viewModel.episodesLoading.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val current = state) {
            is DetailsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                FloatingBackButton(onBack = onBack)
            }

            is DetailsUiState.Error -> {
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
                        Text(
                            text = stringResource(R.string.details_load_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = viewModel::loadDetails,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(text = stringResource(R.string.player_retry))
                        }
                    }
                }
                FloatingBackButton(onBack = onBack)
            }

            is DetailsUiState.Success -> {
                val detail = current.detail
                val validSeasons = detail.seasons.filter { it.seasonNumber > 0 }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 820.dp)
                        .align(Alignment.TopCenter),
                    contentPadding = PaddingValues(bottom = 48.dp)
                ) {
                    item(key = "hero_backdrop") {
                        DetailsBackdrop(
                            detail = detail,
                            onBack = onBack
                        )
                    }

                    item(key = "poster_and_info") {
                        DetailsHeaderInfo(
                            detail = detail,
                            mediaType = mediaType,
                            inWatchlist = inWatchlist,
                            onPlay = {
                                val initialSeason = selectedSeason ?: 1
                                onPlay(initialSeason, 1, detail.displayTitle, detail.posterPath)
                            },
                            onToggleWatchlist = viewModel::toggleWatchlist
                        )
                    }

                    val overview = detail.overview
                    if (!overview.isNullOrBlank()) {
                        item(key = "overview") {
                            Text(
                                text = overview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                            )
                        }
                    }

                    if (mediaType == MediaType.TV && validSeasons.isNotEmpty()) {
                        item(key = "episodes_header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.details_episodes),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                SeasonSelectorRow(
                                    seasons = validSeasons,
                                    selectedSeason = selectedSeason,
                                    onSelectSeason = viewModel::selectSeason
                                )
                            }
                        }

                        if (episodesLoading) {
                            item(key = "episodes_loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else {
                            items(
                                items = episodes,
                                key = { "ep_${selectedSeason ?: 1}_${it.episodeNumber}" }
                            ) { ep ->
                                EpisodeRow(
                                    episode = ep,
                                    onClick = {
                                        onPlay(
                                            selectedSeason ?: 1,
                                            ep.episodeNumber,
                                            detail.displayTitle,
                                            detail.posterPath
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingBackButton(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(12.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.scrim)
                .testTag("details_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun DetailsBackdrop(
    detail: TitleDetail,
    onBack: () -> Unit
) {
    val backdropUrl = TmdbImages.backdrop(detail.backdropPath, "w1280")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        if (backdropUrl != null) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = detail.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        FloatingBackButton(onBack = onBack)
    }
}

@Composable
private fun DetailsHeaderInfo(
    detail: TitleDetail,
    mediaType: MediaType,
    inWatchlist: Boolean,
    onPlay: () -> Unit,
    onToggleWatchlist: () -> Unit
) {
    val posterUrl = TmdbImages.poster(detail.posterPath, "w342")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-64).dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .width(110.dp)
                .height(165.dp)
                .shadow(12.dp, MaterialTheme.shapes.medium)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = detail.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 68.dp)
        ) {
            Text(
                text = detail.displayTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            val meta = buildString {
                if (detail.year.isNotEmpty()) append(detail.year)
                detail.displayRuntime?.takeIf { it > 0 }?.let { rt ->
                    if (isNotEmpty()) append(" · ")
                    append("${rt}m")
                }
                detail.voteAverage?.takeIf { it > 0.0 }?.let { vote ->
                    if (isNotEmpty()) append(" · ")
                    append(String.format(Locale.US, "★ %.1f", vote))
                }
            }
            if (meta.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (detail.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = detail.genres.take(3).joinToString(" · ") { it.name },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.onBackground)
                        .clickable(onClick = onPlay)
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .testTag("details_play_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (mediaType == MediaType.TV) {
                            stringResource(R.string.details_play_s1e1)
                        } else {
                            stringResource(R.string.details_play)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.background
                    )
                }

                IconButton(
                    onClick = onToggleWatchlist,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (inWatchlist) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        .testTag("details_watchlist_button")
                ) {
                    Icon(
                        imageVector = if (inWatchlist) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = stringResource(R.string.cd_toggle_watchlist),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonSelectorRow(
    seasons: List<Season>,
    selectedSeason: Int?,
    onSelectSeason: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(seasons, key = { it.seasonNumber }) { season ->
            val selected = season.seasonNumber == selectedSeason
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelectSeason(season.seasonNumber) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .testTag("season_chip_${season.seasonNumber}")
            ) {
                Text(
                    text = season.name.ifBlank { "Season ${season.seasonNumber}" },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: Episode,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag("episode_row_${episode.episodeNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = episode.episodeNumber.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.name.ifBlank { "Episode ${episode.episodeNumber}" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val epMeta = buildString {
                    episode.airDate?.take(10)?.takeIf { it.isNotBlank() }?.let { append(it) }
                    episode.runtime?.takeIf { it > 0 }?.let {
                        if (isNotEmpty()) append(" · ")
                        append("${it}m")
                    }
                    if (episode.voteAverage > 0.0) {
                        if (isNotEmpty()) append(" · ")
                        append(String.format(Locale.US, "★ %.1f", episode.voteAverage))
                    }
                }
                if (epMeta.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = epMeta,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = stringResource(R.string.cd_play_episode),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

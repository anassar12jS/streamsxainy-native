package com.example.ui.screens.details

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.Episode
import com.example.data.model.MediaType
import com.example.data.model.ProgressItem
import com.example.data.model.Season
import com.example.data.model.TitleDetail
import com.example.data.model.TmdbImages
import com.example.data.model.TmdbItem
import com.example.data.model.VideoItem
import com.example.ui.components.CastCard
import com.example.ui.components.MediaRow
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DetailsScreen(
    id: Int,
    mediaType: MediaType,
    onBack: () -> Unit,
    onPlay: (season: Int, episode: Int, position: Int, title: String, posterPath: String?) -> Unit,
    onOpenDetails: (TmdbItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailsViewModel = viewModel(
        key = "details_${mediaType.path}_$id",
        factory = DetailsViewModel.factory(id, mediaType)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val inWatchlist by viewModel.inWatchlist.collectAsStateWithLifecycle()
    val savedProgress by viewModel.savedProgress.collectAsStateWithLifecycle()
    val selectedSeason by viewModel.selectedSeason.collectAsStateWithLifecycle()
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()
    val episodesLoading by viewModel.episodesLoading.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var activeTrailer by remember { mutableStateOf<VideoItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                    val cast = detail.credits?.cast?.take(15).orEmpty()
                    val recommendations = detail.recommendations?.results.orEmpty()
                    val trailer = detail.primaryTrailer

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
                                savedProgress = savedProgress,
                                trailer = trailer,
                                onPlay = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val prog = savedProgress
                                    val season = prog?.season ?: selectedSeason ?: 1
                                    val ep = prog?.episode ?: 1
                                    val pos = prog?.position ?: 0
                                    onPlay(season, ep, pos, detail.displayTitle, detail.posterPath)
                                },
                                onToggleWatchlist = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scope.launch {
                                        val added = viewModel.toggleWatchlistAndReturnState()
                                        if (added != null) {
                                            snackbarHostState.showSnackbar(
                                                message = context.getString(
                                                    if (added) R.string.watchlist_added else R.string.watchlist_removed
                                                )
                                            )
                                        }
                                    }
                                },
                                onOpenTrailer = { activeTrailer = trailer }
                            )
                        }

                        val tagline = detail.tagline
                        if (!tagline.isNullOrBlank()) {
                            item(key = "tagline") {
                                Text(
                                    text = "“$tagline”",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                                )
                            }
                        }

                        val overview = detail.overview
                        if (!overview.isNullOrBlank()) {
                            item(key = "overview") {
                                Text(
                                    text = overview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                                )
                            }
                        }

                        if (cast.isNotEmpty()) {
                            item(key = "cast_section") {
                                Column(modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)) {
                                    Text(
                                        text = stringResource(R.string.details_cast),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(cast, key = { it.id }) { member ->
                                            CastCard(member = member)
                                        }
                                    }
                                }
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
                                                0,
                                                detail.displayTitle,
                                                detail.posterPath
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        if (recommendations.isNotEmpty()) {
                            item(key = "recommendations_row") {
                                Spacer(modifier = Modifier.height(16.dp))
                                MediaRow(
                                    title = stringResource(R.string.details_more_like_this),
                                    items = recommendations,
                                    onItemClick = onOpenDetails
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    activeTrailer?.let { video ->
        TrailerDialog(
            video = video,
            onDismiss = { activeTrailer = null }
        )
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
    val context = LocalContext.current
    val backdropUrl = TmdbImages.backdrop(detail.backdropPath, "w1280")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        if (backdropUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backdropUrl)
                    .crossfade(true)
                    .build(),
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
    savedProgress: ProgressItem?,
    trailer: VideoItem?,
    onPlay: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onOpenTrailer: () -> Unit
) {
    val context = LocalContext.current
    val posterUrl = TmdbImages.poster(detail.posterPath, "w342")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-58).dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        model = ImageRequest.Builder(context)
                            .data(posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = detail.displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 64.dp)
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
                    detail.status?.takeIf { it.isNotBlank() }?.let { status ->
                        if (isNotEmpty()) append(" · ")
                        append(status)
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
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val playLabel = when {
                savedProgress != null && mediaType == MediaType.TV && savedProgress.season != null && savedProgress.episode != null ->
                    "${stringResource(R.string.details_resume)} S${savedProgress.season}E${savedProgress.episode}"
                savedProgress != null -> stringResource(R.string.details_resume)
                mediaType == MediaType.TV -> stringResource(R.string.details_play_s1e1)
                else -> stringResource(R.string.details_play)
            }

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
                    text = playLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.background
                )
            }

            if (trailer != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                        .clickable(onClick = onOpenTrailer)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("details_trailer_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SmartDisplay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.details_trailer),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
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

        if (savedProgress != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(savedProgress.progressFraction.coerceAtLeast(0.05f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                )
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
    val context = LocalContext.current
    val stillUrl = TmdbImages.still(episode.stillPath, "w300")

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
                    .width(96.dp)
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                if (stillUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(stillUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = episode.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.scrim)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = episode.episodeNumber.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
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
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (episode.overview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = episode.overview,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TrailerDialog(
    video: VideoItem,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = video.name.ifBlank { stringResource(R.string.details_trailer) },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            webChromeClient = WebChromeClient()
                            webViewClient = WebViewClient()
                            loadUrl("https://www.youtube.com/embed/${video.key}?autoplay=1&rel=0")
                        }
                    }
                )
            }
        }
    }
}

package com.example.ui.screens.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.ProgressItem
import com.example.data.model.TmdbItem
import com.example.ui.components.FilterPill
import com.example.ui.components.GridPosterCard
import com.example.ui.components.ProgressCard

@Composable
fun ListScreen(
    onOpenDetails: (TmdbItem) -> Unit,
    onResumeProgress: (ProgressItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListViewModel = viewModel(factory = ListViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = stringResource(R.string.my_list_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 108.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("my_list_grid"),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.progress.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "continue_watching_header") {
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(
                            text = stringResource(R.string.continue_watching),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(
                                items = state.progress,
                                key = { "${it.mediaType.path}-${it.id}-${it.season ?: 0}-${it.episode ?: 0}" }
                            ) { progress ->
                                ProgressCard(
                                    item = progress,
                                    onClick = { onResumeProgress(progress) },
                                    onRemove = { viewModel.removeProgress(progress.id, progress.mediaType) }
                                )
                            }
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "watchlist_header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.watchlist),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (state.totalWatchlistCount > 0) {
                            Text(
                                text = "${state.watchlist.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    if (state.totalWatchlistCount > 0) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            item {
                                FilterPill(
                                    label = stringResource(R.string.filter_all),
                                    selected = state.filter == WatchlistFilter.ALL,
                                    onClick = { viewModel.setFilter(WatchlistFilter.ALL) }
                                )
                            }
                            item {
                                FilterPill(
                                    label = stringResource(R.string.filter_movies),
                                    selected = state.filter == WatchlistFilter.MOVIES,
                                    onClick = { viewModel.setFilter(WatchlistFilter.MOVIES) }
                                )
                            }
                            item {
                                FilterPill(
                                    label = stringResource(R.string.filter_series),
                                    selected = state.filter == WatchlistFilter.SERIES,
                                    onClick = { viewModel.setFilter(WatchlistFilter.SERIES) }
                                )
                            }
                            item {
                                FilterPill(
                                    label = stringResource(R.string.sort_recent),
                                    selected = state.sort == WatchlistSort.RECENT,
                                    onClick = { viewModel.setSort(WatchlistSort.RECENT) }
                                )
                            }
                            item {
                                FilterPill(
                                    label = stringResource(R.string.sort_rating),
                                    selected = state.sort == WatchlistSort.RATING,
                                    onClick = { viewModel.setSort(WatchlistSort.RATING) }
                                )
                            }
                            item {
                                FilterPill(
                                    label = stringResource(R.string.sort_title),
                                    selected = state.sort == WatchlistSort.TITLE,
                                    onClick = { viewModel.setSort(WatchlistSort.TITLE) }
                                )
                            }
                        }
                    }
                }
            }

            if (state.watchlist.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "watchlist_empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp, start = 32.dp, end = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = stringResource(R.string.watchlist_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(
                    items = state.watchlist,
                    key = { "${it.resolvedMediaType.path}-${it.id}" }
                ) { item ->
                    GridPosterCard(
                        item = item,
                        onClick = { onOpenDetails(item) }
                    )
                }
            }
        }
    }
}

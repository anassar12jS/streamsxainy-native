package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.data.model.MediaType
import com.example.ui.screens.category.CategoryScreen
import com.example.ui.screens.details.DetailsScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.list.ListScreen
import com.example.ui.screens.player.PlayerScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.theme.NavBarBackground
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SearchRoute

@Serializable
data object ListRoute

@Serializable
data class CategoryRoute(
    val title: String,
    val categoryKey: String? = null,
    val genreId: Int = -1,
    val mediaType: String = "movie"
)

@Serializable
data class DetailsRoute(
    val type: String,
    val id: Int
)

@Serializable
data class PlayerRoute(
    val type: String,
    val id: Int,
    val season: Int = 1,
    val episode: Int = 1,
    val position: Int = 0,
    val title: String = "",
    val poster: String = ""
)

private enum class TopLevelTab(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME(HomeRoute, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
    SEARCH(SearchRoute, R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search, "tab_search"),
    MY_LIST(ListRoute, R.string.nav_my_list, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "tab_my_list")
}

@Composable
fun StreamsxainyNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val showChrome = destination == null ||
        destination.hasRoute<HomeRoute>() ||
        destination.hasRoute<SearchRoute>() ||
        destination.hasRoute<ListRoute>()

    val navigateToTab: (Any) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openDetails: (MediaType, Int) -> Unit = { type, id ->
        navController.navigate(DetailsRoute(type = type.path, id = id))
    }

    val openPlayer: (MediaType, Int, Int, Int, Int, String, String?) -> Unit =
        { type, id, season, episode, position, title, poster ->
            navController.navigate(
                PlayerRoute(
                    type = type.path,
                    id = id,
                    season = season,
                    episode = episode,
                    position = position,
                    title = title,
                    poster = poster.orEmpty()
                )
            )
        }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp && showChrome

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showChrome && !useRail) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NavBarBackground)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        NavigationBar(
                            containerColor = Color.Transparent,
                            windowInsets = WindowInsets.navigationBars,
                            tonalElevation = 0.dp
                        ) {
                            TopLevelTab.entries.forEach { tab ->
                                val selected = when (tab) {
                                    TopLevelTab.HOME -> destination?.hasRoute<HomeRoute>() == true
                                    TopLevelTab.SEARCH -> destination?.hasRoute<SearchRoute>() == true
                                    TopLevelTab.MY_LIST -> destination?.hasRoute<ListRoute>() == true
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    modifier = Modifier.testTag(tab.testTag),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            .windowInsetsPadding(WindowInsets.statusBars),
                        containerColor = NavBarBackground
                    ) {
                        TopLevelTab.entries.forEach { tab ->
                            val selected = when (tab) {
                                TopLevelTab.HOME -> destination?.hasRoute<HomeRoute>() == true
                                TopLevelTab.SEARCH -> destination?.hasRoute<SearchRoute>() == true
                                TopLevelTab.MY_LIST -> destination?.hasRoute<ListRoute>() == true
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                modifier = Modifier.testTag(tab.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                )
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            onOpenDetails = { item -> openDetails(item.resolvedMediaType, item.id) },
                            onPlayNow = { item ->
                                openPlayer(
                                    item.resolvedMediaType,
                                    item.id,
                                    1,
                                    1,
                                    0,
                                    item.displayTitle,
                                    item.posterPath
                                )
                            },
                            onResumeProgress = { p ->
                                openPlayer(
                                    p.mediaType,
                                    p.id,
                                    p.season ?: 1,
                                    p.episode ?: 1,
                                    p.position,
                                    p.title,
                                    p.posterPath
                                )
                            },
                            onSeeAllCategory = { category ->
                                navController.navigate(
                                    CategoryRoute(
                                        title = category.title,
                                        categoryKey = category.key
                                    )
                                )
                            },
                            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                        )
                    }

                    composable<SearchRoute> {
                        SearchScreen(
                            onItemClick = { item -> openDetails(item.resolvedMediaType, item.id) },
                            onGenreClick = { genre, type ->
                                navController.navigate(
                                    CategoryRoute(
                                        title = genre.name,
                                        genreId = genre.id,
                                        mediaType = type.path
                                    )
                                )
                            },
                            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                        )
                    }

                    composable<ListRoute> {
                        ListScreen(
                            onOpenDetails = { item -> openDetails(item.resolvedMediaType, item.id) },
                            onResumeProgress = { p ->
                                openPlayer(
                                    p.mediaType,
                                    p.id,
                                    p.season ?: 1,
                                    p.episode ?: 1,
                                    p.position,
                                    p.title,
                                    p.posterPath
                                )
                            },
                            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                        )
                    }

                    composable<CategoryRoute> { entry ->
                        val route = entry.toRoute<CategoryRoute>()
                        CategoryScreen(
                            title = route.title,
                            categoryKey = route.categoryKey,
                            genreId = route.genreId.takeIf { it >= 0 },
                            mediaType = MediaType.from(route.mediaType),
                            onBack = { navController.popBackStack() },
                            onItemClick = { item -> openDetails(item.resolvedMediaType, item.id) }
                        )
                    }

                    composable<DetailsRoute> { entry ->
                        val route = entry.toRoute<DetailsRoute>()
                        val type = MediaType.from(route.type)
                        DetailsScreen(
                            id = route.id,
                            mediaType = type,
                            onBack = { navController.popBackStack() },
                            onPlay = { season, episode, position, title, poster ->
                                openPlayer(type, route.id, season, episode, position, title, poster)
                            },
                            onOpenDetails = { item -> openDetails(item.resolvedMediaType, item.id) }
                        )
                    }

                    composable<PlayerRoute> { entry ->
                        val route = entry.toRoute<PlayerRoute>()
                        PlayerScreen(
                            tmdbId = route.id,
                            mediaType = MediaType.from(route.type),
                            season = route.season,
                            episode = route.episode,
                            startPosition = route.position,
                            title = route.title,
                            posterPath = route.poster.ifBlank { null },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

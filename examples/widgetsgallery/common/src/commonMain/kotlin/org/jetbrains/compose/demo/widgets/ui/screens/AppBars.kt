package org.jetbrains.compose.demo.widgets.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.demo.widgets.platform.Res
import org.jetbrains.compose.demo.widgets.platform.imageResource
import org.jetbrains.compose.demo.widgets.platform.vectorResource
import org.jetbrains.compose.demo.widgets.theme.twitterColor
import org.jetbrains.compose.demo.widgets.theme.typography
import org.jetbrains.compose.demo.widgets.ui.utils.SubtitleText
import org.jetbrains.compose.demo.widgets.ui.utils.TitleText

@Composable
fun AppBars() {
    TopAppBarsDemo()
    BottomAppBarDemo()
    NavigationBarDemo()
}

@Composable
private fun TopAppBarsDemo() {
    SubtitleText(subtitle = "Top App bar")

    TopAppBar(
        title = { Text(text = "Home") },
        elevation = 8.dp,
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.ArrowBack)
            }
        }
    )

    Spacer(modifier = Modifier.height(8.dp))

    TopAppBar(
        title = { Text(text = "Instagram") },
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        elevation = 8.dp,
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(vectorResource(Res.drawable.ic_instagram))
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(vectorResource(Res.drawable.ic_send))
            }
        }
    )

    Spacer(modifier = Modifier.height(8.dp))

    TopAppBar(
        title = {
            Icon(
                vectorResource(Res.drawable.ic_twitter),
                tint = twitterColor,
                modifier = Modifier.fillMaxWidth()
            )
        },
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        elevation = 8.dp,
        navigationIcon = {
            Image(
                imageResource(Res.drawable.p6),
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    .preferredSize(32.dp).clip(CircleShape)
            )
        },
        actions = {
            Icon(
                Icons.Default.StarBorder,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun BottomAppBarDemo() {
    Spacer(modifier = Modifier.height(16.dp))
    SubtitleText("Bottom app bars: Note bottom app bar support FAB cutouts when used with scafolds see demoUI crypto app")

    BottomAppBar(
        cutoutShape = CircleShape
    ) {
        IconButton(onClick = {}) {
            Icon(Icons.Default.MoreHoriz)
        }
        TitleText(title = "Bottom App Bar")
    }
}

@Composable
private fun NavigationBarDemo() {
    Spacer(modifier = Modifier.height(16.dp))
    SubtitleText(subtitle = "Bottom Navigation Bars")
    val navItemState = remember { mutableStateOf(NavType.HOME) }
    BottomNavigation(backgroundColor = MaterialTheme.colors.surface) {
        BottomNavigationItem(
            icon = { Icon(Icons.Outlined.Home) },
            selected = navItemState.value == NavType.HOME,
            onClick = { navItemState.value = NavType.HOME },
            label = { Text(text = Res.string.spotify_nav_home) },
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Outlined.Search) },
            selected = navItemState.value == NavType.SEARCH,
            onClick = { navItemState.value = NavType.SEARCH },
            label = { Text(text = Res.string.spotify_nav_search) }
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Outlined.LibraryMusic) },
            selected = navItemState.value == NavType.LIBRARY,
            onClick = { navItemState.value = NavType.LIBRARY },
            label = { Text(text = Res.string.spotify_nav_library) }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    BottomNavigation {
        BottomNavigationItem(
            icon = { Icon(Icons.Outlined.ReadMore) },
            selected = navItemState.value == NavType.HOME,
            onClick = { navItemState.value = NavType.HOME },
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Outlined.Search) },
            selected = navItemState.value == NavType.SEARCH,
            onClick = { navItemState.value = NavType.SEARCH },
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Outlined.CleanHands) },
            selected = navItemState.value == NavType.LIBRARY,
            onClick = { navItemState.value = NavType.LIBRARY },
        )
    }
}

private enum class NavType {
    HOME, SEARCH, LIBRARY
}
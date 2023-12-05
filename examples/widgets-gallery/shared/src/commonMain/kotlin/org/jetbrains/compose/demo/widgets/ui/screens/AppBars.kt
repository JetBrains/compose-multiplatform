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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.demo.widgets.theme.twitterColor
import org.jetbrains.compose.demo.widgets.ui.utils.SubtitleText
import org.jetbrains.compose.demo.widgets.ui.utils.TitleText
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.ImageResource

@Composable
fun AppBars() {
    TopAppBarsDemo()
    BottomAppBarDemo()
    NavigationBarDemo()
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun TopAppBarsDemo() {
    SubtitleText(subtitle = "Top App bar")

    TopAppBar(
        title = { Text(text = "Home") },
        elevation = 8.dp,
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.ArrowBack, contentDescription = "ArrowBack")
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
                Icon(painterResource(ImageResource("composeRes/images/ic_instagram.xml")), contentDescription = "Instagram")
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(painterResource(ImageResource("composeRes/images/ic_send.xml")), contentDescription = "Send")
            }
        }
    )

    Spacer(modifier = Modifier.height(8.dp))

    TopAppBar(
        title = {
            Icon(
                painterResource(ImageResource("composeRes/images/ic_twitter.xml")),
                contentDescription = "Twitter",
                tint = twitterColor,
                modifier = Modifier.fillMaxWidth()
            )
        },
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        elevation = 8.dp,
        navigationIcon = {
            Image(
                painterResource(ImageResource("composeRes/images/p6.jpeg")),
                contentDescription = "",
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    .requiredSize(32.dp).clip(CircleShape)
            )
        },
        actions = {
            Icon(
                Icons.Default.StarBorder,
                contentDescription = "",
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
            Icon(Icons.Default.MoreHoriz, contentDescription = "")
        }
        TitleText(title = "Bottom App Bar")
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun NavigationBarDemo() {
    Spacer(modifier = Modifier.height(16.dp))
    SubtitleText(subtitle = "Bottom Navigation Bars")
    val navItemState = remember { mutableStateOf(NavType.HOME) }
    BottomNavigation(backgroundColor = MaterialTheme.colors.surface) {
        BottomNavigationItem(
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
            selected = navItemState.value == NavType.HOME,
            onClick = { navItemState.value = NavType.HOME },
            label = { Text(text = Res.strings.spotify_nav_home) },
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
            selected = navItemState.value == NavType.SEARCH,
            onClick = { navItemState.value = NavType.SEARCH },
            label = { Text(text = Res.strings.spotify_nav_search) }
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Outlined.LibraryMusic, contentDescription = "LibraryMusic") },
            selected = navItemState.value == NavType.LIBRARY,
            onClick = { navItemState.value = NavType.LIBRARY },
            label = { Text(text = Res.strings.spotify_nav_library) }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    BottomNavigation {
        BottomNavigationItem(
            icon = { Icon(Icons.Outlined.ReadMore, contentDescription = "ReadMore") },
            selected = navItemState.value == NavType.HOME,
            onClick = { navItemState.value = NavType.HOME },
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
            selected = navItemState.value == NavType.SEARCH,
            onClick = { navItemState.value = NavType.SEARCH },
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Outlined.CleanHands, contentDescription = "CleanHands") },
            selected = navItemState.value == NavType.LIBRARY,
            onClick = { navItemState.value = NavType.LIBRARY },
        )
    }
}

private enum class NavType {
    HOME, SEARCH, LIBRARY
}
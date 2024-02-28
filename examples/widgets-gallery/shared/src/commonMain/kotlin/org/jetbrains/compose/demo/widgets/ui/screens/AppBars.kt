package org.jetbrains.compose.demo.widgets.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReadMore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.CleanHands
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.demo.widgets.theme.twitterColor
import org.jetbrains.compose.demo.widgets.ui.WidgetsType
import org.jetbrains.compose.demo.widgets.ui.utils.SubtitleText
import org.jetbrains.compose.demo.widgets.ui.utils.TitleText
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import widgets_gallery.shared.generated.resources.Res
import widgets_gallery.shared.generated.resources.ic_instagram
import widgets_gallery.shared.generated.resources.ic_send
import widgets_gallery.shared.generated.resources.ic_twitter
import widgets_gallery.shared.generated.resources.p6
import widgets_gallery.shared.generated.resources.spotify_nav_home
import widgets_gallery.shared.generated.resources.spotify_nav_library
import widgets_gallery.shared.generated.resources.spotify_nav_search

@Composable
fun AppBars() {
    Column(Modifier.testTag(WidgetsType.APP_BARS.testTag)) {
        TopAppBarsDemo()
        BottomAppBarDemo()
        NavigationBarDemo()
    }
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
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "ArrowBack")
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
                Icon(painterResource(Res.drawable.ic_instagram), contentDescription = "Instagram")
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(painterResource(Res.drawable.ic_send), contentDescription = "Send")
            }
        }
    )

    Spacer(modifier = Modifier.height(8.dp))

    TopAppBar(
        title = {
            Icon(
                painterResource(Res.drawable.ic_twitter),
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
                painterResource(Res.drawable.p6),
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
    SubtitleText("Bottom app bars: Note bottom app bar support FAB cutouts when used with scaffolds see demoUI crypto app")

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
            label = { Text(text = stringResource(Res.string.spotify_nav_home)) },
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
            selected = navItemState.value == NavType.SEARCH,
            onClick = { navItemState.value = NavType.SEARCH },
            label = { Text(text = stringResource(Res.string.spotify_nav_search)) }
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Outlined.LibraryMusic, contentDescription = "LibraryMusic") },
            selected = navItemState.value == NavType.LIBRARY,
            onClick = { navItemState.value = NavType.LIBRARY },
            label = { Text(text = stringResource(Res.string.spotify_nav_library)) }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    BottomNavigation {
        BottomNavigationItem(
            icon = { Icon(Icons.AutoMirrored.Outlined.ReadMore, contentDescription = "ReadMore") },
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
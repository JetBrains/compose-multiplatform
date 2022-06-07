/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.demos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LeadingIconTab
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun BadgeDemo() {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        var badgeCount by remember { mutableStateOf(8) }
        Spacer(Modifier.requiredHeight(24.dp))
        TopAppBarWithBadge(
            { badgeCount = 0 },
            badgeCount
        )
        Spacer(Modifier.requiredHeight(24.dp))
        BottomNavigationWithBadge(
            { badgeCount = 0 },
            artistsBadgeCount = badgeCount
        )
        Spacer(Modifier.requiredHeight(24.dp))
        TextTabsWithBadge(
            { badgeCount = 0 },
            tab1BadgeCount = badgeCount
        )
        Spacer(Modifier.requiredHeight(24.dp))
        LeadingIconTabsWithBadge(
            { badgeCount = 0 },
            tab1BadgeCount = badgeCount
        )
        Spacer(Modifier.requiredHeight(24.dp))
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                badgeCount++
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Cyan)
        ) {
            Text("+ badge number")
        }
        Spacer(Modifier.height(50.dp))
    }
}

@Composable
fun TopAppBarWithBadge(
    onActionIcon1BadgeClick: () -> Unit,
    actionIcon1BadgeCount: Int,
) {
    var showNavigationIconBadge by remember { mutableStateOf(true) }
    var showActionIcon2Badge by remember { mutableStateOf(true) }
    TopAppBar(
        title = { Text("Simple TopAppBar") },
        navigationIcon = {
            IconButton(onClick = { showNavigationIconBadge = false }) {
                if (showNavigationIconBadge) {
                    DemoBadgedBox(null) {
                        Icon(Icons.Filled.Menu, contentDescription = "Localized description")
                    }
                } else {
                    Icon(Icons.Filled.Menu, contentDescription = "Localized description")
                }
            }
        },
        actions = {
            // RowScope here, so these icons will be placed horizontally
            IconButton(
                onClick = onActionIcon1BadgeClick
            ) {
                if (actionIcon1BadgeCount > 0) {
                    DemoBadgedBox(actionIcon1BadgeCount.toString()) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                } else {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            IconButton(
                onClick = { showActionIcon2Badge = false }
            ) {
                if (showActionIcon2Badge) {
                    DemoBadgedBox("99+") {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                } else {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
    )
}

private const val initialSelectedIndex = 0

@Composable
fun BottomNavigationWithBadge(
    onArtistsBadgeClick: () -> Unit,
    artistsBadgeCount: Int
) {
    var selectedItem by remember { mutableStateOf(initialSelectedIndex) }
    val items = listOf("Songs", "Artists", "Playlists", "Something else")

    var showSongsBadge by remember { mutableStateOf(true) }
    var showPlaylistsBadge by remember { mutableStateOf(true) }

    Column {
        BottomNavigation {
            items.forEachIndexed { index, item ->
                val showBadge = when (index) {
                    0 -> showSongsBadge
                    1 -> artistsBadgeCount > 0
                    2 -> showPlaylistsBadge
                    else -> false
                }
                BottomNavigationItem(
                    icon = {
                        if (!showBadge) {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = "Localized description"
                            )
                        } else {
                            when (item) {
                                "Artists" -> {
                                    DemoBadgedBox(artistsBadgeCount.toString()) {
                                        Icon(
                                            Icons.Filled.Favorite,
                                            contentDescription = "Localized description"
                                        )
                                    }
                                }
                                else -> {
                                    DemoBadgedBox(
                                        when (index) {
                                            2 -> "99+"
                                            else -> null
                                        }
                                    ) {
                                        Icon(
                                            Icons.Filled.Favorite,
                                            contentDescription = "Localized description"
                                        )
                                    }
                                }
                            }
                        }
                    },
                    label = { Text(item) },
                    selected = selectedItem == index,
                    onClick = {
                        selectedItem = index
                        when (item) {
                            "Songs" -> showSongsBadge = false
                            "Artists" -> onArtistsBadgeClick()
                            "Playlists" -> showPlaylistsBadge = false
                        }
                    },
                    alwaysShowLabel = false
                )
            }
        }

        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Bottom nav with badge: ${selectedItem + 1} selected",
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
fun TextTabsWithBadge(
    onTab1BadgeClick: () -> Unit,
    tab1BadgeCount: Int
) {
    var state by remember { mutableStateOf(initialSelectedIndex) }
    val titles = listOf("TAB 1", "TAB 2", "TAB 3 WITH LOTS OF TEXT")
    val showTabBadgeList = remember { mutableStateListOf(true, true) }

    Column {
        TabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                val showBadge: Boolean = when (index) {
                    0 -> showTabBadgeList[0]
                    1 -> tab1BadgeCount > 0
                    2 -> showTabBadgeList[1]
                    else -> false
                }
                Tab(
                    text = {
                        if (!showBadge) {
                            Text(title)
                        } else {
                            DemoBadgedBox(
                                when (index) {
                                    1 -> tab1BadgeCount.toString()
                                    2 -> "99+"
                                    else -> null
                                }
                            ) {
                                Text(title)
                            }
                        }
                    },
                    selected = state == index,
                    onClick = {
                        state = index
                        when (index) {
                            0 -> showTabBadgeList[0] = false
                            1 -> onTab1BadgeClick()
                            2 -> showTabBadgeList[1] = false
                        }
                    }
                )
            }
        }

        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Icon tab with badge: ${state + 1} selected",
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
fun LeadingIconTabsWithBadge(
    onTab1BadgeClick: () -> Unit,
    tab1BadgeCount: Int,
) {
    var state by remember { mutableStateOf(0) }
    val titlesAndIcons = listOf(
        "TAB" to Icons.Filled.Favorite,
        "TAB & ICON" to Icons.Filled.Favorite,
        "TAB 3 WITH LOTS OF TEXT" to Icons.Filled.Favorite
    )
    val showTabBadgeList = remember { mutableStateListOf(true, true) }

    Column {
        TabRow(selectedTabIndex = state) {
            titlesAndIcons.forEachIndexed { index, (title, icon) ->
                val showBadge: Boolean = when (index) {
                    0 -> showTabBadgeList[0]
                    1 -> tab1BadgeCount > 0
                    2 -> showTabBadgeList[1]
                    else -> false
                }
                LeadingIconTab(
                    text = {
                        if (!showBadge) {
                            Text(title)
                        } else {
                            DemoBadgedBox(
                                when (index) {
                                    1 -> tab1BadgeCount.toString()
                                    2 -> "99+"
                                    else -> null
                                }
                            ) {
                                Text(title)
                            }
                        }
                    },
                    icon = {
                        Icon(
                            icon,
                            contentDescription = "Localized description"
                        )
                    },
                    selected = state == index,
                    onClick = {
                        state = index
                        when (index) {
                            0 -> showTabBadgeList[0] = false
                            1 -> onTab1BadgeClick()
                            2 -> showTabBadgeList[1] = false
                        }
                    }
                )
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Leading icon tab ${state + 1} selected",
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun DemoBadgedBox(
    badgeText: String?,
    content: @Composable () -> Unit
) {
    BadgedBox(
        badge = {
            Badge(
                content =
                    if (!badgeText.isNullOrEmpty()) {
                        {
                            Text(
                                badgeText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.semantics {
                                    this.contentDescription = "$badgeText notifications"
                                }
                            )
                        }
                    } else null
            )
        }
    ) {
        content()
    }
}

/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.desktop.examples.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ListItemUsage() {
    Column {
        ListItem(
            text = { Text("One line list item with no icon") },
            overlineText = { Text("overlineText") },
            trailing = { Text("trailing") }
        )
        Divider()
        ListItem(
            text = { Text("One line list item with 24x24 icon") },
            icon = {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null
                )
            },
            trailing = {
                Text("trailing")
            }
        )
        Divider()
        ListItem(
            text = { Text("One line list item with 40x40 icon") },
            icon = {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
        )
        Divider()
        ListItem(
            text = { Text("One line list item with 56x56 icon") },
            icon = {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
            }
        )
        Divider()
        ListItem(
            text = { Text("One line clickable list item") },
            icon = {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
            },
            modifier = Modifier.clickable { }
        )
        Divider()
        ListItem(
            text = { Text("One line list item with trailing icon") },
            trailing = {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Localized Description"
                )
            }
        )
        Divider()
        ListItem(
            text = { Text("One line list item") },
            icon = {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            },
            trailing = {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Localized description"
                )
            }
        )
        Divider()
    }
}

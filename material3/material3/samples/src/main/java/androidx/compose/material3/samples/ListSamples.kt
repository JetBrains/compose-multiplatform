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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Sampled
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun OneLineListItem() {
    Column {
        ListItem(
            headlineText = { Text("One line list item with 24x24 icon") },
            leadingContent = {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Localized description",
                )
            }
        )
        Divider()
    }
}

@Sampled
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TwoLineListItem() {
    Column {
        ListItem(
            headlineText = { Text("Two line list item with trailing") },
            supportingText = { Text("Secondary text") },
            trailingContent = { Text("meta") },
            leadingContent = {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Localized description",
                )
            }
        )
        Divider()
    }
}

@Sampled
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ThreeLineListItem() {
    Column {
        ListItem(
            headlineText = { Text("Three line list item") },
            overlineText = { Text("OVERLINE") },
            supportingText = { Text("Secondary text") },
            leadingContent = {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Localized description",
                )
            },
            trailingContent = { Text("meta") }
        )
        Divider()
    }
}
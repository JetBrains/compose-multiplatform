/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Sampled
@Composable
fun SimpleFab() {
    FloatingActionButton(onClick = { /*do something*/ }) {
        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
    }
}

@Composable
fun SimpleExtendedFabNoIcon() {
    ExtendedFloatingActionButton(
        text = { Text("EXTENDED") },
        onClick = {}
    )
}

@Sampled
@Composable
fun SimpleExtendedFabWithIcon() {
    ExtendedFloatingActionButton(
        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
        text = { Text("ADD TO BASKET") },
        onClick = { /*do something*/ }
    )
}

@Sampled
@Composable
fun FluidExtendedFab() {
    ExtendedFloatingActionButton(
        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
        text = { Text("FLUID FAB") },
        onClick = { /*do something*/ },
        modifier = Modifier.fillMaxWidth()
    )
}
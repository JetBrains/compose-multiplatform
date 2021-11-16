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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterialApi::class)
@Sampled
@Composable
fun FloatingActionButtonSample() {
    FloatingActionButton(
        onClick = { /* do something */ },
    ) {
        Icon(Icons.Filled.Add, "Localized description")
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Sampled
@Composable
fun SmallFloatingActionButtonSample() {
    SmallFloatingActionButton(
        onClick = { /* do something */ },
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Localized description")
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Sampled
@Composable
fun LargeFloatingActionButtonSample() {
    LargeFloatingActionButton(
        onClick = { /* do something */ },
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = "Localized description",
            modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Sampled
@Composable
fun ExtendedFloatingActionButtonSample() {
    ExtendedFloatingActionButton(
        onClick = { /* do something */ },
        icon = { Icon(Icons.Filled.Add, "Localized description") },
        text = { Text(text = "Extended FAB") },
    )
}

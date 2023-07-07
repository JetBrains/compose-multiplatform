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

package androidx.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Sampled
@Composable
fun ChipSample() {
    Chip(onClick = { /* Do something! */ }) {
        Text("Action Chip")
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Sampled
@Composable
fun OutlinedChipWithIconSample() {
    Chip(
        onClick = { /* Do something! */ },
        border = ChipDefaults.outlinedBorder,
        colors = ChipDefaults.outlinedChipColors(),
        leadingIcon = {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Localized description"
            )
        }
    ) {
        Text("Change settings")
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Sampled
@Composable
fun FilterChipSample() {
    val state = remember { mutableStateOf(false) }
    FilterChip(
        selected = state.value,
        onClick = { state.value = !state.value },
        selectedIcon = {
            Icon(
                imageVector = Icons.Filled.Done,
                contentDescription = "Localized Description",
                modifier = Modifier.requiredSize(ChipDefaults.SelectedIconSize)
            )
        }) {
        Text("Filter chip")
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Sampled
@Composable
fun OutlinedFilterChipSample() {
    val state = remember { mutableStateOf(false) }
    FilterChip(
        selected = state.value,
        onClick = { state.value = !state.value },
        border = ChipDefaults.outlinedBorder,
        colors = ChipDefaults.outlinedFilterChipColors(),
        selectedIcon = {
            Icon(
                imageVector = Icons.Filled.Done,
                contentDescription = "Localized Description",
                modifier = Modifier.requiredSize(ChipDefaults.SelectedIconSize)
            )
        }) {
        Text("Filter chip")
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Sampled
@Composable
fun FilterChipWithLeadingIconSample() {
    val state = remember { mutableStateOf(false) }
    FilterChip(
        selected = state.value,
        onClick = { state.value = !state.value },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = "Localized description",
                modifier = Modifier.requiredSize(ChipDefaults.LeadingIconSize)
            )
        },
        selectedIcon = {
            Icon(
                imageVector = Icons.Filled.Done,
                contentDescription = "Localized Description",
                modifier = Modifier.requiredSize(ChipDefaults.SelectedIconSize)
            )
        }
    ) {
        Text("Filter chip")
    }
}

@Sampled
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChipGroupSingleLineSample() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            repeat(9) { index ->
                Chip(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onClick = { /* do something*/ }) {
                    Text("Chip $index")
                }
            }
        }
    }
}

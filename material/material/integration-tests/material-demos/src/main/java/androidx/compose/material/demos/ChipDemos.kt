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

package androidx.compose.material.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.Switch
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

@Composable
fun ChipDemo() {
    val chipEnabledState = remember { mutableStateOf(true) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LazyColumn(
            contentPadding = PaddingValues(DefaultSpace),
            verticalArrangement = Arrangement.spacedBy(DefaultSpace)
        ) {
            item {
                Chips(chipEnabledState.value)
            }
            item {
                FilterChips(chipEnabledState.value)
            }
        }
        Row(horizontalArrangement = Arrangement.Center) {
            Text("Enabled")
            Spacer(Modifier.size(8.dp))
            Switch(
                checked = chipEnabledState.value,
                onCheckedChange = { chipEnabledState.value = it }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Chips(enabled: Boolean) {
    Text("Action Chips")
    Spacer(Modifier.height(DefaultSpace))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Chip(
            onClick = { /* Do something! */ },
            enabled = enabled
        ) {
            Text("Action chip")
        }
        Chip(
            onClick = { /* Do something! */ },
            border = ChipDefaults.outlinedBorder,
            colors = ChipDefaults.outlinedChipColors(),
            enabled = enabled,
            leadingIcon =
            {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null
                )
            }
        ) {
            Text("Action chip")
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun FilterChips(enabled: Boolean) {
    Text("Filter Chips")
    Spacer(Modifier.height(DefaultSpace))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        val state1 = remember { mutableStateOf(false) }
        FilterChip(
            selected = state1.value, onClick = { state1.value = !state1.value }, enabled = enabled,
            selectedIcon = {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Localized Description",
                    modifier = Modifier.size(ChipDefaults.SelectedIconSize)
                )
            }
        ) {
            Text("Filter chip")
        }
        val state2 = remember { mutableStateOf(false) }
        FilterChip(
            selected = state2.value,
            onClick = { state2.value = !state2.value },
            border = ChipDefaults.outlinedBorder,
            colors = ChipDefaults.outlinedFilterChipColors(),
            enabled = enabled,
            leadingIcon = {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = "Localized Description",
                    modifier = Modifier.size(ChipDefaults.LeadingIconSize)
                )
            },
            selectedIcon = {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Localized Description",
                    modifier = Modifier.size(ChipDefaults.SelectedIconSize)
                )
            }
        ) {
            Text("Filter chip")
        }
    }
}

private val DefaultSpace = 16.dp

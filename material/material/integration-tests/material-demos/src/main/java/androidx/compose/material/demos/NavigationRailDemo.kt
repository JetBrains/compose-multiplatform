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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.samples.CompactNavigationRailSample
import androidx.compose.material.samples.NavigationRailSample
import androidx.compose.material.samples.NavigationRailWithOnlySelectedLabelsSample
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NavigationRailDemo() {
    val alwaysShowLabelsState = remember { mutableStateOf(false) }
    val compactNavRail = remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        if (compactNavRail.value) {
            CompactNavigationRailSample()
        } else {
            if (alwaysShowLabelsState.value) {
                NavigationRailSample()
            } else {
                NavigationRailWithOnlySelectedLabelsSample()
            }
        }

        // The default width difference between a regular NavBar and a compact one is 16dp.
        // This will adjust the spacer width to compose the elements at the same position
        // when switching between the NavRail modes.
        Spacer(Modifier.width(if (compactNavRail.value) 32.dp else 16.dp))

        Column {
            LabelsConfig(enabled = !compactNavRail.value, alwaysShowLabelsState)
            CompactNavRailConfig(compactNavRail)
        }
    }
}

@Composable
private fun LabelsConfig(enabled: Boolean, alwaysShowLabelsState: MutableState<Boolean>) {
    Column(
        Modifier.selectableGroup()
            .padding(top = 16.dp, end = 16.dp)
            .border(
                width = 1.dp,
                color = LocalContentColor.current.copy(alpha = ContentAlpha.medium),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(start = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .selectable(
                    enabled = enabled,
                    selected = !alwaysShowLabelsState.value,
                    onClick = { alwaysShowLabelsState.value = false }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                enabled = enabled,
                selected = !alwaysShowLabelsState.value,
                onClick = null
            )
            Spacer(Modifier.requiredWidth(16.dp))
            Text("Only show labels when selected")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .selectable(
                    enabled = enabled,
                    selected = alwaysShowLabelsState.value,
                    onClick = { alwaysShowLabelsState.value = true }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                enabled = enabled,
                selected = alwaysShowLabelsState.value,
                onClick = null
            )
            Spacer(Modifier.requiredWidth(16.dp))
            Text("Always show labels")
        }
    }
}

@Composable
private fun CompactNavRailConfig(compactNavRailState: MutableState<Boolean>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(56.dp)
            .selectable(
                selected = compactNavRailState.value,
                onClick = { compactNavRailState.value = !compactNavRailState.value }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = compactNavRailState.value,
            onCheckedChange = null
        )
        Spacer(Modifier.requiredWidth(16.dp))
        Text("Compact Navigation Rail")
    }
}

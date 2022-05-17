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

package androidx.compose.animation.demos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun InspectionAnimatedVisibilityContentSizeChange() {
    Column {
        val isOpen = remember { mutableStateOf(true) }
        val itemListState = remember { mutableStateOf(listOf(1)) }
        val itemList = itemListState.value
        val (checked, onCheckedChanged) = remember { mutableStateOf(false) }
        Row(
            Modifier.height(60.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { isOpen.value = !isOpen.value },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text("Toggle\n visibility")
            }

            Row(modifier = Modifier.align(Alignment.CenterVertically)) {
                Checkbox(
                    checked,
                    onCheckedChanged,
                )
                Text("animateContentSize", Modifier.clickable { onCheckedChanged(!checked) })
            }

            Button(
                onClick = { itemListState.value = itemList + (itemList.size + 1) },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text("Add\n item")
            }
        }

        AnimatedVisibility(visible = isOpen.value) {
            Column(
                Modifier.background(pastelColors[2]).fillMaxWidth()
                    .then(if (checked) Modifier.animateContentSize() else Modifier)
            ) {
                itemList.map {
                    Text("Item #$it", Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}
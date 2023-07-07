/*
 * Copyright 2020 The Android expanded Source Project
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun MenuDemo() {
    Box {
        for (i in 0..10) {
            for (j in 0..10) {
                MenuInstance(
                    Modifier.fillMaxSize().wrapContentSize(
                        object : Alignment {
                            override fun align(
                                size: IntSize,
                                space: IntSize,
                                layoutDirection: LayoutDirection
                            ) = IntOffset(
                                (space.width * i / 10f).roundToInt(),
                                (space.height * j / 10f).roundToInt()
                            )
                        }
                    )
                )
            }
        }
    }
}

@Composable
fun MenuInstance(modifier: Modifier = Modifier) {
    val options = listOf(
        "Refresh",
        "Settings",
        "Send Feedback",
        "Help",
        "Signout"
    )

    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(24.dp, 0.dp),
        ) {
            options.forEach {
                DropdownMenuItem(onClick = {}) {
                    Text(it)
                }
            }
        }
    }
}
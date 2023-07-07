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

package androidx.compose.foundation.layout.benchmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.Modifier

class SimpleColumnWithRowsTestCase(
    private val subLayouts: Int,
    private val numberOfBoxes: Int
) : LayeredComposeTestCase(), ToggleableTestCase {

    private val numberOfSubLayouts = mutableStateOf(subLayouts)

    @Composable
    override fun MeasuredContent() {
        Column(modifier = Modifier.fillMaxSize()) {
            repeat(numberOfSubLayouts.value) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(numberOfBoxes) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    override fun toggleState() {
        if (numberOfSubLayouts.value == subLayouts) {
            numberOfSubLayouts.value = subLayouts - 1
        } else {
            numberOfSubLayouts.value = subLayouts
        }
    }
}
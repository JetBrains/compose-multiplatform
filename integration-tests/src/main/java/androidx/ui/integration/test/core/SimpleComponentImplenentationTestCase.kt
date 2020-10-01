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

package androidx.ui.integration.test.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

abstract class SimpleComponentImplenentationTestCase : ComposeTestCase, ToggleableTestCase {

    private var state: MutableState<Dp>? = null

    @Composable
    fun getInnerSize(): MutableState<Dp> {
        val innerSize = remember { mutableStateOf(10.dp) }
        state = innerSize
        return innerSize
    }

    override fun toggleState() {
        with(state!!) {
            value = if (value == 10.dp) {
                20.dp
            } else {
                10.dp
            }
        }
    }
}

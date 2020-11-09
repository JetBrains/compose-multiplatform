/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.integration.test.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.testutils.ComposeTestCase

/**
 * Test case representing a layout hierarchy of nested boxes.
 */
class NestedBoxesTestCase(
    private val depth: Int,
    private val children: Int
) : ComposeTestCase {

    @Composable
    override fun Content() {
        Box {
            Boxes(depth - 1, children)
        }
    }

    @Composable
    private fun Boxes(depth: Int, children: Int) {
        if (depth == 0) return
        repeat(children) {
            Box {
                Boxes(depth - 1, children)
            }
        }
    }
}
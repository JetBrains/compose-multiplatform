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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import kotlin.math.min

@Sampled
@Composable
fun AlignmentLineSample() {
    // Create a horizontal alignment line. Note it is not common for alignment lines to be created
    // in the scope of one composable, since they are usually used across more than one function.
    // We use ::min as merging strategy, which means that the parent will have the minimum of
    // the values of the alignment line, when this is inherited from more than one child.
    val exampleLine = remember { HorizontalAlignmentLine(::min) }

    // A layout with a fixed size, and a given position for the exampleLine alignment line.
    @Composable
    fun LineProviderLayout(exampleLinePosition: Int) {
        val size: Int = 20
        Layout({}) { _, _ ->
            layout(size, size, mapOf(exampleLine to exampleLinePosition)) {}
        }
    }

    Layout({
        LineProviderLayout(exampleLinePosition = 5)
        LineProviderLayout(exampleLinePosition = 10)
    }) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        // placeables[0][line] will be 5
        // placeables[1][line] will be 10
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables[0].place(0, 3)
            placeables[1].place(constraints.maxWidth / 2, 0)
        }
    }
    // Note that if the parent of this Layout (the parent of AlignmentLineSample) was able to
    // query its position of exampleLine (assuming this was in a shared scope), its position would
    // be 8. This is because the Layout positioned its first child at 3 from the top,
    // and because of the ::min merging strategy the position of exampleLine will be
    // min(5 + 3, 10 + 0).
}

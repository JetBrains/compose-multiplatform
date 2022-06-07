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

package androidx.compose.ui.platform

import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize

/**
 * Class for storing outline. Recalculates outline when [size] or [shape] is changed.
 * It' s needed so we don't have to recreate it every time we use it for rendering
 * (it can be expensive to create outline every frame).
 */
internal class OutlineCache(
    density: Density,
    size: IntSize,
    shape: Shape,
    layoutDirection: LayoutDirection
) {
    var density = density
        set(value) {
            if (value != field) {
                field = value
                outline = createOutline()
            }
        }

    var size = size
        set(value) {
            if (value != field) {
                field = value
                outline = createOutline()
            }
        }

    var shape = shape
        set(value) {
            if (value != field) {
                field = value
                outline = createOutline()
            }
        }

    var layoutDirection = layoutDirection
        set(value) {
            if (value != field) {
                field = value
                outline = createOutline()
            }
        }

    var outline: Outline = createOutline()
        private set

    private fun createOutline() =
        shape.createOutline(size.toSize(), layoutDirection, density)
}
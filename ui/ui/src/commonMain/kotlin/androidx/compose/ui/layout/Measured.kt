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

package androidx.compose.ui.layout

/**
 * Read-only wrapper over [Placeable] that exposes the measurement result with no placing ability.
 */
inline class Measured(internal val placeable: Placeable) {
    /**
     * The measured width of the layout.
     */
    val width: Int get() = placeable.measuredWidth

    /**
     * The measured height of the layout.
     */
    val height: Int get() = placeable.measuredHeight

    /**
     * Returns the position of an [alignment line][AlignmentLine],
     * or `null` if the line is not provided.
     */
    operator fun get(alignmentLine: AlignmentLine): Int = placeable[alignmentLine]
}
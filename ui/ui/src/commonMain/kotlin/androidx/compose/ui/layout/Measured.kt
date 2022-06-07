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
 * A [Measured] corresponds to a layout that has been measured by its parent layout.
 */
interface Measured {
    /**
     * The measured width of the layout. This might not respect the measurement constraints.
     */
    val measuredWidth: Int

    /**
     * The measured height of the layout. This might not respect the measurement constraints.
     */
    val measuredHeight: Int

    /**
     * Data provided by the [ParentDataModifier] applied to the layout.
     */
    val parentData: Any? get() = null

    /**
     * Returns the position of an [alignment line][AlignmentLine],
     * or [AlignmentLine.Unspecified] if the line is not provided.
     */
    operator fun get(alignmentLine: AlignmentLine): Int
}

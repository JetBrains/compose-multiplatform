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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.IntSize

/**
 * Invoke [onSizeChanged] with the size of the content after it has been measured.
 * This will only be invoked either the first time measurement happens or when the content
 * size has changed.
 *
 * Use [Layout] or [SubcomposeLayout] to have the size of one component to affect the size
 * of another component. Using the size received from the [onSizeChanged] callback in a
 * [MutableState] to affect layout will cause the new value to be recomposed and read only in the
 * following frame, causing a one frame lag.
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.OnSizeChangedSample
 */
@Suppress("ModifierInspectorInfo") // cannot access crossinline parameter
inline fun Modifier.onSizeChanged(
    crossinline onSizeChanged: (IntSize) -> Unit
) = composed {
    remember {
        object : OnRemeasuredModifier {
            private var previousSize = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)
            override fun onRemeasured(size: IntSize) {
                if (previousSize != size) {
                    onSizeChanged(size)
                    previousSize = size
                }
            }
        }
    }
}

/**
 * A modifier whose [onRemeasured] is called when the layout content is remeasured. The
 * most common usage is [onSizeChanged].
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.OnSizeChangedSample
 */
interface OnRemeasuredModifier : Modifier.Element {
    /**
     * Called after a layout's contents have been remeasured.
     */
    fun onRemeasured(size: IntSize)
}

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
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.internal.JvmDefaultWithCompatibility

/**
 * Invoke [onSizeChanged] when the size of the modifier immediately after it has changed. If
 * there is no modifier following [onSizeChanged], the content size of the layout is reported.
 *
 * There are no guarantees [onSizeChanged] will not be re-invoked with the same size.
 *
 * Use [Layout] or [SubcomposeLayout] to have the size of one component affect the size
 * of another component. Using the size received from the [onSizeChanged] callback in a
 * [MutableState] to affect layout will cause the new value to be recomposed and read only in the
 * following frame, causing a one frame lag. You can use [onSizeChanged] to affect
 * drawing operations.
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.OnSizeChangedSample
 */
@Stable
fun Modifier.onSizeChanged(
    onSizeChanged: (IntSize) -> Unit
) = this.then(
    OnSizeChangedModifier(
        onSizeChanged = onSizeChanged,
        inspectorInfo = debugInspectorInfo {
            name = "onSizeChanged"
            properties["onSizeChanged"] = onSizeChanged
        }
    )
)

private class OnSizeChangedModifier(
    val onSizeChanged: (IntSize) -> Unit,
    inspectorInfo: InspectorInfo.() -> Unit
) : OnRemeasuredModifier, InspectorValueInfo(inspectorInfo) {
    private var previousSize = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)

    override fun onRemeasured(size: IntSize) {
        if (previousSize != size) {
            onSizeChanged(size)
            previousSize = size
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OnSizeChangedModifier) return false

        return onSizeChanged == other.onSizeChanged
    }

    override fun hashCode(): Int {
        return onSizeChanged.hashCode()
    }
}

/**
 * A modifier whose [onRemeasured] is called when the layout content is remeasured. The
 * most common usage is [onSizeChanged].
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.OnSizeChangedSample
 */
@JvmDefaultWithCompatibility
interface OnRemeasuredModifier : Modifier.Element {
    /**
     * Called after a layout's contents have been remeasured.
     */
    fun onRemeasured(size: IntSize)
}

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

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.internal.JvmDefaultWithCompatibility

/**
 * Invoke [onGloballyPositioned] with the [LayoutCoordinates] of the element when the
 * global position of the content may have changed.
 * Note that it will be called **after** a composition when the coordinates are finalized.
 *
 * This callback will be invoked at least once when the [LayoutCoordinates] are available, and every
 * time the element's position changes within the window. However, it is not guaranteed to be
 * invoked every time the position _relative to the screen_ of the modified element changes. For
 * example, the system may move the contents inside a window around without firing a callback.
 * If you are using the [LayoutCoordinates] to calculate position on the screen, and not just inside
 * the window, you may not receive a callback.
 *
 * Usage example:
 * @sample androidx.compose.ui.samples.OnGloballyPositioned
 */
@Stable
fun Modifier.onGloballyPositioned(
    onGloballyPositioned: (LayoutCoordinates) -> Unit
) = this.then(
    OnGloballyPositionedModifierImpl(
        callback = onGloballyPositioned,
        inspectorInfo = debugInspectorInfo {
            name = "onGloballyPositioned"
            properties["onGloballyPositioned"] = onGloballyPositioned
        }
    )
)

private class OnGloballyPositionedModifierImpl(
    val callback: (LayoutCoordinates) -> Unit,
    inspectorInfo: InspectorInfo.() -> Unit
) : OnGloballyPositionedModifier, InspectorValueInfo(inspectorInfo) {
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        callback(coordinates)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OnGloballyPositionedModifierImpl) return false

        return callback == other.callback
    }

    override fun hashCode(): Int {
        return callback.hashCode()
    }
}

/**
 * A modifier whose [onGloballyPositioned] is called with the final LayoutCoordinates of the
 * Layout when the global position of the content may have changed.
 * Note that it will be called after a composition when the coordinates are finalized.
 *
 * Usage example:
 * @sample androidx.compose.ui.samples.OnGloballyPositioned
 */
@JvmDefaultWithCompatibility
interface OnGloballyPositionedModifier : Modifier.Element {
    /**
     * Called with the final LayoutCoordinates of the Layout after measuring.
     * Note that it will be called after a composition when the coordinates are finalized.
     * The position in the modifier chain makes no difference in either
     * the [LayoutCoordinates] argument or when the [onGloballyPositioned] is called.
     */
    fun onGloballyPositioned(coordinates: LayoutCoordinates)
}

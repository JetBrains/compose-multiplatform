/*
 * Copyright 2021 The Android Open Source Project
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
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * Invoke [onPlaced] after the parent [LayoutModifier] and parent layout has been placed and before
 * child [LayoutModifier] is placed. This allows child [LayoutModifier] to adjust its
 * own placement based on where the parent is.
 *
 * @sample androidx.compose.ui.samples.OnPlaced
 */
@Stable
fun Modifier.onPlaced(
    onPlaced: (LayoutCoordinates) -> Unit
) = this.then(
    OnPlacedModifierImpl(
        callback = onPlaced,
        inspectorInfo = debugInspectorInfo {
            name = "onPlaced"
            properties["onPlaced"] = onPlaced
        }
    )
)

private class OnPlacedModifierImpl(
    val callback: (LayoutCoordinates) -> Unit,
    inspectorInfo: InspectorInfo.() -> Unit
) : OnPlacedModifier, InspectorValueInfo(inspectorInfo) {

    override fun onPlaced(coordinates: LayoutCoordinates) {
        callback(coordinates)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OnPlacedModifierImpl) return false

        return callback == other.callback
    }

    override fun hashCode(): Int {
        return callback.hashCode()
    }
}

/**
 * A modifier whose [onPlaced] is called after the parent [LayoutModifier] and parent layout has
 * been placed and before child [LayoutModifier] is placed. This allows child
 * [LayoutModifier] to adjust its own placement based on where the parent is.
 *
 * @sample androidx.compose.ui.samples.OnPlaced
 */
@JvmDefaultWithCompatibility
interface OnPlacedModifier : Modifier.Element {
    /**
     * [onPlaced] is called after parent [LayoutModifier] and parent layout gets placed and
     * before any child [LayoutModifier] is placed.
     *
     * [coordinates] provides [LayoutCoordinates] of the [OnPlacedModifier]. Placement in both
     * parent [LayoutModifier] and parent layout can be calculated using the [LayoutCoordinates].
     */
    fun onPlaced(coordinates: LayoutCoordinates)
}
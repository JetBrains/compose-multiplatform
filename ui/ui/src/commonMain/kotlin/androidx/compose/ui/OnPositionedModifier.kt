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

package androidx.compose.ui

import androidx.compose.ui.layout.LayoutCoordinates

/**
 * Invoke [onPositioned] with the [LayoutCoordinates] of the element after positioning.
 * Note that it will be called **after** a composition when the coordinates are finalized.
 *
 * Usage example:
 * @sample androidx.compose.ui.samples.OnPositionedSample
 */
// TODO inline me!
fun Modifier.onPositioned(
    onPositioned: (LayoutCoordinates) -> Unit
) = this.then(object : OnPositionedModifier {
    override fun onPositioned(coordinates: LayoutCoordinates) {
        onPositioned(coordinates)
    }
})

/**
 * A modifier whose [onPositioned] is called with the final LayoutCoordinates of the Layout
 * after measuring.
 * Note that it will be called after a composition when the coordinates are finalized.
 *
 * Usage example:
 * @sample androidx.compose.ui.samples.OnPositionedSample
 */
interface OnPositionedModifier : Modifier.Element {
    /**
     * Called with the final LayoutCoordinates of the Layout after measuring.
     * Note that it will be called after a composition when the coordinates are finalized.
     * The position in the modifier chain makes no difference in either
     * the [LayoutCoordinates] argument or when the [onPositioned] is called.
     */
    fun onPositioned(coordinates: LayoutCoordinates)
}

/**
 * Returns modifier whose [onPositioned] is called with the final LayoutCoordinates of the Layout
 * after measuring.
 * Note that it will be called after a composition when the coordinates are finalized.
 *
 * Usage example:
 * @sample androidx.compose.ui.samples.OnPositionedSample
 */
@Deprecated(
    "use Modifier.onPositioned",
    replaceWith = ReplaceWith(
        "Modifier.onPositioned(onPositioned)",
        "androidx.compose.ui.Modifier",
        "androidx.compose.ui.onPositioned"
    )
)
fun onPositioned(onPositioned: (LayoutCoordinates) -> Unit): Modifier =
    Modifier.onPositioned(onPositioned)

/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.selection

import androidx.compose.runtime.ambientOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates

/**
 *  An interface allowing a composable to subscribe and unsubscribe to selection changes.
 */
interface SelectionRegistrar {
    /**
     * Subscribe to SelectionContainer selection changes.
     */
    fun subscribe(selectable: Selectable): Selectable

    /**
     * Unsubscribe from SelectionContainer selection changes.
     */
    fun unsubscribe(selectable: Selectable)

    /**
     * When the Global Position of a subscribed [Selectable] changes, this method
     * is called.
     */
    fun onPositionChange()

    /**
     * When selection changes, this method is called.
     *
     * @param layoutCoordinates [LayoutCoordinates] of the [Selectable].
     * @param startPosition coordinates of where the selection starts.
     * @param endPosition coordinates of where the selection ends.
     */
    fun onUpdateSelection(
        layoutCoordinates: LayoutCoordinates,
        startPosition: Offset,
        endPosition: Offset
    )
}

/**
 * Ambient of SelectionRegistrar. Composables that implement selection logic can use this ambient
 * to get a [SelectionRegistrar] in order to subscribe and unsubscribe to [SelectionRegistrar].
 */
@Suppress("AmbientNaming")
@Deprecated(
    "Renamed to AmbientSelectionRegistrar",
    replaceWith = ReplaceWith(
        "AmbientSelectionRegistrar",
        "androidx.compose.ui.selection.AmbientSelectionRegistrar"
    )
)
val SelectionRegistrarAmbient get() = AmbientSelectionRegistrar

/**
 * Ambient of SelectionRegistrar. Composables that implement selection logic can use this ambient
 * to get a [SelectionRegistrar] in order to subscribe and unsubscribe to [SelectionRegistrar].
 */
val AmbientSelectionRegistrar = ambientOf<SelectionRegistrar?>()

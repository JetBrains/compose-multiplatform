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
import androidx.compose.ui.text.ExperimentalTextApi

/**
 *  An interface allowing a composable to subscribe and unsubscribe to selection changes.
 */
@ExperimentalTextApi
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
    fun notifyPositionChange()

    /**
     * Call this method to notify the [SelectionContainer] that the selection has been initiated.
     * Depends on the input, [notifySelectionUpdate] may be called repeatedly after
     * [notifySelectionUpdateStart] is called. And [notifySelectionUpdateEnd] should always be
     * called after selection finished.
     * For example:
     *  1. User long pressed the text and then release. [notifySelectionUpdateStart] should be
     *  called followed by [notifySelectionUpdateEnd] being called once.
     *  2. User long pressed the text and then drag a distance and then release.
     *  [notifySelectionUpdateStart] should be called first after the user long press, and then
     *  [notifySelectionUpdate] is called several times reporting the updates, in the end
     *  [notifySelectionUpdateEnd] is called to finish the selection.
     *
     * @param layoutCoordinates [LayoutCoordinates] of the [Selectable].
     * @param startPosition coordinates of where the selection is initiated.
     *
     * @see notifySelectionUpdate
     * @see notifySelectionUpdateEnd
     */
    fun notifySelectionUpdateStart(
        layoutCoordinates: LayoutCoordinates,
        startPosition: Offset
    )

    /**
     * Call this method to notify the [SelectionContainer] that  the selection has been updated.
     * The caller of this method should make sure that [notifySelectionUpdateStart] is always
     * called once before calling this function. And [notifySelectionUpdateEnd] is always called
     * once after the all updates finished.
     *
     * @param layoutCoordinates [LayoutCoordinates] of the [Selectable].
     * @param startPosition coordinates of where the selection starts.
     * @param endPosition coordinates of where the selection ends.
     *
     * @see notifySelectionUpdateStart
     * @see notifySelectionUpdateEnd
     */
    fun notifySelectionUpdate(
        layoutCoordinates: LayoutCoordinates,
        startPosition: Offset,
        endPosition: Offset,
    )

    /**
     * Call this method to notify the [SelectionContainer] that the selection update has stopped.
     *
     * @see notifySelectionUpdateStart
     * @see notifySelectionUpdate
     */
    fun notifySelectionUpdateEnd()

    /**
     * Call this method to notify the [SelectionContainer] that the content of the passed
     * selectable has been changed.
     *
     * @param selectable the selectable whose the content has been updated.
     */
    fun notifySelectableChange(selectable: Selectable)
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
@OptIn(ExperimentalTextApi::class)
val AmbientSelectionRegistrar = ambientOf<SelectionRegistrar?>()

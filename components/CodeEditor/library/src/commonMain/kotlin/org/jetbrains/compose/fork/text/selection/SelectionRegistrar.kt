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

package org.jetbrains.compose.fork.text.selection

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates

/**
 *  An interface allowing a composable to subscribe and unsubscribe to selection changes.
 */
internal interface SelectionRegistrar {
    /**
     * The map stored current selection information on each [Selectable]. A selectable can query
     * its selected range using its [Selectable.selectableId]. This field is backed by a
     * [MutableState]. And any composable reading this field will be recomposed once its value
     * changed.
     */
    val subselections: Map<Long, Selection>

    /**
     * Subscribe to SelectionContainer selection changes.
     * @param selectable the [Selectable] that is subscribing to this [SelectionRegistrar].
     */
    fun subscribe(selectable: Selectable): Selectable

    /**
     * Unsubscribe from SelectionContainer selection changes.
     * @param selectable the [Selectable] that is unsubscribing to this [SelectionRegistrar].
     */
    fun unsubscribe(selectable: Selectable)

    /**
     * Return a unique ID for a [Selectable].
     * @see [Selectable.selectableId]
     */
    fun nextSelectableId(): Long

    /**
     * When the Global Position of a subscribed [Selectable] changes, this method
     * is called.
     */
    fun notifyPositionChange(selectableId: Long)

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
     * @param adjustment selection should be adjusted according to this param
     *
     * @see notifySelectionUpdate
     * @see notifySelectionUpdateEnd
     */
    fun notifySelectionUpdateStart(
        layoutCoordinates: LayoutCoordinates,
        startPosition: Offset,
        adjustment: SelectionAdjustment
    )

    /**
     * Call this method to notify the [SelectionContainer] that the selection has been initiated
     * with selectAll [Selection].
     *
     * @param selectableId [selectableId] of the [Selectable]
     */
    fun notifySelectionUpdateSelectAll(selectableId: Long)

    /**
     * Call this method to notify the [SelectionContainer] that one of the selection handle has
     * moved and selection should be updated.
     * The caller of this method should make sure that [notifySelectionUpdateStart] is always
     * called once before calling this function. And [notifySelectionUpdateEnd] is always called
     * once after the all updates finished.
     *
     * @param layoutCoordinates [LayoutCoordinates] of the [Selectable].
     * @param previousPosition coordinates of where the selection starts.
     * @param newPosition coordinates of where the selection ends.
     * @param isStartHandle whether the moving selection handle the start handle.
     * @param adjustment selection should be adjusted according to this parameter
     *
     * @return true if the selection handle movement is consumed. This function acts like a
     * pointer input consumer when a selection handle is dragged. It expects the caller to
     * accumulate the unconsumed pointer movement:
     * 1. if it returns true, the caller will zero out the previous movement.
     * 2. if it returns false, the caller will continue accumulate pointer movement.
     * @see notifySelectionUpdateStart
     * @see notifySelectionUpdateEnd
     */
    fun notifySelectionUpdate(
        layoutCoordinates: LayoutCoordinates,
        newPosition: Offset,
        previousPosition: Offset,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment
    ): Boolean

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
     * @param selectableId the ID of the selectable whose the content has been updated.
     */
    fun notifySelectableChange(selectableId: Long)

    companion object {
        /**
         * Representing an invalid ID for [Selectable].
         */
        const val InvalidSelectableId = 0L
    }
}

/**
 * Helper function that checks if there is a selection on this CoreText.
 */
internal fun SelectionRegistrar?.hasSelection(selectableId: Long): Boolean {
    return this?.subselections?.containsKey(selectableId) ?: false
}

/**
 * SelectionRegistrar CompositionLocal. Composables that implement selection logic can use this
 * CompositionLocal to get a [SelectionRegistrar] in order to subscribe and unsubscribe to
 * [SelectionRegistrar].
 */
internal val LocalSelectionRegistrar = compositionLocalOf<SelectionRegistrar?> { null }

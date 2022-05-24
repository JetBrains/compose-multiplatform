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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.AtomicLong
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates

internal class SelectionRegistrarImpl : SelectionRegistrar {
    /**
     * A flag to check if the [Selectable]s have already been sorted.
     */
    internal var sorted: Boolean = false

    /**
     * This is essentially the list of registered components that want
     * to handle text selection that are below the SelectionContainer.
     */
    private val _selectables = mutableListOf<Selectable>()

    /**
     * Getter for handlers that returns a List.
     */
    internal val selectables: List<Selectable>
        get() = _selectables

    private val _selectableMap = mutableMapOf<Long, Selectable>()

    /**
     * A map from selectable keys to subscribed selectables.
     */
    internal val selectableMap: Map<Long, Selectable>
        get() = _selectableMap

    /**
     * The incremental id to be assigned to each selectable. It starts from 1 and 0 is used to
     * denote an invalid id.
     * @see SelectionRegistrar.InvalidSelectableId
     */
    private var incrementId = AtomicLong(1)

    /**
     * The callback to be invoked when the position change was triggered.
     */
    internal var onPositionChangeCallback: ((Long) -> Unit)? = null

    /**
     * The callback to be invoked when the selection is initiated.
     */
    internal var onSelectionUpdateStartCallback:
        ((LayoutCoordinates, Offset, SelectionAdjustment) -> Unit)? = null

    /**
     * The callback to be invoked when the selection is initiated with selectAll [Selection].
     */
    internal var onSelectionUpdateSelectAll: (
        (Long) -> Unit
    )? = null

    /**
     * The callback to be invoked when the selection is updated.
     * If the first offset is null it means that the start of selection is unknown for the caller.
     */
    internal var onSelectionUpdateCallback:
        ((LayoutCoordinates, Offset, Offset, Boolean, SelectionAdjustment) -> Boolean)? = null

    /**
     * The callback to be invoked when selection update finished.
     */
    internal var onSelectionUpdateEndCallback: (() -> Unit)? = null

    /**
     * The callback to be invoked when one of the selectable has changed.
     */
    internal var onSelectableChangeCallback: ((Long) -> Unit)? = null

    /**
     * The callback to be invoked after a selectable is unsubscribed from this [SelectionRegistrar].
     */
    internal var afterSelectableUnsubscribe: ((Long) -> Unit)? = null

    override var subselections: Map<Long, Selection> by mutableStateOf(emptyMap())

    override fun subscribe(selectable: Selectable): Selectable {
        require(selectable.selectableId != SelectionRegistrar.InvalidSelectableId) {
            "The selectable contains an invalid id: ${selectable.selectableId}"
        }
        require(!_selectableMap.containsKey(selectable.selectableId)) {
            "Another selectable with the id: $selectable.selectableId has already subscribed."
        }
        _selectableMap[selectable.selectableId] = selectable
        _selectables.add(selectable)
        sorted = false
        return selectable
    }

    override fun unsubscribe(selectable: Selectable) {
        if (!_selectableMap.containsKey(selectable.selectableId)) return
        _selectables.remove(selectable)
        _selectableMap.remove(selectable.selectableId)
        afterSelectableUnsubscribe?.invoke(selectable.selectableId)
    }

    override fun nextSelectableId(): Long {
        var id = incrementId.getAndIncrement()
        while (id == SelectionRegistrar.InvalidSelectableId) {
            id = incrementId.getAndIncrement()
        }
        return id
    }

    /**
     * Sort the list of registered [Selectable]s in [SelectionRegistrar]. Currently the order of
     * selectables is geometric-based.
     */
    fun sort(containerLayoutCoordinates: LayoutCoordinates): List<Selectable> {
        if (!sorted) {
            // Sort selectables by y-coordinate first, and then x-coordinate, to match English
            // hand-writing habit.
            _selectables.sortWith { a: Selectable, b: Selectable ->
                val layoutCoordinatesA = a.getLayoutCoordinates()
                val layoutCoordinatesB = b.getLayoutCoordinates()

                val positionA = if (layoutCoordinatesA != null) {
                    containerLayoutCoordinates.localPositionOf(layoutCoordinatesA, Offset.Zero)
                } else {
                    Offset.Zero
                }
                val positionB = if (layoutCoordinatesB != null) {
                    containerLayoutCoordinates.localPositionOf(layoutCoordinatesB, Offset.Zero)
                } else {
                    Offset.Zero
                }

                if (positionA.y == positionB.y) {
                    compareValues(positionA.x, positionB.x)
                } else {
                    compareValues(positionA.y, positionB.y)
                }
            }
            sorted = true
        }
        return selectables
    }

    override fun notifyPositionChange(selectableId: Long) {
        // Set the variable sorted to be false, when the global position of a registered
        // selectable changes.
        sorted = false
        onPositionChangeCallback?.invoke(selectableId)
    }

    override fun notifySelectionUpdateStart(
        layoutCoordinates: LayoutCoordinates,
        startPosition: Offset,
        adjustment: SelectionAdjustment
    ) {
        onSelectionUpdateStartCallback?.invoke(layoutCoordinates, startPosition, adjustment)
    }

    override fun notifySelectionUpdateSelectAll(selectableId: Long) {
        onSelectionUpdateSelectAll?.invoke(selectableId)
    }

    override fun notifySelectionUpdate(
        layoutCoordinates: LayoutCoordinates,
        newPosition: Offset,
        previousPosition: Offset,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment
    ): Boolean {
        return onSelectionUpdateCallback?.invoke(
            layoutCoordinates,
            newPosition,
            previousPosition,
            isStartHandle,
            adjustment
        ) ?: true
    }

    override fun notifySelectionUpdateEnd() {
        onSelectionUpdateEndCallback?.invoke()
    }

    override fun notifySelectableChange(selectableId: Long) {
        onSelectableChangeCallback?.invoke(selectableId)
    }
}
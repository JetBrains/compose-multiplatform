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

package androidx.compose.ui.input.pointer

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.InternalCoreApi

/**
 * The normalized data structure for pointer input event information that is taken in processed by
 * Compose (via the [PointerInputEventProcessor]).
 *
 * All pointer locations are relative to the device screen.
 */
@InternalCoreApi
internal expect class PointerInputEvent {
    val uptime: Long
    val pointers: List<PointerInputEventData>
}

/**
 * Data that describes a particular pointer
 *
 * [positionOnScreen] is relative to the device screen. [position] is relative to the owner.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal data class PointerInputEventData(
    val id: PointerId,
    val uptime: Long,
    val positionOnScreen: Offset,
    val position: Offset,
    val down: Boolean,
    val type: PointerType,
    val historical: List<HistoricalChange> = mutableListOf()
)

/**
 * Represents a pointer input event internally.
 *
 * [PointerInputChange]s are stored in a map so that as this internal event traverses the tree,
 * it is efficient to split the changes between those that are relevant to the sub tree and those
 * that are not.
 */
@OptIn(InternalCoreApi::class)
internal expect class InternalPointerEvent(
    changes: Map<PointerId, PointerInputChange>,
    pointerInputEvent: PointerInputEvent
) {
    val changes: Map<PointerId, PointerInputChange>
}
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

package androidx.compose.ui.input.pointer

import java.awt.event.MouseEvent

/**
 * Describes a pointer input change event that has occurred at a particular point in time.
 */
actual data class PointerEvent internal constructor(
    /**
     * The changes.
     */
    actual val changes: List<PointerInputChange>,

    /**
     * Original raw native event from AWT
     */
    val mouseEvent: MouseEvent?
) {
    internal actual constructor(
        changes: List<PointerInputChange>,
        internalPointerEvent: InternalPointerEvent?
    ) : this(changes, internalPointerEvent?.mouseEvent)

    /**
     * @param changes The changes.
     */
    actual constructor(changes: List<PointerInputChange>) : this(changes, mouseEvent = null)
}
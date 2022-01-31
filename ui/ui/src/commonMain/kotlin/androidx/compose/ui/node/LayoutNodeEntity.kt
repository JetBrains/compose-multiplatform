/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize

/**
 * Base class for entities in [LayoutNodeWrapper]. a [LayoutNodeEntity] is
 * a node in a linked list referenced from [EntityList].
 */
internal open class LayoutNodeEntity<T : LayoutNodeEntity<T, M>, M : Modifier>(
    val layoutNodeWrapper: LayoutNodeWrapper,
    val modifier: M
) {
    /**
     * The next element in the list. [next] is the element that is wrapped
     * by this [LayoutNodeEntity].
     */
    var next: T? = null

    /**
     * Convenience access to [LayoutNode]
     */
    val layoutNode: LayoutNode
        get() = layoutNodeWrapper.layoutNode

    /**
     * Convenience access to [LayoutNodeWrapper.size]
     */
    val size: IntSize
        get() = layoutNodeWrapper.size

    /**
     * `true` only when the entity is attached to the hierarchy.
     */
    var isAttached = false
        private set

    /**
     * Called when the entity is attached to the layout hierarchy.
     */
    open fun onAttach() {
        isAttached = true
    }

    /**
     * Called when the entity has been detached from the layout hierarchy.
     */
    open fun onDetach() {
        isAttached = false
    }
}
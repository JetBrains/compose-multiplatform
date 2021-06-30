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

package androidx.compose.ui.focus

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.ModifiedFocusNode
import androidx.compose.ui.util.fastForEach

internal const val FocusTag = "Compose Focus"

// TODO(b/152051577): Measure the performance of findFocusableChildren().
//  Consider caching the children.
internal fun LayoutNode.findFocusableChildren(focusableChildren: MutableList<ModifiedFocusNode>) {
    // TODO(b/152529395): Write a test for LayoutNode.focusableChildren(). We were calling the wrong
    //  function on [LayoutNodeWrapper] but no test caught this.
    outerLayoutNodeWrapper.findNextFocusWrapper()?.let { focusableChildren.add(it) }
        ?: children.fastForEach { it.findFocusableChildren(focusableChildren) }
}

// TODO(b/144126759): For now we always return the first focusable child. We might want to
//  provide some API that allows the developers flexibility to specify which focusable
//  child they need, or provide a priority among children.
/**
 * Searches (Breadth-first) through all the children and returns the first focus wrapper found.
 *
 * @param queue a mutable list used as a queue for breadth-first search.
 */
internal fun LayoutNode.searchChildrenForFocusNode(
    queue: MutableVector<LayoutNode> = mutableVectorOf()
): ModifiedFocusNode? {
    // Check if any child has a focus Wrapper.
    _children.forEach { layoutNode ->
        val focusNode = layoutNode.outerLayoutNodeWrapper.findNextFocusWrapper()
        if (focusNode != null) {
            return focusNode
        } else {
            queue.add(layoutNode)
        }
    }

    // Perform a breadth-first search through the children.
    while (queue.isNotEmpty()) {
        val focusNode = queue.removeAt(0).searchChildrenForFocusNode(queue)
        if (focusNode != null) {
            return focusNode
        }
    }

    return null
}
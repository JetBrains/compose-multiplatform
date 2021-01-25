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

package androidx.compose.ui.inspection.compose

import androidx.compose.ui.inspection.util.ThreadUtils
import androidx.compose.ui.tooling.inspector.InspectorNode
import androidx.compose.ui.tooling.inspector.LayoutInspectorTree
import androidx.compose.ui.tooling.inspector.NodeParameter
import androidx.compose.ui.tooling.inspector.RawParameter

/**
 * Convert a node's [RawParameter]s into [NodeParameter]s.
 *
 * This method can take a long time, especially the first time, and should be called off the main
 * thread.
 */
fun InspectorNode.convertParameters(): List<NodeParameter> {
    ThreadUtils.assertOffMainThread()
    return LayoutInspectorTree().convertParameters(this)
}

/**
 * Flatten an inspector node into a list containing itself and all its children.
 */
fun InspectorNode.flatten(): Sequence<InspectorNode> {
    ThreadUtils.assertOnMainThread()

    val remaining = mutableListOf(this)
    return generateSequence {
        val next = remaining.removeLastOrNull()
        if (next != null) {
            remaining.addAll(next.children)
        }
        next
    }
}
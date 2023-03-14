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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier

/**
 * A [Modifier.Node] which is able to delegate work to other [Modifier.Node] instances.
 *
 * This can be useful to compose multiple node implementations into one.
 *
 * @sample androidx.compose.ui.samples.DelegatedNodeSample
 *
 * @see DelegatingNode
 */
@ExperimentalComposeUiApi
abstract class DelegatingNode : Modifier.Node() {
    override fun updateCoordinator(coordinator: NodeCoordinator?) {
        super.updateCoordinator(coordinator)
        forEachDelegate {
            it.updateCoordinator(coordinator)
        }
    }

    private var delegate: Modifier.Node? = null

    /**
     * In order to properly delegate work to another [Modifier.Node], the delegated instance must
     * be created and returned inside of a [delegated] call. Doing this will
     * ensure that the created node instance follows all of the right lifecycles and is properly
     * discoverable in this position of the node tree.
     *
     * By using [delegated], the [fn] parameter is executed synchronously, and the result is
     * returned from this function for immediate use.
     *
     * This method can be called from within an `init` block, however the returned delegated node
     * will not be attached until the delegating node is attached. If [delegated] is called after
     * the delegating node is already attached, the returned delegated node will be attached.
     */
    fun <T : Modifier.Node> delegated(fn: () -> T): T {
        val owner = node
        val delegate = fn()
        delegate.setAsDelegateTo(owner)
        if (isAttached) {
            updateCoordinator(owner.coordinator)
            delegate.attach()
        }
        addDelegate(delegate)
        return delegate
    }

    private fun addDelegate(node: Modifier.Node) {
        val tail = delegate
        if (tail != null) {
            node.parent = tail
        }
        delegate = node
    }

    private inline fun forEachDelegate(block: (Modifier.Node) -> Unit) {
        var node: Modifier.Node? = delegate
        while (node != null) {
            block(node)
            node = node.parent
        }
    }

    override fun attach() {
        super.attach()
        forEachDelegate {
            it.updateCoordinator(coordinator)
            it.attach()
        }
    }

    override fun detach() {
        forEachDelegate { it.detach() }
        super.detach()
    }

    override fun reset() {
        super.reset()
        forEachDelegate { it.reset() }
    }
}

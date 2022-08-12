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

@ExperimentalComposeUiApi
abstract class DelegatingNode : Modifier.Node() {
    override fun updateCoordinator(coordinator: NodeCoordinator?) {
        super.updateCoordinator(coordinator)
        forEachDelegate {
            it.updateCoordinator(coordinator)
        }
    }

    private var delegate: Modifier.Node? = null
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

    fun <T : Modifier.Node> lazyDelegated(fn: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
        delegated(fn)
    }

    private inline fun forEachDelegate(block: (Modifier.Node) -> Unit) {
        var node: Modifier.Node? = delegate
        while (node != null) {
            block(node)
            node = node.parent
        }
    }

    override fun onAttach() {
        super.onAttach()
        forEachDelegate {
            updateCoordinator(coordinator)
            it.attach()
        }
    }

    override fun onDetach() {
        forEachDelegate { it.detach() }
        super.onDetach()
    }
}

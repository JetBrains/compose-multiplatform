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

package androidx.compose.ui.input.nestedscroll

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.DelegatingLayoutNodeWrapper
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNodeWrapper
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope

internal class NestedScrollDelegatingWrapper(
    wrapped: LayoutNodeWrapper,
    nestedScrollModifier: NestedScrollModifier
) : DelegatingLayoutNodeWrapper<NestedScrollModifier>(wrapped, nestedScrollModifier) {

    // reference to the parent connection to properly dispatch or provide to children when detached
    private var parentConnection: NestedScrollConnection? = null
        set(value) {
            modifier.dispatcher.parent = value
            childScrollConnection.parent = value ?: NoOpConnection
            field = value
        }

    private var coroutineScopeEvaluation: () -> CoroutineScope?
        get() = modifier.dispatcher.calculateNestedScrollScope
        set(value) {
            modifier.dispatcher.calculateNestedScrollScope = value
        }

    // save last modifier until the next onModifierChanged() call to understand if we got new
    // connection or a new dispatcher, therefore we need to update self and our children
    private var lastModifier: NestedScrollModifier? = null

    override fun onModifierChanged() {
        super.onModifierChanged()
        childScrollConnection.self = modifier.connection
        modifier.dispatcher.parent = parentConnection
        refreshSelfIfNeeded()
    }

    override var modifier: NestedScrollModifier
        get() = super.modifier
        set(value) {
            lastModifier = super.modifier
            super.modifier = value
        }

    override fun attach() {
        super.attach()
        refreshSelfIfNeeded()
    }

    override fun detach() {
        super.detach()
        refreshChildrenWithParentConnection(parentConnection)
        lastModifier = null
    }

    override fun findPreviousNestedScrollWrapper() = this

    override fun findNextNestedScrollWrapper() = this

    private val childScrollConnection = ParentWrapperNestedScrollConnection(
        parent = parentConnection ?: NoOpConnection,
        self = nestedScrollModifier.connection
    )

    private fun refreshSelfIfNeeded() {
        val localLastModifier = lastModifier
        val modifierChanged = localLastModifier == null ||
            localLastModifier.connection !== modifier.connection ||
            localLastModifier.dispatcher !== modifier.dispatcher
        if (modifierChanged && isAttached) {
            val parent = super.findPreviousNestedScrollWrapper()
            parentConnection = parent?.childScrollConnection
            coroutineScopeEvaluation = parent?.coroutineScopeEvaluation ?: coroutineScopeEvaluation
            refreshChildrenWithParentConnection(childScrollConnection)
            lastModifier = modifier
        }
    }

    /**
     * Supply new parent connection for children. Initially children can do it themselves, but
     * after runtime nestedscroll graph changes parents need to update their children.
     *
     * This is O(n) operation, so call only when parent really changes (connection changes,
     * detach, attach, etc)
     */
    private fun refreshChildrenWithParentConnection(newParent: NestedScrollConnection?) {
        nestedScrollChildrenResult.clear()
        val nextNestedScrollWrapper = wrapped.findNextNestedScrollWrapper()
        if (nextNestedScrollWrapper != null) {
            nestedScrollChildrenResult.add(nextNestedScrollWrapper)
        } else {
            loopChildrenForNestedScroll(layoutNode._children)
        }
        // we have done DFS collection, the 0 index is the outer child
        val outerChild =
            if (nestedScrollChildrenResult.isNotEmpty()) nestedScrollChildrenResult[0] else null
        nestedScrollChildrenResult.forEach {
            it.parentConnection = newParent
            it.coroutineScopeEvaluation =
                if (newParent != null) {
                    // if new parent exists - take its scope
                    { this.coroutineScopeEvaluation.invoke() }
                } else {
                    {
                        // if no parent above - take most outer child's origin scope
                        outerChild?.modifier?.dispatcher?.originNestedScrollScope
                    }
                }
        }
    }

    private fun loopChildrenForNestedScroll(children: MutableVector<LayoutNode>) {
        children.forEach { child ->
            val nestedScrollChild =
                child.outerLayoutNodeWrapper.findNextNestedScrollWrapper()
            if (nestedScrollChild != null) {
                nestedScrollChildrenResult.add(nestedScrollChild)
            } else {
                loopChildrenForNestedScroll(child._children)
            }
        }
    }

    // do not use directly, this is only for optimization.
    // Populated and returned by findNestedScrollChildren.
    private val nestedScrollChildrenResult = MutableVector<NestedScrollDelegatingWrapper>()
}

/**
 * Parent-child binding contract. This wrapper guarantees pre-scroll/scroll/pre-fling/fling call
 * order in the nested scroll chain.
 */
private class ParentWrapperNestedScrollConnection(
    var parent: NestedScrollConnection,
    var self: NestedScrollConnection
) : NestedScrollConnection {

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val parentPreConsumed = parent.onPreScroll(available, source)
        val selfPreConsumed = self.onPreScroll(available - parentPreConsumed, source)
        return parentPreConsumed + selfPreConsumed
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val selfConsumed = self.onPostScroll(consumed, available, source)
        val parentConsumed =
            parent.onPostScroll(consumed + selfConsumed, available - selfConsumed, source)
        return selfConsumed + parentConsumed
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val parentPreConsumed = parent.onPreFling(available)
        val selfPreConsumed = self.onPreFling(available - parentPreConsumed)
        return parentPreConsumed + selfPreConsumed
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val selfConsumed = self.onPostFling(consumed, available)
        val parentConsumed = parent.onPostFling(consumed + selfConsumed, available - selfConsumed)
        return selfConsumed + parentConsumed
    }
}

/**
 * No-op parent that consumed nothing. Should be gone by b/174348612
 */
private val NoOpConnection: NestedScrollConnection = object : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
        Offset.Zero

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset =
        Offset.Zero

    override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero

    override suspend fun onPostFling(consumed: Velocity, available: Velocity) = Velocity.Zero
}
/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal.runtime

import androidx.compose.runtime.AbstractApplier
import kotlinx.browser.dom.Node
import kotlinx.browser.dom.get

@ComposeWebInternalApi
class DomApplier(
    root: DomNodeWrapper
) : AbstractApplier<DomNodeWrapper>(root) {

    override fun insertTopDown(index: Int, instance: DomNodeWrapper) {
        // ignored. Building tree bottom-up
    }

    override fun insertBottomUp(index: Int, instance: DomNodeWrapper) {
        current.insert(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        current.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.move(from, to, count)
    }

    override fun onClear() {
        root.clear()
    }
}

@ComposeWebInternalApi
open class DomNodeWrapper(open val node: Node) {

    private var adoptedChildren: MutableList<DomNodeWrapper>? = null
    protected open val logicalChildren: MutableList<DomNodeWrapper>? get() = adoptedChildren
    protected open val childrenParent: Node get() = node
    protected open val childrenEnd: Node? get() = null

    open val isRange: Boolean get() = this is DomRangeNodeWrapper
    open fun domNodes(): List<Node> = listOf(node)
    open fun detach() { node.parentNode?.removeChild(node) }

    fun initializeLogicalChildren(children: List<DomNodeWrapper>) {
        if (children.any { it.isRange }) adoptedChildren = children.toMutableList()
    }

    open fun clear() {
        logicalChildren?.forEach { it.detach() }
        logicalChildren?.clear()
        while (node.firstChild != null) node.removeChild(node.firstChild!!)
    }

    open fun insert(index: Int, nodeWrapper: DomNodeWrapper) {
        if (logicalChildren == null && nodeWrapper.isRange) {
            adoptedChildren = MutableList(node.childNodes.length) { DomNodeWrapper(node.childNodes[it]!!) }
        }
        val children = logicalChildren
        if (children != null) {
            val anchor = children.getOrNull(index)?.domNodes()?.firstOrNull() ?: childrenEnd
            val parent = childrenParent
            nodeWrapper.domNodes().forEach { parent.insertBefore(it, anchor) }
            children.add(index, nodeWrapper)
            return
        }
        val length = node.childNodes.length
        if (index < length) {
            node.insertBefore(nodeWrapper.node, node.childNodes[index]!!)
        } else {
            node.appendChild(nodeWrapper.node)
        }
    }

    open fun remove(index: Int, count: Int) {
        logicalChildren?.let { children ->
            repeat(count) { children.removeAt(index).detach() }
            return
        }
        repeat(count) {
            node.removeChild(node.childNodes[index]!!)
        }
    }

    open fun move(from: Int, to: Int, count: Int) {
        logicalChildren?.let { children ->
            if (from == to || count == 0) return
            val moved = children.subList(from, from + count).toList()
            repeat(count) { children.removeAt(from) }
            val destination = if (from < to) to - count else to
            val anchor = children.getOrNull(destination)?.domNodes()?.firstOrNull() ?: childrenEnd
            val parent = childrenParent
            moved.forEach { child -> child.domNodes().forEach { parent.insertBefore(it, anchor) } }
            children.addAll(destination, moved)
            return
        }
        if (from == to) {
            return // nothing to do
        }

        for (i in 0 until count) {
            // if "from" is after "to," the from index moves because we're inserting before it
            val fromIndex = if (from > to) from + i else from
            val toIndex = if (from > to) to + i else to - 1

            val child = node.removeChild(node.childNodes[fromIndex]!!)
            node.insertBefore(child, node.childNodes[toIndex])
        }
    }
}

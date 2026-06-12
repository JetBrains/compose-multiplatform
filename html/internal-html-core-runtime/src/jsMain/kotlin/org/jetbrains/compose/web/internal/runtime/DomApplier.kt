package org.jetbrains.compose.web.internal.runtime

import androidx.compose.runtime.AbstractApplier
import kotlinx.dom.clear
import org.w3c.dom.*
import org.w3c.dom.css.CSSStyleDeclaration
import org.w3c.dom.events.EventListener

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
        // or current.node.clear()?; in all examples it calls 'clear' on the root
        root.clear()
    }
}


@ComposeWebInternalApi
interface NamedEventListener : EventListener {
    val name: String
}

@ComposeWebInternalApi
open class DomNodeWrapper(open val node: Node) {
    private val knownNodes: MutableList<DomNodeWrapper> = mutableListOf<DomNodeWrapper>()

    fun insert(index: Int, nodeWrapper: DomNodeWrapper) {
        val length = knownNodes.size
        if (index < length) {
            val nodeOnIndex = knownNodes[index]
            knownNodes.add(index, nodeWrapper)
            node.insertBefore(nodeWrapper.node, nodeOnIndex.node)
        } else {
            knownNodes.add(nodeWrapper)

            node.appendChild(nodeWrapper.node)
        }
    }

    fun remove(index: Int, count: Int) {
        repeat(count) {
            val nodeToRemove = knownNodes.removeAt(index)
            node.removeChild(nodeToRemove.node)
        }
    }

    fun move(from: Int, to: Int, count: Int) {
        if (from == to) {
            return // nothing to do
        }

        for (i in 0 until count) {
            // if "from" is after "to," the from index moves because we're inserting before it
            val fromIndex = if (from > to) from + i else from
            val toIndex = if (from > to) to + i else to + count - 2

            val nodeOnIndex = knownNodes[toIndex]

            val nodeToMove = knownNodes.removeAt(fromIndex)
            node.removeChild(nodeToMove.node)

            knownNodes.add(toIndex, nodeToMove)
            node.insertBefore(nodeToMove.node, nodeOnIndex.node)
        }
    }

    internal fun clear() {
        knownNodes.forEach {
            node.removeChild(it.node)
        }
        knownNodes.clear()
    }
}

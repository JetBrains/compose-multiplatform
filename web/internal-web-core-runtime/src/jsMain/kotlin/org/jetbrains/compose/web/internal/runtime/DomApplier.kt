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
        root.node.clear()
    }
}


@ComposeWebInternalApi
open class DomNodeWrapper(open val node: Node) {

    @ComposeWebInternalApi
    interface NamedEventListener : EventListener {
        val name: String
    }

    private var currentListeners = emptyList<NamedEventListener>()

    fun updateEventListeners(list: List<NamedEventListener>) {
        val htmlElement = node as? HTMLElement ?: return

        currentListeners.forEach {
            htmlElement.removeEventListener(it.name, it)
        }

        currentListeners = list

        currentListeners.forEach {
            htmlElement.addEventListener(it.name, it)
        }
    }

    fun insert(index: Int, nodeWrapper: DomNodeWrapper) {
        val length = node.childNodes.length
        if (index < length) {
            node.insertBefore(nodeWrapper.node, node.childNodes[index]!!)
        } else {
            node.appendChild(nodeWrapper.node)
        }
    }

    fun remove(index: Int, count: Int) {
        repeat(count) {
            node.removeChild(node.childNodes[index]!!)
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

            val child = node.removeChild(node.childNodes[fromIndex]!!)
            node.insertBefore(child, node.childNodes[toIndex]!!)
        }
    }
}

@ComposeWebInternalApi
class DomElementWrapper(override val node: HTMLElement): DomNodeWrapper(node) {
    private var currentAttrs: Map<String, String>? = null

    fun updateAttrs(attrs: Map<String, String>) {
        currentAttrs?.forEach {
            node.removeAttribute(it.key)
        }

        attrs.forEach {
            node.setAttribute(it.key, it.value)
        }
        currentAttrs = attrs
    }

    fun updateProperties(list: List<Pair<(Element, Any) -> Unit, Any>>) {
        if (node.className.isNotEmpty()) node.className = ""

        list.forEach {
            it.first(node, it.second)
        }
    }

    @ComposeWebInternalApi
    fun interface StyleDeclarationsApplier {
        @ComposeWebInternalApi
        fun applyToNodeStyle(nodeStyle: CSSStyleDeclaration)
    }

    fun updateStyleDeclarations(styleApplier: StyleDeclarationsApplier) {
        node.removeAttribute("style")
        styleApplier.applyToNodeStyle(node.style)
    }
}

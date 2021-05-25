package androidx.compose.web

import androidx.compose.runtime.AbstractApplier
import androidx.compose.web.attributes.WrappedEventListener
import androidx.compose.web.css.StyleHolder
import androidx.compose.web.css.attributeStyleMap
import androidx.compose.web.elements.setProperty
import androidx.compose.web.elements.setVariable
import kotlinx.browser.document
import kotlinx.dom.clear
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.get

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

class DomNodeWrapper(val node: Node) {

    constructor(tag: String) : this(document.createElement(tag))

    private var currentListeners: List<WrappedEventListener<*>> = emptyList()
    private var currentAttrs: Map<String, String?> = emptyMap()

    fun updateProperties(list: List<Pair<(HTMLElement, Any) -> Unit, Any>>) {
        val htmlElement = node as? HTMLElement ?: return

        if (node.className.isNotEmpty()) node.className = ""

        list.forEach { it.first(htmlElement, it.second) }
    }

    fun updateEventListeners(list: List<WrappedEventListener<*>>) {
        val htmlElement = node as? HTMLElement ?: return

        currentListeners.forEach {
            htmlElement.removeEventListener(it.event, it)
        }

        currentListeners = list

        currentListeners.forEach {
            htmlElement.addEventListener(it.event, it)
        }
    }

    fun updateAttrs(attrs: Map<String, String?>) {
        val htmlElement = node as? HTMLElement ?: return
        currentAttrs.forEach {
            htmlElement.removeAttribute(it.key)
        }
        currentAttrs = attrs
        currentAttrs.forEach {
            if (it.value != null) htmlElement.setAttribute(it.key, it.value ?: "")
        }
    }

    fun updateStyleDeclarations(style: StyleHolder?) {
        val htmlElement = node as? HTMLElement ?: return
        // TODO: typed-om-polyfill hasn't StylePropertyMap::clear()
        htmlElement.style.cssText = ""

        style?.properties?.forEach { (name, value) ->
            setProperty(htmlElement.attributeStyleMap, name, value)
        }
        style?.variables?.forEach { (name, value) ->
            setVariable(htmlElement.style, name, value)
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

    companion object {

        val UpdateAttrs: DomNodeWrapper.(Map<String, String?>) -> Unit = {
            this.updateAttrs(it)
        }
        val UpdateListeners: DomNodeWrapper.(List<WrappedEventListener<*>>) -> Unit = {
            this.updateEventListeners(it)
        }
        val UpdateProperties: DomNodePropertiesUpdater = {
            this.updateProperties(it)
        }
        val UpdateStyleDeclarations: DomNodeWrapper.(StyleHolder?) -> Unit = {
            this.updateStyleDeclarations(it)
        }
    }
}

typealias DomNodePropertiesUpdater =
    DomNodeWrapper.(List<Pair<(HTMLElement, Any) -> Unit, Any>>) -> Unit
package org.jetbrains.compose.resources.vector.xmldom

import org.w3c.dom.Node as DomNode
import org.w3c.dom.Element as DomElement

internal open class NodeImpl(val n: DomNode): Node {
    override val nodeName: String
        get() = n.nodeName
    override val localName: String
        get() = n.localName
    override val childNodes: NodeList
        get() =
            object: NodeList {
                override fun item(i: Int): Node {
                    val child = n.childNodes.item(i)
                    return if (child is DomElement) ElementImpl(child) else NodeImpl(child)
                }

                override val length: Int = n.childNodes.length

            }

    override val namespaceURI: String
        get() = n.namespaceURI

    override fun lookupPrefix(namespaceURI: String): String = n.lookupPrefix(namespaceURI)

}
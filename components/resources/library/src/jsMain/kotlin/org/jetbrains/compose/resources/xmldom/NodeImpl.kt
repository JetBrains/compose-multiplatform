package org.jetbrains.compose.resources.vector.xmldom

import org.w3c.dom.Node as DomNode
import org.w3c.dom.Element as DomElement

internal open class NodeImpl(val n: DomNode): Node {
    override val nodeName: String
        get() = n.nodeName

    override val localName = "" /* localName is not a Node property, only applies to Elements and Attrs */

    override val namespaceURI = "" /* localName is not a Node property, only applies to Elements and Attrs */

    override val childNodes: NodeList
        get() =
            object: NodeList {
                override fun item(i: Int): Node {
                    val child = n.childNodes.item(i)
                        ?: throw IndexOutOfBoundsException("no child node accessible at index=$i")
                    return if (child is DomElement) ElementImpl(child) else NodeImpl(child)
                }

                override val length: Int = n.childNodes.length

            }

    override fun lookupPrefix(namespaceURI: String): String = n.lookupPrefix(namespaceURI) ?: ""

}

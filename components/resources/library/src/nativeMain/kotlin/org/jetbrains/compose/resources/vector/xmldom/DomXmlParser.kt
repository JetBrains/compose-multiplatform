/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.compose.resources.vector.xmldom

import kotlinx.cinterop.ObjCSignatureOverride
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSXMLParser
import platform.Foundation.NSXMLParserDelegateProtocol
import platform.Foundation.dataUsingEncoding
import platform.darwin.NSObject

internal fun parse(xml: String): Element {
    val parser = DomXmlParser()
    @Suppress("CAST_NEVER_SUCCEEDS")
    NSXMLParser((xml as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!).apply {
        shouldReportNamespacePrefixes = true
        shouldProcessNamespaces = true
        delegate = parser
    }.parse()
    return parser.root!!
}

private class ElementImpl(
    override val localName: String,
    override val nodeName: String,
    override val namespaceURI: String,
    val prefixMap: Map<String, String>,
    val attributes: Map<Any?, *>
) : Element {

    override var textContent: String? = null

    override val childNodes: NodeList
        get() = object : NodeList {
            override fun item(i: Int): Node {
                return childs[i]
            }

            override val length: Int
                get() = childs.size
        }


    var childs = mutableListOf<Node>()

    override fun getAttributeNS(nameSpaceURI: String, localName: String): String {
        val prefix = prefixMap[nameSpaceURI]
        val attrKey = if (prefix == null) localName else "$prefix:$localName"
        return getAttribute(attrKey)
    }

    override fun getAttribute(name: String): String = attributes[name] as String? ?: ""

    override fun lookupPrefix(namespaceURI: String): String = prefixMap[namespaceURI] ?: ""
}

@Suppress("CONFLICTING_OVERLOADS", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
private class DomXmlParser : NSObject(), NSXMLParserDelegateProtocol {

    val curPrefixMapInverted = mutableMapOf<String, String>()

    var curPrefixMap: Map<String, String> = emptyMap()

    val nodeStack = mutableListOf<ElementImpl>()

    var root: Element? = null

    override fun parser(
        parser: NSXMLParser,
        didStartElement: String,
        namespaceURI: String?,
        qualifiedName: String?,
        attributes: Map<Any?, *>
    ) {
        val node = ElementImpl(
            didStartElement,
            qualifiedName!!,
            namespaceURI ?: "",
            curPrefixMap,
            attributes
        )

        if (root == null) root = node

        if (!nodeStack.isEmpty())
            nodeStack.last().childs.add(node)

        nodeStack.add(node)
    }

    @ObjCSignatureOverride
    override fun parser(parser: NSXMLParser, foundCharacters: String) {
        nodeStack.lastOrNull()?.let { node ->
            node.textContent = node.textContent.orEmpty() + foundCharacters
        }
    }

    override fun parser(
        parser: NSXMLParser,
        didEndElement: String,
        namespaceURI: String?,
        qualifiedName: String?
    ) {
        val node = nodeStack.removeLast()
        assert(node.localName.equals(didEndElement))
    }

    override fun parser(
        parser: NSXMLParser,
        didStartMappingPrefix: String,
        toURI: String
    ) {
        curPrefixMapInverted.put(didStartMappingPrefix, toURI)
        curPrefixMap = curPrefixMapInverted.entries.associateBy({ it.value }, { it.key })
    }

    @ObjCSignatureOverride
    override fun parser(parser: NSXMLParser, didEndMappingPrefix: String) {
        curPrefixMapInverted.remove(didEndMappingPrefix)
        curPrefixMap = curPrefixMapInverted.entries.associateBy({ it.value }, { it.key })
    }

    @ObjCSignatureOverride
    override fun parser(parser: NSXMLParser, validationErrorOccurred: NSError) {
        throw MalformedXMLException("validation error occurred")
    }

    @ObjCSignatureOverride
    override fun parser(parser: NSXMLParser, parseErrorOccurred: NSError) {
        throw MalformedXMLException("parse error occurred")
    }
}


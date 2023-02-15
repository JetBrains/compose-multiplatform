package org.jetbrains.compose.resources.vector.xmldom

/**
 * XML DOM Node list.
 */
internal interface NodeList {
    fun item(i: Int): Node

    val length: Int
}
package org.jetbrains.compose.resources.vector.xmldom

/**
 * XML DOM Node list.
 */
interface NodeList {
    fun item(i: Int): Node

    val length: Int
}
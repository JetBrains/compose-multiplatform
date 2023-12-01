package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.compose.resources.vector.xmldom.ElementImpl
import org.jetbrains.compose.resources.vector.xmldom.MalformedXMLException
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.parsing.DOMParser

internal actual fun parseXML(byteArray: ByteArray): Element {
    val xmlString = byteArray.decodeToString()
    val xmlDom = DOMParser().parseFromString(xmlString, "application/xml")
    val domElement = xmlDom.documentElement ?: throw MalformedXMLException("missing documentElement")
    return ElementImpl(domElement)
}

internal actual fun ArrayBuffer.toByteArray(): ByteArray =
    Int8Array(this, 0, byteLength).unsafeCast<ByteArray>()
package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.compose.resources.vector.xmldom.ElementImpl
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

internal actual fun ByteArray.toXmlElement(): Element = ElementImpl(
    DocumentBuilderFactory.newInstance()
        .apply { isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(InputSource(ByteArrayInputStream(this)))
        .documentElement
)
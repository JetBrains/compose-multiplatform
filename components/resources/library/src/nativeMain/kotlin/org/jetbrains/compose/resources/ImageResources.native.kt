package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.compose.resources.vector.xmldom.parse

internal actual fun ByteArray.toXmlElement(): Element = parse(decodeToString())

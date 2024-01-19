package org.jetbrains.compose.resources.vector.xmldom

import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Error throw when parsed XML is malformed
 */
@ExperimentalResourceApi
class MalformedXMLException(message: String?) : Exception(message)

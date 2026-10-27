/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Access shared serialization helpers without exposing them as public API.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element

internal class StringElementBuilder<TElement : Element>(
    override val tagName: String,
    override val namespace: String = HtmlNamespace,
) : ElementBuilder<TElement>, HtmlTagMetadata {
    override val serializationFlags = htmlStringTagFlags(tagName, namespace)

    override fun create(): TElement {
        throw UnsupportedOperationException(
            "String element builder for <$tagName> cannot create a DOM element"
        )
    }
}

internal interface HtmlTagMetadata {
    val namespace: String
    val serializationFlags: Int
}

internal const val RawTextTagFlag = 1
internal const val VoidTagFlag = 2
internal const val LeadingNewlineTagFlag = 4
internal const val ValidTagFlag = 8

internal fun htmlStringTagFlags(tagName: String, namespace: String): Int =
    (if (isHtmlRawTextElement(tagName, namespace)) RawTextTagFlag else 0) or
    (if (isHtmlVoidElement(tagName, namespace)) VoidTagFlag else 0) or
    (if (isHtmlLeadingNewlineElement(tagName, namespace)) LeadingNewlineTagFlag else 0) or
    (if (isValidHtmlTagName(tagName)) ValidTagFlag else 0)

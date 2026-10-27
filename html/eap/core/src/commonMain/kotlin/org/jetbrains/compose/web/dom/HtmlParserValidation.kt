/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Access shared serialization helpers without exposing them as public API.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.compose.web.dom

/** Rejects implied table wrappers and foster parenting before HTML leaves the string renderer. */
internal fun requireHtmlParserStableTableChild(parentTagName: String?, child: StringHtmlNode) {
    when (child) {
        is StringHtmlElementNode -> requireHtmlParserStableTableElement(parentTagName, child.tagName)
        is StringHtmlTextNode -> {
            requireHtmlParserStableTableText(parentTagName, child.text)
        }
        is StringHtmlRawTextNode -> {
            requireHtmlParserStableTableText(parentTagName, child.content.text)
        }
    }
}

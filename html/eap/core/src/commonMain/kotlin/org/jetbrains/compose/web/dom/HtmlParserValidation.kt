/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

private val HtmlTableChildNames = mapOf(
    "table" to setOf("caption", "colgroup", "thead", "tbody", "tfoot", "script", "style", "template"),
    "thead" to setOf("tr", "script", "style", "template"),
    "tbody" to setOf("tr", "script", "style", "template"),
    "tfoot" to setOf("tr", "script", "style", "template"),
    "tr" to setOf("td", "th", "script", "style", "template"),
    "colgroup" to setOf("col", "template"),
)

/** Rejects implied table wrappers and foster parenting before HTML leaves the string renderer. */
internal fun requireHtmlParserStableTableChild(parentTagName: String?, child: StringHtmlNode) {
    val allowedChildren = HtmlTableChildNames[parentTagName] ?: return
    val childTagName = when (child) {
        is StringHtmlElementNode -> child.tagName
        is StringHtmlTextNode -> {
            requireHtmlParserStableTableText(parentTagName, child.text)
            return
        }
        is StringHtmlRawTextNode -> {
            requireHtmlParserStableTableText(parentTagName, child.content.text)
            return
        }
    }
    if (childTagName in allowedChildren) return

    val suggestion = when {
        parentTagName == "table" && childTagName == "tr" -> "wrap rows in Tbody { }"
        parentTagName == "table" && (childTagName == "td" || childTagName == "th") ->
            "wrap rows in Tbody { } and cells in Tr { }"
        parentTagName == "table" && childTagName == "col" -> "wrap columns in Colgroup { }"
        parentTagName in setOf("tbody", "thead", "tfoot") &&
            (childTagName == "td" || childTagName == "th") -> "wrap cells in Tr { }"
        else -> "use explicit table sections, rows and cells, or move this content outside the table"
    }
    throw IllegalArgumentException(
        "Element <$childTagName> cannot be serialized directly inside <$parentTagName>; " +
            "the HTML parser would insert wrappers, move or discard content. $suggestion",
    )
}

private fun requireHtmlParserStableTableText(parentTagName: String?, text: String) {
    require(text.all { it in AsciiWhitespaceCharacters }) {
        "Non-whitespace text cannot be serialized directly inside <$parentTagName>; " +
            "the HTML parser would move it outside the table. " +
            "Wrap text in Td { } or Th { } inside Tr { }, or move it outside the table"
    }
}

/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.css.*

fun CSSRuleDeclaration.stringPresentation(
    baseIndent: String = "",
    indent: String = "    ",
    delimiter: String = "\n"
): String {
    return buildString { appendRulePresentation(this@stringPresentation, baseIndent, indent, delimiter) }
}

internal fun StringBuilder.appendRulePresentation(
    rule: CSSRuleDeclaration,
    baseIndent: String,
    indent: String,
    delimiter: String,
) {
    append(baseIndent).append(rule.header).append(" {")
    when (rule) {
        is CSSStyledRuleDeclaration -> {
            for ((name, value, important) in rule.style.properties) {
                append(delimiter).append(baseIndent).append(indent).append(name).append(": ").append(value)
                if (important) append(" !important")
                append(';')
            }
            for ((name, value) in rule.style.variables) {
                append(delimiter).append(baseIndent).append(indent).append("--").append(name)
                    .append(": ").append(value).append(';')
            }
        }
        is CSSGroupingRuleDeclaration -> {
            for (child in rule.rules) {
                append(delimiter)
                appendRulePresentation(child, baseIndent + indent, indent, delimiter)
            }
        }
        is CSSKeyframesRuleDeclaration -> {
            for (child in rule.keys) {
                append(delimiter)
                appendRulePresentation(child, baseIndent + indent, indent, delimiter)
            }
        }
    }
    append(delimiter).append(baseIndent).append('}')
}

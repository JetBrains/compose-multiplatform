@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.css.*

fun CSSRuleDeclaration.stringPresentation(
    baseIndent: String = "",
    indent: String = "    ",
    delimiter: String = "\n"
): String {
    val cssRuleDeclaration = this
    val strings = mutableListOf<String>()
    strings.add("$baseIndent${cssRuleDeclaration.header} {")
    when (cssRuleDeclaration) {
        is CSSStyledRuleDeclaration -> {
            cssRuleDeclaration.style.properties.forEach { (name, value, important) ->
                strings.add("$baseIndent$indent$name: $value${if (important) " !important" else ""};")
            }
            cssRuleDeclaration.style.variables.forEach { (name, value) ->
                strings.add("$baseIndent$indent--$name: $value;")
            }
        }
        is CSSGroupingRuleDeclaration -> {
            cssRuleDeclaration.rules.forEach { childRuleDeclaration ->
                strings.add(childRuleDeclaration.stringPresentation(baseIndent + indent, indent, delimiter))
            }
        }
        is CSSKeyframesRuleDeclaration -> {
            cssRuleDeclaration.keys.forEach { childRuleDeclaration ->
                strings.add(childRuleDeclaration.stringPresentation(baseIndent + indent, indent, delimiter))
            }
        }
    }
    strings.add("$baseIndent}")
    return strings.joinToString(delimiter)
}

/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.html.compiler.APP_NAME as applicationName
import org.jetbrains.compose.html.compiler.CARD_CLASS
import org.jetbrains.compose.html.compiler.COUNT
import org.jetbrains.compose.html.compiler.Tokens.SUFFIX

private const val SAME_FILE = "same"

fun renderCrossFileConstants() = composeHtmlToString {
    Div(attrs = {
        classes(CARD_CLASS)
        id(org.jetbrains.compose.html.compiler.Theme.ID)
        attr(org.jetbrains.compose.html.compiler.ATTRIBUTE, SAME_FILE + SUFFIX)
    }) {
        Span { Text(applicationName) }
    }
}

fun renderConstantExpressions() = composeHtmlToString {
    val count = COUNT
    val twice = count * 2
    val title = "items=$twice"
    Div(attrs = {
        val name = "ti" + "tle"
        attr(name, title)
    }) {
        val label = "value=" + (twice + 1)
        Span { Text(label) }
    }
}

fun renderPrimitiveExpressions() = composeHtmlToString {
    val max = Int.MAX_VALUE
    val yes = COUNT > 2 && COUNT != 0
    val long = 40L + 2L
    val character = 'A'
    val nothing: String? = null
    Span(attrs = { attr("title", "${max + 1}:$yes:$long:$character:$nothing") }) {
        Text("${-COUNT / 2}:${7 % COUNT}:${1 shl 3}")
    }
}

private fun unreachableEffect(): Boolean = error("Unreachable")

fun renderShortCircuitConstants() = composeHtmlToString {
    val no = false
    val yes = true
    val and = no && unreachableEffect()
    val or = yes || unreachableEffect()
    Span { Text("$and:$or") }
}

var constantRuntimeCalls = 0

val runtimeLabel: String
    get() = "getter-${++constantRuntimeCalls}"

fun renderRuntimeGetters() = composeHtmlToString {
    Span(attrs = { attr("title", runtimeLabel) }) { Text(runtimeLabel) }
}

fun renderEffectfulAlias() = composeHtmlToString {
    val label = "effect-${++constantRuntimeCalls}"
    Span { Text(label) }
}

fun renderMutableAlias() = composeHtmlToString {
    var label = "first"
    label = "changed"
    Span { Text(label) }
}

private object UserLabel {
    override fun toString() = "user"
}

fun renderUserToString() = composeHtmlToString {
    Span { Text("$UserLabel") }
}

fun renderThrowingExpression() = composeHtmlToString {
    val zero = 0
    Span { Text("${1 / zero}") }
}

private val ordinaryLabel = "ordinary"

fun renderOrdinaryVal() = composeHtmlToString {
    Span { Text(ordinaryLabel) }
}

fun renderShadowedConstant(CARD_CLASS: String) = composeHtmlToString {
    Span(attrs = { classes(CARD_CLASS) }) { Text(CARD_CLASS) }
}

fun renderAfterConstantDepthLimit() = composeHtmlToString {
    val value = "depth"
    // The 64 nested operations reach value just before the depth limit.
    Span(attrs = { id("deep") }) {
        Text(
            ("" + ("" + ("" + ("" + ("" + ("" + ("" + ("" +
            ("" + ("" + ("" + ("" + ("" + ("" + ("" + ("" +
            ("" + ("" + ("" + ("" + ("" + ("" + ("" + ("" +
            ("" + ("" + ("" + ("" + ("" + ("" + ("" + ("" +
            ("" + ("" + ("" + ("" + ("" + ("" + ("" + ("" +
            ("" + ("" + ("" + ("" + ("" + ("" + ("" + ("" +
            ("" + ("" + ("" + ("" + ("" + ("" + ("" + ("" +
            ("" + ("" + ("" + ("" + ("" + ("" + ("" + ("" +
            value
            ))))))))
            ))))))))
            ))))))))
            ))))))))
            ))))))))
            ))))))))
            ))))))))
            ))))))))
        )
    }
    Span { Text(value) }
}

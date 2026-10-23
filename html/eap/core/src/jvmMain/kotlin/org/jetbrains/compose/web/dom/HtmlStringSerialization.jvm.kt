/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.internal.unsafeCast

internal actual fun canAppendUnescapedHtmlString(value: String, attribute: Boolean, startIndex: Int): Boolean {
    if (value.length - startIndex < 32) return false
    // Use JVM String searches to skip Kotlin's search wrappers.
    val text = value.unsafeCast<java.lang.String>()
    return text.indexOf('&'.code, startIndex) < 0 && text.indexOf('<'.code, startIndex) < 0 && text.indexOf('>'.code, startIndex) < 0 &&
        text.indexOf('\r'.code, startIndex) < 0 && text.indexOf('\u0000'.code, startIndex) < 0 &&
        (!attribute || (text.indexOf('"'.code, startIndex) < 0 && text.indexOf('\n'.code, startIndex) < 0 && text.indexOf('\t'.code, startIndex) < 0))
}

internal actual fun StringBuilder.appendHtmlRun(value: String, start: Int, end: Int) {
    // JDK 21 bulk-copies String ranges; avoid constructing a temporary substring.
    append(value, start, end)
}


private object HtmlEscapeTable {
    val flags = ByteArray(Char.MAX_VALUE.code + 1).apply {
        for (character in "\u0000&<>\r") this[character.code] = 3
        for (character in "\"\n\t") this[character.code] = 2
    }
}

internal actual fun StringBuilder.appendEscapedHtmlString(value: String, attribute: Boolean): Boolean {
    if (value.length < HtmlEscapeTableMinLength) return false
    if (canAppendUnescapedHtmlString(value, attribute)) {
        append(value)
        return true
    }
    val flags = HtmlEscapeTable.flags
    val mask = if (attribute) 2 else 1
    var start = 0
    while (true) {
        var index = start
        while (index < value.length && flags[value[index].code].toInt() and mask == 0) index++
        if (index == value.length) {
            appendHtmlRun(value, start, value.length)
            return true
        }
        val character = value[index]
        val replacement = when (character) {
            '\u0000' -> throw IllegalArgumentException(
                if (attribute) "HTML attribute values must not contain NUL (U+0000)"
                else "HTML text must not contain NUL (U+0000)"
            )
            '&' -> "&amp;"
            '<' -> "&lt;"
            '>' -> "&gt;"
            '\r' -> "&#13;"
            '"' -> "&quot;"
            '\n' -> "&#10;"
            else -> "&#9;"
        }
        appendHtmlRun(value, start, index)
        append(replacement)
        start = index + 1
        if (character == '&' && canAppendUnescapedHtmlString(value, attribute, start)) {
            appendHtmlRun(value, start, value.length)
            return true
        }
    }
}

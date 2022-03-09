/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal

internal fun String.uppercaseFirstChar(): String =
    transformFirstCharIfNeeded(
        shouldTransform = { it.isLowerCase() },
        transform = { it.uppercaseChar() }
    )

internal fun String.lowercaseFirstChar(): String =
    transformFirstCharIfNeeded(
        shouldTransform = { it.isUpperCase() },
        transform = { it.lowercaseChar() }
    )

private inline fun String.transformFirstCharIfNeeded(
    shouldTransform: (Char) -> Boolean,
    transform: (Char) -> Char
): String {
    if (isNotEmpty()) {
        val firstChar = this[0]
        if (shouldTransform(firstChar)) {
            val sb = java.lang.StringBuilder(length)
            sb.append(transform(firstChar))
            sb.append(this, 1, length)
            return sb.toString()
        }
    }
    return this
}

internal fun joinLowerCamelCase(vararg parts: String): String =
    parts.withIndex().joinToString(separator = "") { (i, part) ->
        if (i == 0) part.lowercaseFirstChar() else part.uppercaseFirstChar()
    }

internal fun joinUpperCamelCase(vararg parts: String): String =
    parts.joinToString(separator = "") { it.uppercaseFirstChar() }
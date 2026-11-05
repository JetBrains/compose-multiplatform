/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

internal actual fun String.matchesRawTextTagName(start: Int, tagName: String): Boolean {
    // Match tag names without allocating folded substrings.
    // Preserve ECMAScript folding for long s and Kelvin sign.
    for (index in tagName.indices) {
        val character = this[start + index]
        val folded = when (character) {
            in 'A'..'Z' -> character + 32
            '\u017F' -> 's'
            '\u212A' -> 'k'
            else -> character
        }
        if (folded != tagName[index]) return false
    }
    return true
}

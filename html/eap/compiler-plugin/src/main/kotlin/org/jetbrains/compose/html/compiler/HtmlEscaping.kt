/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import org.jetbrains.compose.web.dom.htmlEscapeReplacement

internal fun escapeHtml(value: String, attribute: Boolean): String = buildString {
    for (character in value) {
        val replacement = htmlEscapeReplacement(character, attribute)
        if (replacement != null) append(replacement) else append(character)
    }
}

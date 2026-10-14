/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.internal.unsafeCast

internal actual fun canAppendUnescapedHtmlString(value: String, attribute: Boolean): Boolean {
    if (value.length < 32) return false
    // Use JVM String searches to skip Kotlin's search wrappers.
    val text = value.unsafeCast<java.lang.String>()
    return text.indexOf('&'.code) < 0 && text.indexOf('<'.code) < 0 && text.indexOf('>'.code) < 0 &&
        text.indexOf('\r'.code) < 0 && text.indexOf('\u0000'.code) < 0 &&
        (!attribute || (text.indexOf('"'.code) < 0 && text.indexOf('\n'.code) < 0 && text.indexOf('\t'.code) < 0))
}

internal actual fun StringBuilder.appendHtmlRun(value: String, start: Int, end: Int) {
    // JDK 21 bulk-copies String ranges; avoid constructing a temporary substring.
    append(value, start, end)
}

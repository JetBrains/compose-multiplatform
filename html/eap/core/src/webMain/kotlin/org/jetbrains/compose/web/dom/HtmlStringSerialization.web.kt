/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

internal actual fun canAppendUnescapedHtmlString(value: String, attribute: Boolean, startIndex: Int): Boolean = false

internal actual fun StringBuilder.appendHtmlRun(value: String, start: Int, end: Int) {
    append(value, start, end)
}

internal actual fun StringBuilder.appendEscapedHtmlString(value: String, attribute: Boolean): Boolean = false

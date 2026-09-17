/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web.internal

import kotlinx.browser.dom.Element
import kotlinx.browser.window

internal expect fun Element.noncePropertyOrNull(): String?

internal actual fun scheduleAfterEvent(block: () -> Unit) {
    window.setTimeout({
        block()
        null
    }, 0)
}

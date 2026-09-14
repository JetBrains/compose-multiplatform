/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web

import kotlin.js.js

private fun consoleError(message: String): Unit = js("console.error(message)")

internal actual fun reportHydrationMismatch(mismatch: HydrationMismatchException) {
    consoleError(mismatch.stackTraceToString())
}

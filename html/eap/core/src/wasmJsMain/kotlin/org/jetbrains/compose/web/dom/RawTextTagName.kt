/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

// Wasm's regionMatches performs the comparison in linear time without creating temporary
// lowercase strings; unlike JS Regex, its existing ignore-case behavior is already the contract.
internal actual fun String.matchesRawTextTagName(start: Int, tagName: String): Boolean =
    regionMatches(start, tagName, 0, tagName.length, ignoreCase = true)

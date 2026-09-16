/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

// The scanner shares JVM's allocation-free region comparison while retaining the former
// case-insensitive Regex semantics used by server-side rendering.
internal actual fun String.matchesRawTextTagName(start: Int, tagName: String): Boolean =
    regionMatches(start, tagName, 0, tagName.length, ignoreCase = true)

/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Style

@Composable
@Suppress("NOTHING_TO_INLINE")
inline fun Style(
    styleSheet: CSSRulesHolder
) {
    Style(cssRules = styleSheet.cssRules)
}

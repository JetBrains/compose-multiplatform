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

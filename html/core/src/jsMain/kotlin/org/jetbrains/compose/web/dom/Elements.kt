package org.jetbrains.compose.web.dom

import org.w3c.dom.css.CSSStyleSheet

internal fun CSSStyleSheet.clearCSSRules() {
    repeat(cssRules.length) {
        deleteRule(0)
    }
}

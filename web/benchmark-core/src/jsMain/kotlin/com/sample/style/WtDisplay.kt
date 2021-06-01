package com.sample.style

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.*

object WtDisplay : StyleSheet(AppStylesheet) {
    val wtDisplayNone by style {
        display(DisplayStyle.None)
    }

    val wtDisplayMdBlock by style {
        media(maxWidth(1000.px)) {
            self style {
                display(DisplayStyle.Block)
            }
        }
    }

    val wtDisplayMdNone by style {
        media(maxWidth(1000.px)) {
            self style {
                display(DisplayStyle.None)
            }
        }
    }
}
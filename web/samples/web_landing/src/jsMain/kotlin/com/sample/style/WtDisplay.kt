package com.sample.style

import org.jetbrains.compose.web.css.*

object WtDisplay : StyleSheet(AppStylesheet) {
    val wtDisplayNone by style {
        display(DisplayStyle.None)
    }

    val wtDisplayMdBlock by style {
        media(mediaMaxWidth(1000.px)) {
            self style {
                display(DisplayStyle.Block)
            }
        }
    }

    val wtDisplayMdNone by style {
        media(mediaMaxWidth(1000.px)) {
            self style {
                display(DisplayStyle.None)
            }
        }
    }
}

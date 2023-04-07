package com.sample.style

import org.jetbrains.compose.web.css.*

object WtContainer : StyleSheet(AppStylesheet) {
    val wtContainer by style {
        property("margin-left", "auto")
        property("margin-right", "auto")
        boxSizing("border-box")
        paddingLeft(22.px)
        paddingRight(22.px)
        maxWidth(1276.px)

        media(mediaMaxWidth(640.px)) {
            self style {
                maxWidth(100.percent)
                paddingLeft(16.px)
                paddingRight(16.px)
            }
        }

        media(mediaMaxWidth(1276.px)) {
            self style {
                maxWidth(996.px)
                paddingLeft(22.px)
                paddingRight(22.px)
            }
        }

        media(mediaMaxWidth(1000.px)) {
            self style {
                maxWidth(100.percent)
                paddingLeft(22.px)
                paddingRight(22.px)
            }
        }
    }
}

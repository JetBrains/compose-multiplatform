package com.sample.style

import org.jetbrains.compose.web.css.*

object WtContainer : StyleSheet(AppStylesheet) {
    val wtContainer by style {
        property("margin-left", "auto")
        property("margin-right", "auto")
        property("box-sizing", "border-box")
        property("padding-left", 22.px)
        property("padding-right", 22.px)
        property("max-width", 1276.px)

        media(mediaMaxWidth(640.px)) {
            self style {
                property("max-width", 100.percent)
                property("padding-left", 16.px)
                property("padding-right", 16.px)
            }
        }

        media(mediaMaxWidth(1276.px)) {
            self style {
                property("max-width", 996.px)
                property("padding-left", 22.px)
                property("padding-right", 22.px)
            }
        }

        media(mediaMaxWidth(1000.px)) {
            self style {
                property("max-width", 100.percent)
                property("padding-left", 22.px)
                property("padding-right", 22.px)
            }
        }
    }
}

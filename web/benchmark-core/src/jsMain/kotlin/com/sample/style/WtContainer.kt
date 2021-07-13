package com.sample.style

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.*

object WtContainer : StyleSheet(AppStylesheet) {
    val wtContainer by style {
        property("margin-left", "auto")
        property("margin-right", "auto")
        boxSizing( "border-box")
        paddingLeft(22.px)
        paddingRight(22.px)
        property("max-width", 1276.px)

        media(maxWidth(640.px)) {
            self style {
                property("max-width", 100.percent)
                paddingLeft(16.px)
                paddingRight(16.px)
            }
        }

        media(maxWidth(1276.px)) {
            self style {
                property("max-width", 996.px)
                paddingLeft(996.px)
                paddingRight(22.px)
            }
        }

        media(maxWidth(1000.px)) {
            self style {
                property("max-width", 100.percent)
                paddingLeft(22.px)
                paddingRight(22.px)
            }
        }
    }
}
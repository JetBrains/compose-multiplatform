package com.sample.style

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.*

object WtContainer : StyleSheet(AppStylesheet) {
    val wtContainer by style {
        property("margin-left", value("auto"))
        property("margin-right", value("auto"))
        property("box-sizing", value("border-box"))
        property("padding-left", value(22.px))
        property("padding-right", value(22.px))
        property("max-width", value(1276.px))

        media(maxWidth(640.px)) {
            self style {
                property("max-width", value(100.percent))
                property("padding-left", value(16.px))
                property("padding-right", value(16.px))
            }
        }

        media(maxWidth(1276.px)) {
            self style {
                property("max-width", value(996.px))
                property("padding-left", value(22.px))
                property("padding-right", value(22.px))
            }
        }

        media(maxWidth(1000.px)) {
            self style {
                property("max-width", value(100.percent))
                property("padding-left", value(22.px))
                property("padding-right", value(22.px))
            }
        }
    }
}
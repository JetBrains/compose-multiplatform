package com.sample.style

import org.jetbrains.compose.web.css.*

object WtCards : StyleSheet(AppStylesheet) {
    val wtCard by style {
        display(Flex)
        flexDirection(Column)
        border(1.px, Solid)
        property("min-height", 0)
        property("box-sizing", "border-box")
    }

    val wtCardThemeLight by style {
        border(color = rgba(39, 40, 44, .2))
        color("#27282c")
        backgroundColor("white")
    }

    val wtCardThemeDark by style {
        backgroundColor(rgba(255, 255, 255, 0.05))
        color(rgba(255, 255, 255, 0.6))
        border(0.px)
    }

    val wtCardSection by style {
        position(Relative)
        property("overflow", "auto")
        property("flex", "1 1 auto")
        property("min-height", 0)
        property("box-sizing", "border-box")
        property("padding", "24px 32px")

        media(mediaMaxWidth(640.px)) {
            self style { padding(16.px) }
        }
    }

    val wtVerticalFlex by style {
        display(Flex)
        flexDirection(Column)
        alignItems(FlexStart)
    }

    val wtVerticalFlexGrow by style {
        flexGrow(1)
        property("max-width", 100.percent)
    }
}
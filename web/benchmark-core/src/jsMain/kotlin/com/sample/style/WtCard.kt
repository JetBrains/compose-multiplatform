package com.sample.style

import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.media
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px

object WtCards : StyleSheet(AppStylesheet) {
    val wtCard by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        border(1.px, LineStyle.Solid)
        property("min-height", 0)
        property("box-sizing", "border-box")
    }

    val wtCardThemeLight by style {
        border(color = Color.RGBA(39, 40, 44, .2))
        color("#27282c")
        backgroundColor("white")
    }

    val wtCardThemeDark by style {
        backgroundColor(Color.RGBA(255, 255, 255, 0.05))
        color(Color.RGBA(255, 255, 255, 0.6))
        border(0.px)
    }

    val wtCardSection by style {
        position(Position.Relative)
        property("overflow", "auto")
        property("flex", "1 1 auto")
        property("min-height", 0)
        property("box-sizing", "border-box")
        property("padding", "24px 32px")

        media(maxWidth(640.px)) {
            self style { padding(16.px) }
        }
    }

    val wtVerticalFlex by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.FlexStart)
    }

    val wtVerticalFlexGrow by style {
        flexGrow(1)
        property("max-width", 100.percent)
    }
}
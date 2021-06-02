package com.sample.style

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.*

object WtCards : StyleSheet(AppStylesheet) {
    val wtCard by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        border(1.px, LineStyle.Solid)
        property("min-height", value(0))
        property("box-sizing", value("border-box"))
    }
    
    val wtCardThemeLight by style {
        border(color = Color.RGBA(39,40,44,.2))
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
        property("overflow", value("auto"))
        property("flex", value("1 1 auto"))
        property("min-height", value(0))
        property("box-sizing", value("border-box"))
        property("padding", value("24px 32px"))

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
        property("max-width", value(100.percent))
    }
}
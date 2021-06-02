package com.sample.style

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.*

object AppCSSVariables : CSSVariables {
    val wtColorGreyLight by variable<Color>()
    val wtColorGreyDark by variable<Color>()

    val wtOffsetTopUnit by variable<CSSSizeValue>()
    val wtHorizontalLayoutGutter by variable<CSSSizeValue>()
    val wtFlowUnit by variable<CSSSizeValue>()

    val wtHeroFontSize by variable<CSSSizeValue>()
    val wtHeroLineHeight by variable<CSSSizeValue>()
    val wtSubtitle2FontSize by variable<CSSSizeValue>()
    val wtSubtitle2LineHeight by variable<CSSSizeValue>()
    val wtH2FontSize by variable<CSSSizeValue>()
    val wtH2LineHeight by variable<CSSSizeValue>()
    val wtH3FontSize by variable<CSSSizeValue>()
    val wtH3LineHeight by variable<CSSSizeValue>()

    val wtColCount by variable<Int>()
}


object AppStylesheet : StyleSheet() {
    val composeLogo by style {
        property("max-width", value(100.percent))
    }

    val composeTitleTag by style {
        property("padding", value("5px 12px"))
        property("letter-spacing", value("normal"))
        property("font-weight", value(400))
        property("line-height", value(24.px))

        position(Position.Relative)
        top((-32).px)
        marginLeft(8.px)
        fontSize(15.px)
        backgroundColor(Color.RGBA(39, 40, 44, .05))
        color(Color.RGBA(39,40,44,.7))
        borderRadius(4.px, 4.px, 4.px)

        media(maxWidth(640.px)) {
            self style {
                top((-16).px)
            }
        }
    }

    init {
        "label, a, button" style {
            property(
                "font-family",
                value("system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Droid Sans,Helvetica Neue,Arial,sans-serif")
            )
        }

        CSSSelector.Universal style {
            AppCSSVariables.wtColorGreyLight(Color("#f4f4f4"))
            AppCSSVariables.wtColorGreyDark(Color("#323236"))
            AppCSSVariables.wtOffsetTopUnit(24.px)

            margin(0.px)
        }

        media(maxWidth(640.px)) {
            CSSSelector.Universal style {
                AppCSSVariables.wtOffsetTopUnit(16.px)
                AppCSSVariables.wtFlowUnit(16.px)
            }
        }

        CSSSelector.Attribute(
            name = "class",
            value = "wtCol",
            operator = CSSSelector.Attribute.Operator.Contains
        ) style {
            property("margin-right", AppCSSVariables.wtHorizontalLayoutGutter.value())
            property("margin-left", AppCSSVariables.wtHorizontalLayoutGutter.value())

            property(
                "flex-basis",
                value("calc(8.33333%*${AppCSSVariables.wtColCount.value()} - ${AppCSSVariables.wtHorizontalLayoutGutter.value()}*2)")
            )
            property(
                "max-width",
                value("calc(8.33333%*${AppCSSVariables.wtColCount.value()} - ${AppCSSVariables.wtHorizontalLayoutGutter.value()}*2)")
            )
            property("box-sizing", value("border-box"))
        }
    }
}
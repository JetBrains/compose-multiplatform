package com.sample.style

import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.*

object AppCSSVariables : CSSVariables {
    val wtColorGreyLight by variable<Color>()
    val wtColorGreyDark by variable<Color>()

    val wtOffsetTopUnit by variable<CSSUnitValue>()
    val wtHorizontalLayoutGutter by variable<CSSUnitValue>()
    val wtFlowUnit by variable<CSSUnitValue>()

    val wtHeroFontSize by variable<CSSUnitValue>()
    val wtHeroLineHeight by variable<CSSUnitValue>()
    val wtSubtitle2FontSize by variable<CSSUnitValue>()
    val wtSubtitle2LineHeight by variable<CSSUnitValue>()
    val wtH2FontSize by variable<CSSUnitValue>()
    val wtH2LineHeight by variable<CSSUnitValue>()
    val wtH3FontSize by variable<CSSUnitValue>()
    val wtH3LineHeight by variable<CSSUnitValue>()

    val wtColCount by variable<StylePropertyNumber>()
}


object AppStylesheet : StyleSheet() {
    val composeLogo by style {
        property("max-width", 100.percent)
    }

    val composeTitleTag by style {
        property("padding", "5px 12px")
        property("letter-spacing", "normal")
        property("font-weight", 400)
        property("line-height", 24.px)

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
                "system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Droid Sans,Helvetica Neue,Arial,sans-serif"
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
                "calc(8.33333%*${AppCSSVariables.wtColCount.value()} - ${AppCSSVariables.wtHorizontalLayoutGutter.value()}*2)"
            )
            property(
                "max-width",
                "calc(8.33333%*${AppCSSVariables.wtColCount.value()} - ${AppCSSVariables.wtHorizontalLayoutGutter.value()}*2)"
            )
            property("box-sizing", "border-box")
        }
    }
}
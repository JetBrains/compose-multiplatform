package com.sample.style

import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.*

object WtTexts : StyleSheet(AppStylesheet) {

    val wtHero by style {
        color("#27282c")
        fontSize(60.px)
        property("font-size", AppCSSVariables.wtHeroFontSize.value(60.px))
        property("letter-spacing", (-1.5).px)
        property("font-weight", 900)
        property("line-height", 64.px)
        property("line-height", AppCSSVariables.wtHeroLineHeight.value(64.px))

        media(maxWidth(640.px)) {
            self style {
                AppCSSVariables.wtHeroFontSize(42.px)
                AppCSSVariables.wtHeroLineHeight(48.px)
            }
        }

        property(
            "font-family",
            "Gotham SSm A,Gotham SSm B,system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Droid Sans,Helvetica Neue,Arial,sans-serif"
        )
    }

    val wtSubtitle2 by style {
        color("#27282c")
        fontSize(28.px)
        property("font-size", AppCSSVariables.wtSubtitle2FontSize.value(28.px))
        property("letter-spacing", "normal")
        property("font-weight", 300)
        property("line-height", 40.px)
        property("line-height", AppCSSVariables.wtSubtitle2LineHeight.value(40.px))

        media(maxWidth(640.px)) {
            self style {
                AppCSSVariables.wtSubtitle2FontSize(24.px)
                AppCSSVariables.wtSubtitle2LineHeight(32.px)
            }
        }

        property(
            "font-family",
            "Gotham SSm A,Gotham SSm B,system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Droid Sans,Helvetica Neue,Arial,sans-serif"
        )
    }

    val wtText1 by style {
        color(Color.RGBA(39, 40, 44, .7))
        fontSize(18.px)
        property("letter-spacing", "normal")
        property("font-weight", 400)
        property("line-height", 28.px)

        property(
            "font-family",
            "system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Droid Sans,Helvetica Neue,Arial,sans-serif"
        )
    }

    val wtText1ThemeDark by style {
        color(Color.RGBA(255, 255, 255, 0.6))
    }

    val wtText2 by style {
        color(Color.RGBA(39, 40, 44, .7))
        fontSize(15.px)
        property("letter-spacing", "normal")
        property("font-weight", 400)
        property("line-height", 24.px)

        property(
            "font-family",
            "system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Droid Sans,Helvetica Neue,Arial,sans-serif"
        )
    }

    val wtText3 by style {
        color(Color.RGBA(39, 40, 44, .7))
        fontSize(12.px)
        property("letter-spacing", "normal")
        property("font-weight", 400)
        property("line-height", 16.px)

        property(
            "font-family",
            "system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Droid Sans,Helvetica Neue,Arial,sans-serif"
        )
    }

    val wtTextPale by style {
        color(Color.RGBA(255, 255, 255, 0.30))
    }

    val wtText2ThemeDark by style {
        color(Color.RGBA(255, 255, 255, 0.6))
    }

    val wtText3ThemeDark by style {
        color(Color.RGBA(255, 255, 255, 0.6))
    }

    val wtLink by style {
        property("border-bottom", "1px solid transparent")
        property("text-decoration", "none")
        color("#167dff")

        hover(self) style {
            property("border-bottom-color", "#167dff")
        }
    }

    val wtH2 by style {
        color("#27282c")
        fontSize(31.px)
        property("font-size", AppCSSVariables.wtH2FontSize.value(31.px))
        property("letter-spacing", (-.5).px)
        property("font-weight", 700)
        property("line-height", 40.px)
        property("line-height", AppCSSVariables.wtH2LineHeight.value(40.px))

        media(maxWidth(640.px)) {
            self style {
                AppCSSVariables.wtH2FontSize(24.px)
                AppCSSVariables.wtH2LineHeight(32.px)
            }
        }

        property(
            "font-family",
            "Gotham SSm A,Gotham SSm B,system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Droid Sans,Helvetica Neue,Arial,sans-serif"
        )
    }

    val wtH2ThemeDark by style {
        color("#fff")
    }

    val wtH3 by style {
        color("#27282c")
        fontSize(21.px)
        property("font-size", AppCSSVariables.wtH3FontSize.value(20.px))
        property("letter-spacing", "normal")
        property("font-weight", 700)
        property("line-height", 28.px)
        property("line-height", AppCSSVariables.wtH3LineHeight.value(28.px))

        property(
            "font-family",
            "system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Droid Sans,Helvetica Neue,Arial,sans-serif"
        )
    }

    val wtH3ThemeDark by style {
        color("#fff")
    }

    val wtButton by style {
        color("white")
        backgroundColor("#167dff")
        fontSize(15.px)
        display(DisplayStyle.InlineBlock)
        property("text-decoration", "none")
        property("border-radius", 24.px)
        property("padding", "12px 32px")
        property("line-height", 24.px)
        property("font-weight", 400)
        property("width", "fit-content")

        hover(self) style {
            backgroundColor(Color.RGBA(22, 125, 255, .8))
        }
    }

    val wtLangButton by style {
        display(DisplayStyle.LegacyInlineFlex)
        justifyContent(JustifyContent.Center)
        alignItems(AlignItems.Center)
        backgroundColor(Color("transparent"))
        border(0.px)

        property("outline", "none")

        hover(self) style {
            backgroundColor(Color.RGBA(255, 255, 255, 0.1))
        }
    }

    val wtButtonContrast by style {
        color("white")
        backgroundColor("#27282c")

        hover(self) style {
            backgroundColor(Color.RGBA(39, 40, 44, .7))
        }
    }

    val wtSocialButtonItem by style {
        property("margin-right", 16.px)
        marginLeft(16.px)
        padding(12.px)
        backgroundColor("transparent")
        display(DisplayStyle.LegacyInlineFlex)

        hover(self) style {
            backgroundColor(Color.RGBA(255, 255, 255, 0.1))
            property("border-radius", 24.px)
        }

        media(maxWidth(640.px)) {
            self style {
                property("margin-right", 8.px)
                property("margin-left", 8.px)
            }
        }
    }
}
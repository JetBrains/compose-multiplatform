package com.sample.style

import org.jetbrains.compose.web.css.*

object WtTexts : StyleSheet(AppStylesheet) {

    val wtHero by style {
        color(Color("#27282c"))
        fontSize(60.px)
        fontSize(AppCSSVariables.wtHeroFontSize.value(60.px))
        letterSpacing(-1.5.px)
        fontWeight(900)
        lineHeight(64.px)
        lineHeight(AppCSSVariables.wtHeroLineHeight.value(64.px))

        media(mediaMaxWidth(640.px)) {
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
        color(Color("#27282c"))
        fontSize(28.px)
        fontSize(AppCSSVariables.wtSubtitle2FontSize.value(28.px))
        letterSpacing("normal")
        fontWeight(300)
        lineHeight(40.px)
        lineHeight(AppCSSVariables.wtSubtitle2LineHeight.value(40.px))

        media(mediaMaxWidth(640.px)) {
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
        color(rgba(39, 40, 44, .7))
        fontSize(18.px)
        letterSpacing("normal")
        fontWeight(400)
        lineHeight(28.px)

        property(
            "font-family",
            "system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Droid Sans,Helvetica Neue,Arial,sans-serif"
        )
    }

    val wtText1HardnessHard by style {
        color(rgb(15, 16, 17))
    }

    val wtText1ThemeDark by style {
        color(rgba(255, 255, 255, 0.6))
    }

    val wtText2 by style {
        color(rgba(39, 40, 44, .7))
        fontSize(15.px)
        letterSpacing("normal")
        fontWeight(400)
        lineHeight(24.px)

        property(
            "font-family",
            "system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Droid Sans,Helvetica Neue,Arial,sans-serif"
        )
    }

    val wtText3 by style {
        color(rgba(39, 40, 44, .7))
        fontSize(12.px)
        letterSpacing("normal")
        fontWeight(400)
        lineHeight(16.px)

        property(
            "font-family",
            "system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Droid Sans,Helvetica Neue,Arial,sans-serif"
        )
    }

    val wtTextPale by style {
        color(rgba(255, 255, 255, 0.30))
    }

    val wtText2ThemeDark by style {
        color(rgba(255, 255, 255, 0.6))
    }

    val wtText3ThemeDark by style {
        color(rgba(255, 255, 255, 0.6))
    }

    val wtLink by style {
        property("border-bottom", "1px solid transparent")
        property("text-decoration", "none")
        color(Color("#167dff"))

        hover(self) style {
            property("border-bottom-color", "#167dff")
        }
    }

    val wtH2 by style {
        color(Color("#27282c"))
        fontSize(31.px)
        fontSize(AppCSSVariables.wtH2FontSize.value(31.px))
        letterSpacing(-.5.px)
        fontWeight(700)
        lineHeight(40.px)
        lineHeight(40.px)

        media(mediaMaxWidth(640.px)) {
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
        color(Color("#fff"))
    }

    val wtH3 by style {
        color(Color("#27282c"))
        fontSize(21.px)
        fontSize(AppCSSVariables.wtH3FontSize.value(20.px))
        letterSpacing("normal")
        fontWeight(700)
        lineHeight(28.px)
        lineHeight(AppCSSVariables.wtH3LineHeight.value(28.px))

        property(
            "font-family",
            "system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Droid Sans,Helvetica Neue,Arial,sans-serif"
        )
    }

    val wtH3ThemeDark by style {
        color(Color("#fff"))
    }

    val wtButton by style {
        color(Color("white"))
        backgroundColor(Color("#167dff"))
        fontSize(15.px)
        display(DisplayStyle.InlineBlock)
        textDecoration("none")
        borderRadius(24.px)
        padding(12.px, 32.px)
        lineHeight(24.px)
        fontWeight(400)
        property("width", "fit-content")

        hover(self) style {
            backgroundColor(rgba(22, 125, 255, .8))
        }
    }

    val wtLangButton by style {
        display(DisplayStyle.LegacyInlineFlex)
        justifyContent(JustifyContent.Center)
        alignItems(AlignItems.Center)
        backgroundColor(Color("transparent"))
        border(0.px)

        outline("none")

        hover(self) style {
            backgroundColor(rgba(255, 255, 255, 0.1))
        }
    }

    val wtButtonContrast by style {
        color(Color("white"))
        backgroundColor(Color("#27282c"))

        hover(self) style {
            backgroundColor(rgba(39, 40, 44, .7))
        }
    }

    val wtButtonThemeLight by style {
        color(Color("#27282c"))
        backgroundColor(Color("transparent"))
        border {
            style(LineStyle.Solid)
            width(1.px)
            color(Color("#27282c"))
        }

        hover(self) style {
            backgroundColor(rgba(39,40,44,.05))
        }
    }

    val wtSocialButtonItem by style {
        marginRight(16.px)
        marginLeft(16.px)
        padding(12.px)
        backgroundColor(Color("transparent"))
        display(DisplayStyle.LegacyInlineFlex)

        hover(self) style {
            backgroundColor(rgba(255, 255, 255, 0.1))
            borderRadius(24.px)
        }

        media(mediaMaxWidth(640.px)) {
            self style {
                marginRight(8.px)
                marginLeft(8.px)
            }
        }
    }
}

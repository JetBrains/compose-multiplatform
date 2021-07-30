package com.sample.style

import org.jetbrains.compose.web.css.*

object WtSections : StyleSheet(AppStylesheet) {

    val wtSection by style {
        boxSizing("border-box")
        paddingBottom(96.px)
        paddingTop(1.px)
        property(
            propertyName = "padding-bottom",
            value = "calc(4*${AppCSSVariables.wtOffsetTopUnit.value(24.px)})"
        )
        backgroundColor(Color("#fff"))
    }

    val wtSectionBgGrayLight by style {
        backgroundColor(Color("#f4f4f4"))
        backgroundColor(AppCSSVariables.wtColorGreyLight.value())
    }

    val wtSectionBgGrayDark by style {
        backgroundColor(Color("#323236"))
        backgroundColor(AppCSSVariables.wtColorGreyDark.value())
    }
}

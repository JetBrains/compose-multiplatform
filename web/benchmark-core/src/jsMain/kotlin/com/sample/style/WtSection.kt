package com.sample.style

import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.*

object WtSections : StyleSheet(AppStylesheet) {

    val wtSection by style {
        property("box-sizing", value("border-box"))
        property("padding-bottom", value(96.px))
        property("padding-top", value(1.px))
        property(
            propertyName = "padding-bottom",
            value = value(
                "calc(4*${AppCSSVariables.wtOffsetTopUnit.value(24.px)})"
            )
        )
        backgroundColor("#fff")
    }

    val wtSectionBgGrayLight by style {
        backgroundColor("#f4f4f4")
        backgroundColor(AppCSSVariables.wtColorGreyLight.value())
    }

    val wtSectionBgGrayDark by style {
        backgroundColor("#323236")
        backgroundColor(AppCSSVariables.wtColorGreyDark.value())
    }
}
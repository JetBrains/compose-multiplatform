package com.sample.content

import androidx.compose.runtime.Composable
import androidx.compose.web.attributes.ATarget
import androidx.compose.web.attributes.target
import androidx.compose.web.css.*
import androidx.compose.web.elements.*
import com.sample.style.*
import kotlinx.browser.window

@Composable
fun Header() {
    Section(attrs = {
        classes(WtSections.wtSectionBgGrayDark)
    }) {
        Div(attrs = { classes(WtContainer.wtContainer) }) {
            Div(attrs = {
                classes(WtRows.wtRow, WtRows.wtRowSizeM)
            }, style = {
                alignItems(AlignItems.Center)
                justifyContent(JustifyContent.SpaceBetween)
            }) {
                Logo()
                // TODO: support i18n
                //LanguageButton()
            }
        }
    }
}

@Composable
private fun Logo() {
    Div(attrs = {
        classes(WtCols.wtColInline)
    }) {
        A(attrs = {
            target(ATarget.Blank)
        }, href = "https://www.jetbrains.com/") {
            Div(attrs = {
                classes("jetbrains-logo", "_logo-jetbrains-square", "_size-3")
            }) {}
        }
    }
}

@Composable
private fun LanguageButton() {
    Div(attrs = {
        classes(WtCols.wtColInline)
    }) {
        Button(attrs = {
            classes(WtTexts.wtButton, WtTexts.wtLangButton)
            onClick { window.alert("Oops! This feature is yet to be implemented") }
        }, style = {
            property("cursor", value("pointer"))
        }) {
            Img(src = "ic_lang.svg", style = {
                property("padding-left", value(8.px))
                property("padding-right", value(8.px))
            }) {}
            Text("English")
        }
    }
}
package com.sample.components

import androidx.compose.runtime.Composable
import androidx.compose.web.attributes.ATarget
import androidx.compose.web.attributes.target
import androidx.compose.web.css.*
import androidx.compose.web.elements.*
import com.sample.style.*


data class LinkOnCard(val linkText: String, val linkUrl: String)

@Composable
fun Card(title: String, linkOnCard: LinkOnCard?, content: @Composable () -> Unit) {
    Div(attrs = {
        classes(
            WtCards.wtCard,
            WtCards.wtCardThemeLight,
            WtCols.wtCol6,
            WtCols.wtColMd6,
            WtCols.wtColSm12,
            WtOffsets.wtTopOffset24,
        )
    }) {
        Div(attrs = {
            classes(WtCards.wtCardSection, WtCards.wtVerticalFlex)
        }) {
            Div(attrs = { classes(WtCards.wtVerticalFlexGrow) }) {
                H3(attrs = {
                    classes(WtTexts.wtH3)
                }) {
                    Text(title)
                }

                content()
            }

            if (linkOnCard != null) {
                A(attrs = {
                    classes(WtTexts.wtLink, WtOffsets.wtTopOffset24)
                    target(ATarget.Blank)
                }, href = linkOnCard.linkUrl) {
                    Text(linkOnCard.linkText)
                }
            }
        }
    }
}

@Composable
fun CardDark(title: String, linkOnCard: LinkOnCard?, content: @Composable () -> Unit) {
    Div(attrs = {
        classes(
            WtCards.wtCard,
            WtCards.wtCardThemeDark,
            WtCols.wtCol4,
            WtCols.wtColMd6,
            WtCols.wtColSm12,
            WtOffsets.wtTopOffset24,
        )
    }) {
        Div(attrs = {
            classes(WtCards.wtCardSection, WtCards.wtVerticalFlex)
        }) {
            Div(attrs = { classes(WtCards.wtVerticalFlexGrow) }) {
                H3(attrs = {
                    classes(WtTexts.wtH3, WtTexts.wtH3ThemeDark)
                }) {
                    Text(title)
                }

                content()
            }

            if (linkOnCard != null) {
                A(attrs = {
                    classes(WtTexts.wtLink, WtOffsets.wtTopOffset24)
                    target(ATarget.Blank)
                }, href = linkOnCard.linkUrl) {
                    Text(linkOnCard.linkText)
                }
            }
        }
    }
}
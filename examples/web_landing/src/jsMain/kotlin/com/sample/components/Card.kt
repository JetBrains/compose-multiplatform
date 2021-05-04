package com.sample.components

import androidx.compose.runtime.Composable
import androidx.compose.web.attributes.ATarget
import androidx.compose.web.attributes.target
import androidx.compose.web.css.*
import androidx.compose.web.elements.*
import com.sample.style.*


data class LinkOnCard(val linkText: String, val linkUrl: String)

@Composable
private fun CardTitle(title: String, darkTheme: Boolean = false) {
    H3(attrs = {
        classes {
            +WtTexts.wtH3
            if (darkTheme) +WtTexts.wtH3ThemeDark
        }
    }) {
        Text(title)
    }
}

@Composable
private fun CardLink(link: LinkOnCard) {
    A(
        attrs = {
            classes(WtTexts.wtLink, WtOffsets.wtTopOffset24)
            target(ATarget.Blank)
        },
        href = link.linkUrl
    ) {
        Text(link.linkText)
    }
}

@Composable
fun Card(
    title: String,
    links: List<LinkOnCard>,
    darkTheme: Boolean = false,
    wtExtraStyleClasses: List<String> = listOf(WtCols.wtCol6, WtCols.wtColMd6, WtCols.wtColSm12),
    content: @Composable () -> Unit
) {
    Div(attrs = {
        classes {
            +WtCards.wtCard
            +WtOffsets.wtTopOffset24
            wtExtraStyleClasses.forEach { +it }
            +if (darkTheme) WtCards.wtCardThemeDark else WtCards.wtCardThemeLight
        }
    }) {
        Div(attrs = {
            classes(WtCards.wtCardSection, WtCards.wtVerticalFlex)
        }) {

            Div(attrs = { classes(WtCards.wtVerticalFlexGrow) }) {
                CardTitle(title = title, darkTheme = darkTheme)
                content()
            }

            links.forEach {
                CardLink(it)
            }
        }
    }
}

@Composable
fun CardDark(
    title: String,
    links: List<LinkOnCard>,
    wtExtraStyleClasses: List<String> = listOf(WtCols.wtCol6, WtCols.wtColMd6, WtCols.wtColSm12),
    content: @Composable () -> Unit
) {
    Card(
        title = title,
        links = links,
        darkTheme = true,
        wtExtraStyleClasses = wtExtraStyleClasses,
        content = content
    )
}
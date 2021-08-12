package com.sample.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import com.sample.style.*


data class LinkOnCard(val linkText: String, val linkUrl: String)

@Composable
private fun CardTitle(title: String, darkTheme: Boolean = false) {
    H3({
        classes(WtTexts.wtH3)
        if (darkTheme) {
            classes(WtTexts.wtH3ThemeDark)
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
    Div({
        classes(WtCards.wtCard, WtOffsets.wtTopOffset24, *wtExtraStyleClasses.toTypedArray())
        classes(if (darkTheme) WtCards.wtCardThemeDark else WtCards.wtCardThemeLight)
    }) {
        Div({
            classes(WtCards.wtCardSection, WtCards.wtVerticalFlex)
        }) {

            Div({ classes(WtCards.wtVerticalFlexGrow) }) {
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
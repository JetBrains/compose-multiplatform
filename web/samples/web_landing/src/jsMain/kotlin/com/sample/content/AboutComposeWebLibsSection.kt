package com.sample.content

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.value
import org.jetbrains.compose.web.dom.*
import com.sample.components.Card
import com.sample.components.ContainerInSection
import com.sample.components.LinkOnCard
import com.sample.style.*
import org.jetbrains.compose.web.css.paddingTop

data class CardWithListPresentation(
    val title: String,
    val list: List<String>,
    val links: List<LinkOnCard> = emptyList()
)

private fun createAboutComposeWebCards(): List<CardWithListPresentation> {
    return listOf(
        CardWithListPresentation(
            title = "Composable DOM API",
            list = listOf(
                "Express your design and layout in terms of DOM elements and HTML tags",
                "Use a type-safe HTML DSL to build your UI representation",
                "Get full control over the look and feel of your application by creating stylesheets with a typesafe CSS DSL",
                "Integrate with other JavaScript libraries via DOM subtrees"
            )
        ),
        CardWithListPresentation(
            title = "Multiplatform Widgets With Web Support",
            list = listOf(
                "Use and build Compose widgets that work on Android, Desktop, and Web by utilizing Kotlin's expect-actual mechanisms to provide platform-specific implementations",
                "Experiment with a set of layout primitives and APIs that mimic the features you already know from Compose for Desktop and Android"
            )
        )
    )
}

@Composable
fun ComposeWebLibraries() {
    ContainerInSection(WtSections.wtSectionBgGrayLight) {
        H2(attrs = { classes(WtTexts.wtH2) }) {
            Text("Building user interfaces with Compose for Web")
        }

        Div(attrs = {
            classes(WtRows.wtRow, WtRows.wtRowSizeM)
        }) {
            Div(attrs = {
                classes(WtCols.wtCol6, WtCols.wtColMd6, WtCols.wtColSm12, WtOffsets.wtTopOffset24)
            }) {
                P(attrs = {
                    classes(WtTexts.wtText1)
                }) {
                    Text("Compose for Web allows you to build reactive user interfaces for the web in Kotlin, using the concepts and APIs of Jetpack Compose to express the state, behavior, and logic of your application.")
                }
            }

            Div(attrs = {
                classes(WtCols.wtCol6, WtCols.wtColMd6, WtCols.wtColSm12, WtOffsets.wtTopOffset24)
            }) {
                P(attrs = {
                    classes(WtTexts.wtText1)
                }) {
                    Text("Compose for Web provides multiple ways of declaring user interfaces in Kotlin code, allowing you to have full control over your website layout with a declarative DOM API, or use versions of the widgets you already know from Jetpack Compose for Desktop and Android.\n")
                }
            }
        }

        Div(attrs = {
            classes(WtRows.wtRow, WtRows.wtRowSizeM, WtOffsets.wtTopOffset48)
        }) {
            createAboutComposeWebCards().forEach { CardWithList(it) }
        }
    }
}

@Composable
private fun CardWithList(card: CardWithListPresentation) {
    Card(
        title = card.title,
        links = card.links
    ) {
        Ul(attrs = {
            classes(WtTexts.wtText2)
        }) {
            card.list.forEachIndexed { _, it ->
                Li({
                    style {
                        paddingTop(24.px)
                    }
                }) { Text(it) }
            }
        }
    }
}

package com.sample.content

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import com.sample.components.CardDark
import com.sample.components.ContainerInSection
import com.sample.components.LinkOnCard
import com.sample.style.*

private data class GetStartedCardPresentation(
    val title: String,
    val content: String,
    val links: List<LinkOnCard>
)

private fun getCards(): List<GetStartedCardPresentation> {
    return listOf(
        GetStartedCardPresentation(
            title = "Start tutorial here",
            content = "In this tutorial we will see how to create our first web UI application using Compose for Web.",
            links = listOf(
                LinkOnCard(
                    linkText = "View tutorial",
                    linkUrl = "https://github.com/JetBrains/compose-jb/tree/master/tutorials/Web/Getting_Started"
                )
            )
        ),
        GetStartedCardPresentation(
            title = "Landing page example",
            content = "An example of a landing page built using the Composable DOM API and Stylesheet DSL.",
            links = listOf(
                LinkOnCard(
                    linkText = "Explore the source code",
                    linkUrl = "https://github.com/JetBrains/compose-jb/tree/master/examples/web-landing"
                )
            )
        ),
        GetStartedCardPresentation(
            title = "Falling Balls app example",
            content = "This example demonstrates the use of multiplatform widgets – sharing user interface code between Compose for Desktop and Web.",
            links = listOf(
                LinkOnCard(
                    linkText = "Explore the source code",
                    linkUrl = "https://github.com/JetBrains/compose-jb/tree/master/examples/falling-balls-web"
                ),
                LinkOnCard(
                    linkText = "Play",
                    linkUrl = "https://falling-balls.ui.pages.jetbrains.team/"
                )
            )
        )
    )
}

@Composable
private fun CardContent(text: String) {
    P(attrs = {
        classes(WtTexts.wtText2, WtTexts.wtText2ThemeDark, WtOffsets.wtTopOffset24)
    }) {
        Text(text)
    }
}

@Composable
fun GetStarted() {
    ContainerInSection(WtSections.wtSectionBgGrayDark) {
        H1(attrs = {
            classes(WtTexts.wtH2, WtTexts.wtH2ThemeDark)
        }) {
            Text("Try out the Compose for Web")
        }

        Div(attrs = {
            classes(WtRows.wtRowSizeM, WtRows.wtRow, WtOffsets.wtTopOffset24)
        }) {
            Div(attrs = {
                classes(WtCols.wtCol6, WtCols.wtColMd10, WtCols.wtColSm12, WtOffsets.wtTopOffset24)
            }) {
                P(attrs = {
                    classes(WtTexts.wtText1)
                    style  {
                        color(Color("#fff"))
                    }
                }) {
                    Text("Ready for your next adventure? Learn how to build reactive user interfaces with Compose for Web.")
                }
            }
        }

        Div(
            attrs = {
                classes(WtRows.wtRow, WtRows.wtRowSizeM, WtOffsets.wtTopOffset24)
            }
        ) {
            getCards().forEach {
                CardDark(
                    title = it.title,
                    links = it.links,
                    wtExtraStyleClasses = listOf(WtCols.wtCol4, WtCols.wtColMd6, WtCols.wtColSm12)
                ) {
                    CardContent(it.content)
                }
            }
        }
    }
}
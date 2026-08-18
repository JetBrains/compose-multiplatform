package com.sample.stringrendering

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Code
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Footer
import org.jetbrains.compose.web.dom.Form
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

private data class LandingLink(
    val text: String,
    val url: String,
)

private data class LandingCardData(
    val title: String,
    val content: String,
    val links: List<LandingLink>,
)

private data class SocialLink(
    val url: String,
    val icon: String,
)

private data class CodeSnippetData(
    val title: String,
    val source: String,
)

private val features = listOf(
    "Same reactive engine that is used on Android/Desktop allows using a common codebase.",
    "Framework for rich UI creation for Kotlin/JS.",
    "Convenient Kotlin DOM DSL that covers all common frontend development scenarios.",
    "Comprehensive CSS-in-Kotlin/JS API.",
)

private val getStartedCards = listOf(
    LandingCardData(
        title = "Start tutorial here",
        content = "In this tutorial we will see how to create our first web UI application using Compose for Web.",
        links = listOf(
            LandingLink(
                text = "View tutorial",
                url = "https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/HTML/Getting_Started",
            )
        ),
    ),
    LandingCardData(
        title = "Landing page example",
        content = "An example of a landing page built using the Composable DOM API and Stylesheet DSL.",
        links = listOf(
            LandingLink(
                text = "Explore the source code",
                url = "https://github.com/JetBrains/compose-multiplatform/tree/master/examples/html/landing",
            )
        ),
    ),
    LandingCardData(
        title = "Compose Bird",
        content = "A simple game built using the most basic Composable DOM API.",
        links = listOf(
            LandingLink(
                text = "Explore the source code",
                url = "https://github.com/JetBrains/compose-multiplatform/tree/master/examples/html/compose-bird",
            ),
            LandingLink(
                text = "Play",
                url = "https://compose-bird.ui.pages.jetbrains.team/",
            ),
        ),
    ),
)

private val codeSnippets = listOf(
    CodeSnippetData(
        title = "Simple Counter using Composable DOM",
        source = """
            fun main() {
                val count = mutableStateOf(0)

                renderComposable(rootElementId = "root") {
                    Button(attrs = {
                        onClick { count.value = count.value - 1 }
                    }) {
                        Text("-")
                    }
                    Span(attrs = { style { padding(15.px) }}) { /* we use inline style here */
                        Text("${'$'}{count.value}")
                    }
                    Button(attrs = {
                        onClick { count.value = count.value + 1 }
                    }) {
                        Text("+")
                    }
                }
            }
        """.trimIndent(),
    ),
    CodeSnippetData(
        title = "Declare and use a stylesheet",
        source = """
            object MyStyleSheet : StyleSheet() {
                val container by style { /* define a class `container` */
                    border(1.px, LineStyle.Solid, Color.RGB(255, 0, 0))
                }
            }

            @Composable
            fun MyComponent() {
                Div(attrs = {
                    classes(MyStyleSheet.container) /* use `container` class */
                }) {
                    Text("Hello world!")
                }
            }

            fun main() {
                renderComposable(rootElementId = "root") {
                    Style(MyStyleSheet) /* mount the stylesheet */
                    MyComponent()
                }
            }
        """.trimIndent(),
    ),
    CodeSnippetData(
        title = "Declare and use CSS variables",
        source = """
            object MyVariables {
                val contentBackgroundColor by variable<Color>() /* declare a variable */
            }

            object MyStyleSheet: StyleSheet() {
                val container by style {
                    MyVariables.contentBackgroundColor(Color("blue")) /* set its value */
                }
                val content by style {
                    backgroundColor(MyVariables.contentBackgroundColor.value()) /* use it */
                }
            }

            @Composable
            fun MyComponent() {
                Div(attrs = {
                    classes(MyStyleSheet.container)
                }) {
                    Span(attrs = {
                        classes(MyStyleSheet.content)
                    }) {
                        Text("Hello world!")
                    }
                }
            }
        """.trimIndent(),
    ),
    CodeSnippetData(
        title = "Hover selector and media query examples",
        source = """
            object MyStyleSheet: StyleSheet() {
                val container by style {

                    backgroundColor(Color("blue"))

                    padding(20.px)

                    hover(self) style { /* `self` is a reference to the class */
                        backgroundColor(Color("red"))
                    }

                    media(maxWidth(500.px)) {
                        self style {
                            padding(10.px)
                        }
                    }
                }
            }
        """.trimIndent(),
    ),
    CodeSnippetData(
        title = "Define a CSS class in a component",
        source = """
            object MyStyleSheet: StyleSheet() {}

            @Composable
            fun MyComponent() {
                Div(attrs = {
                    /* the class name will be generated at runtime */
                    classes(MyStyleSheet.css {

                        backgroundColor(Color("blue"))

                        self + ":hover" style { /* this is an example of a raw selector */
                            backgroundColor(Color("red"))
                        }
                    })
                }) {
                    Text("Hello world!")
                }
            }
        """.trimIndent(),
    ),
)

private val socialLinks = listOf(
    SocialLink("https://www.facebook.com/JetBrains", "ic_fb.svg"),
    SocialLink("https://twitter.com/jetbrains", "ic_twitter.svg"),
    SocialLink("https://www.linkedin.com/company/jetbrains", "ic_linkedin.svg"),
    SocialLink("https://www.youtube.com/user/JetBrainsTV", "ic_youtube.svg"),
    SocialLink("https://www.instagram.com/jetbrains/", "ic_insta.svg"),
    SocialLink("https://blog.jetbrains.com/", "ic_jb_blog.svg"),
    SocialLink("https://blog.jetbrains.com/feed/", "ic_feed.svg"),
)

@Composable
private fun ContentSection(
    vararg classes: String,
    content: @Composable () -> Unit,
) {
    Section(attrs = { classes("content-section", *classes) }) {
        Div(attrs = { classes("container") }) {
            content()
        }
    }
}

@Composable
fun ComposeWebLibraries() {
    ContentSection("section-light") {
        H2(attrs = { classes("section-title") }) {
            Text("Building user interfaces with Compose for Web")
        }

        P(attrs = { classes("section-intro") }) {
            Text(
                "Compose for Web allows you to build reactive user interfaces for the web in " +
                    "Kotlin, using the concepts and APIs of Jetpack Compose to express the state, " +
                    "behavior, and logic of your application."
            )
        }

        Div(attrs = { classes("features-grid") }) {
            features.forEach { feature ->
                Div(attrs = { classes("feature") }) {
                    Img(
                        src = "compose_bullet.svg",
                        alt = "",
                        attrs = { classes("feature-icon") },
                    )
                    P { Text(feature) }
                }
            }
        }

        ExternalLink(
            text = "See all features",
            url = "https://github.com/jetbrains/compose-multiplatform#compose-html",
            classes = arrayOf("secondary-button"),
        )
    }
}

@Composable
fun GetStarted() {
    ContentSection("section-dark") {
        H2(attrs = { classes("section-title") }) {
            Text("Try out the Compose for Web")
        }

        P(attrs = { classes("section-intro") }) {
            Text("Ready for your next adventure? Learn how to build reactive user interfaces with Compose for Web.")
        }

        Div(attrs = { classes("cards-grid") }) {
            getStartedCards.forEach { card ->
                LandingCard(card)
            }
        }
    }
}

@Composable
private fun LandingCard(card: LandingCardData) {
    Div(attrs = { classes("landing-card") }) {
        H3(attrs = { classes("card-title") }) {
            Text(card.title)
        }
        P(attrs = { classes("card-copy") }) {
            Text(card.content)
        }
        Div(attrs = { classes("card-links") }) {
            card.links.forEach { link ->
                ExternalLink(link.text, link.url)
            }
        }
    }
}

@Composable
fun CodeSamples() {
    ContentSection("section-white") {
        Form(
            attrs = {
                classes("code-samples-widget")
                attr("aria-label", "Code sample selection")
            }
        ) {
            codeSnippets.forEachIndexed { index, _ ->
                Input(type = InputType.Radio) {
                    name("code-snippet")
                    value("snippet$index")
                    id("code-snippet-$index")
                    if (index == 0) {
                        defaultChecked()
                    }
                }
            }

            Div(attrs = { classes("code-section-heading") }) {
                H2(attrs = { classes("section-title") }) {
                    Text("Code samples")
                }
                Div(attrs = { classes("sample-switcher") }) {
                    codeSnippets.forEachIndexed { index, _ ->
                        Label(
                            forId = "code-snippet-$index",
                            attrs = { classes("switcher-label", "switcher-label-$index") },
                        ) {
                            Text("${index + 1}")
                        }
                    }
                }
            }

            Div(attrs = { classes("snippet-panels") }) {
                codeSnippets.forEachIndexed { index, snippet ->
                    Div(attrs = { classes("snippet-panel", "snippet-panel-$index") }) {
                        H3(attrs = { classes("code-sample-title") }) {
                            Text(snippet.title)
                        }
                        Div(attrs = { classes("large-code-sample") }) {
                            Pre(attrs = { classes("code-block") }) {
                                Code(attrs = { classes("language-kotlin", "hljs") }) {
                                    Text(snippet.source)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JoinUs() {
    ContentSection("section-light") {
        Div(attrs = { classes("community-copy") }) {
            P(attrs = { classes("community-lead") }) {
                Text("Interested in Compose for other platforms?")
            }
            P(attrs = { classes("community-lead") }) {
                Text("Have a look at ")
                ExternalLink(
                    text = "Compose Multiplatform",
                    url = "https://www.jetbrains.com/lp/compose/",
                )
            }
            P(attrs = { classes("community-lead") }) {
                Text("Feel free to join the ")
                ExternalLink("#compose-web", "https://slack-chats.kotlinlang.org/c/compose-web")
                Text(" channel on Kotlin Slack to discuss Compose for Web, or ")
                ExternalLink("#compose", "https://slack-chats.kotlinlang.org/c/compose")
                Text(" for general Compose discussions.")
            }
        }

        ExternalLink(
            text = "Join Kotlin Slack",
            url = "https://surveys.jetbrains.com/s3/kotlin-slack-sign-up",
            classes = arrayOf("primary-button", "contrast-button"),
        )
    }
}

@Composable
fun LandingFooter() {
    Footer(attrs = { classes("site-footer") }) {
        Div(attrs = { classes("container") }) {
            Div(attrs = { classes("social-row") }) {
                P { Text("Follow us") }
                Div(attrs = { classes("social-links") }) {
                    socialLinks.forEach { link ->
                        A(
                            href = link.url,
                            attrs = {
                                classes("social-link")
                                attr("target", "_blank")
                                attr("rel", "noopener noreferrer")
                            },
                        ) {
                            Img(src = link.icon)
                        }
                    }
                }
            }

            Div(attrs = { classes("copyright-row") }) {
                Span { Text("Copyright © 2000-2021 JetBrains s.r.o.") }
                Span { Text("Developed with drive and IntelliJ IDEA") }
            }
        }
    }
}

@Composable
private fun ExternalLink(
    text: String,
    url: String,
    classes: Array<String> = emptyArray(),
) {
    A(
        href = url,
        attrs = {
            if (classes.isNotEmpty()) {
                classes(*classes)
            }
            attr("target", "_blank")
            attr("rel", "noopener noreferrer")
        },
    ) {
        Text(text)
    }
}

package com.sample.stringrendering

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Body
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Code
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Head
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Hr
import org.jetbrains.compose.web.dom.Html
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Link
import org.jetbrains.compose.web.dom.Main
import org.jetbrains.compose.web.dom.Meta
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Title

private val greetingSnippet = """
    fun greet() = listOf("Hello", "Hallo", "Hola", "Servus").random()

    renderComposable("greetingContainer") {
        var greeting by remember { mutableStateOf(greet()) }
        Button(attrs = { onClick { greeting = greet() } }) {
            Text(greeting)
        }
    }
""".trimIndent()

@Composable
fun LandingDocument() {
    Html(attrs = { lang("en") }) {
        Head {
            Meta { attr("charset", "UTF-8") }
            Meta {
                attr("name", "viewport")
                attr("content", "width=device-width, initial-scale=1, shrink-to-fit=no")
            }
            Title { Text("Compose for Web UI Framework | JetBrains") }
            Link {
                attr("rel", "stylesheet")
                attr("href", "styles.css")
            }
            Link {
                attr("rel", "stylesheet")
                attr("href", "hljs.css")
            }
            Link {
                attr("rel", "stylesheet")
                attr("href", "logos.css")
            }
            Link {
                attr("rel", "icon")
                attr("href", "favicon-32x32.png")
                attr("type", "image/png")
            }
            Link {
                attr("rel", "preconnect")
                attr("href", "https://fonts.gstatic.com")
            }
            Link {
                attr("rel", "stylesheet")
                attr("href", "https://fonts.googleapis.com/css2?family=JetBrains+Mono&display=swap")
            }
        }
        Body {
            LandingPage()
        }
    }
}

@Composable
fun LandingPage() {
    Div(attrs = { classes("page") }) {
        LandingHeader()
        Main(attrs = { classes("main-content") }) {
            LandingIntro()
            ComposeWebLibraries()
            GetStarted()
            CodeSamples()
            JoinUs()
        }
        LandingFooter()
    }
}

@Composable
private fun LandingHeader() {
    Header(attrs = { classes("site-header") }) {
        Div(attrs = { classes("container", "header-content") }) {
            A(
                href = "https://www.jetbrains.com/",
                attrs = {
                    attr("target", "_blank")
                    attr("rel", "noopener noreferrer")
                    attr("aria-label", "JetBrains")
                },
            ) {
                Div(
                    attrs = {
                        classes("jetbrains-logo", "_logo-jetbrains-square", "_size-3")
                    }
                )
            }
        }
    }
}

@Composable
private fun LandingIntro() {
    Section(attrs = { classes("hero-section") }) {
        Div(attrs = { classes("container", "hero-grid") }) {
            Img(
                src = "i1.svg",
                alt = "Compose logo",
                attrs = { classes("compose-logo") },
            )

            Div(attrs = { classes("hero-content") }) {
                H1(attrs = { classes("hero-title") }) {
                    Text("Compose for ")
                    Span(attrs = { classes("nowrap") }) {
                        Text("Web")
                    }
                }

                P(attrs = { classes("hero-lead") }) {
                    Text("Reactive web UIs for Kotlin, based on Google's ")
                    A(
                        href = "https://developer.android.com/jetpack/compose",
                        attrs = {
                            attr("target", "_blank")
                            attr("rel", "noopener noreferrer")
                        },
                    ) {
                        Text("modern toolkit")
                    }
                    Text(" and brought to you by JetBrains.")
                }

                P(attrs = { classes("hero-copy") }) {
                    Text(
                        "Compose for Web simplifies and accelerates UI development for web " +
                            "applications and demonstrates declarative HTML written in Kotlin."
                    )
                }

                IntroCodeSample()

                A(
                    href = "https://github.com/JetBrains/compose-multiplatform",
                    attrs = {
                        classes("primary-button")
                        attr("target", "_blank")
                        attr("rel", "noopener noreferrer")
                    },
                ) {
                    Text("Explore on GitHub")
                }
            }
        }
    }
}

@Composable
private fun IntroCodeSample() {
    Div(attrs = { classes("code-sample") }) {
        Pre(attrs = { classes("code-block") }) {
            Code(attrs = { classes("language-kotlin", "hljs") }) {
                Text(greetingSnippet)
            }
        }

        Hr(attrs = { classes("code-divider") })

        Div(attrs = { classes("sample-result") }) {
            Span(attrs = { classes("sample-label") }) {
                Text("Rendered result:")
            }
            Button(attrs = { classes("result-button") }) {
                Text("Hello")
            }
        }
    }
}

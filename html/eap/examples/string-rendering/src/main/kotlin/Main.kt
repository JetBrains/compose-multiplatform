import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import java.awt.Desktop
import java.io.File

private data class Benefit(
    val symbol: String,
    val title: String,
    val description: String,
)

private val benefits = listOf(
    Benefit("∅", "No JavaScript required", "Useful HTML reaches every browser and crawler."),
    Benefit("◎", "HTML-first SSR", "The initial response already contains the complete page."),
    Benefit("↑", "No client computation", "Rendering work stays on the server."),
    Benefit("ϟ", "Fast when cached", "Reuse generated HTML and skip rendering it again."),
)

private val codeExample = """
    val html = composeHtmlToString {
        Html {
            Head { Style(PageStyles) }
            Body {
                Main(attrs = { classes(PageStyles.page) }) {
                    Span(attrs = { classes(PageStyles.tag) }) {
                        Text("COMPOSE HTML · STRING RENDERING")
                    }
                    H1(attrs = { classes(PageStyles.hero) }) {
                        Text("HTML that arrives ")
                        Span(attrs = { classes(PageStyles.accent) }) { Text("ready.") }
                    }
                    P(attrs = { classes(PageStyles.lead) }) {
                        Text("Compose on the server, send a complete document, and let the browser paint.")
                    }
                    Div(attrs = { classes(PageStyles.grid) }) {
                        benefits.forEach { BenefitCard(it) }
                    }
                }
            }
        }
    }

    @Composable
    private fun BenefitCard(benefit: Benefit) {
        Article(attrs = { classes(PageStyles.card) }) {
            Span(attrs = { classes(PageStyles.icon) }) { Text(benefit.symbol) }
            H2 { Text(benefit.title) }
            P { Text(benefit.description) }
        }
    }

    private object PageStyles : StyleSheet() {
        val card by style {
            padding(22.px)
            backgroundColor(Color("#111727"))
            border(1.px, LineStyle.Solid, Color("#252d42"))
            borderRadius(18.px)
        }
    }
""".trimIndent()

fun main() {
    val html = composeHtmlToString {
        Html(attrs = { attr("lang", "en") }) {
            Head {
                Meta(attrs = { attr("charset", "utf-8") })
                Meta(attrs = {
                    attr("name", "viewport")
                    attr("content", "width=device-width, initial-scale=1")
                })
                Title { Text("Compose HTML string rendering") }
                Style(PageStyles)
            }
            Body { Page() }
        }
    }

    val output = File("build/index.html").absoluteFile
    output.parentFile.mkdirs()
    output.writeText("<!doctype html>$html")
    println("Rendered ${output.toURI()}")
    openInBrowser(output)
}

private fun openInBrowser(file: File) {
    val desktop = runCatching {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
    }.getOrNull()
    val opened = desktop
        ?.takeIf { it.isSupported(Desktop.Action.BROWSE) }
        ?.let { runCatching { it.browse(file.toURI()) }.isSuccess }
        ?: false

    if (!opened) {
        println("Open the rendered page in a browser to view it.")
    }
}

@Composable
private fun Page() {
    Main(attrs = { classes(PageStyles.page) }) {
        Span(attrs = { classes(PageStyles.tag) }) { Text("COMPOSE HTML · STRING RENDERING") }
        H1(attrs = { classes(PageStyles.hero) }) {
            Text("HTML that arrives ")
            Span(attrs = { classes(PageStyles.accent) }) { Text("ready.") }
        }
        P(attrs = { classes(PageStyles.lead) }) {
            Text("Compose on the server, send a complete document, and let the browser paint.")
        }

        Div(attrs = { classes(PageStyles.grid) }) {
            benefits.forEach { BenefitCard(it) }
        }

        H2 { Text("Code used to build this page") }
        Pre(attrs = { classes(PageStyles.code) }) {
            Code { Text(codeExample) }
        }
        Small(attrs = { classes(PageStyles.footer) }) {
            Text("Rendered on the server · 0 KB of JavaScript required")
        }
    }
}

@Composable
private fun BenefitCard(benefit: Benefit) {
    Article(attrs = { classes(PageStyles.card) }) {
        Span(attrs = { classes(PageStyles.icon) }) { Text(benefit.symbol) }
        H2(attrs = { classes(PageStyles.cardTitle) }) { Text(benefit.title) }
        P(attrs = { classes(PageStyles.cardText) }) { Text(benefit.description) }
    }
}

private object PageStyles : StyleSheet() {
    private val background = Color("#090d18")
    private val surface = Color("#111727")
    private val borderColor = Color("#252d42")
    private val text = Color("#f7f8fc")
    private val mutedText = Color("#929cb5")
    private val accentColor = Color("#b8ff65")

    val page by style {
        maxWidth(900.px)
        property("margin", "0 auto")
        padding(72.px, 24.px, 40.px)
    }
    val tag by style {
        color(accentColor)
        fontSize(12.px)
        fontWeight(700)
        letterSpacing(0.14.em)
    }
    val hero by style {
        margin(18.px, 0.px, 14.px)
        property("font-size", "clamp(48px, 8vw, 82px)")
        lineHeight("0.95")
        letterSpacing((-0.05).em)
    }
    val accent by style {
        color(accentColor)
    }
    val lead by style {
        maxWidth(600.px)
        color(Color("#aab2c8"))
        fontSize(18.px)
        lineHeight("1.6")
    }
    val grid by style {
        display(DisplayStyle.Grid)
        gridTemplateColumns("repeat(auto-fit, minmax(180px, 1fr))")
        gap(14.px)
        margin(40.px, 0.px)
    }
    val card by style {
        padding(22.px)
        backgroundColor(surface)
        border(1.px, LineStyle.Solid, borderColor)
        borderRadius(18.px)
    }
    val icon by style {
        display(DisplayStyle.Flex)
        width(38.px)
        height(38.px)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        color(accentColor)
        border(1.px, LineStyle.Solid, accentColor)
        borderRadius(10.px)
        fontFamily("ui-monospace", "monospace")
        fontSize(20.px)
        fontWeight(700)
        lineHeight("1")
    }
    val cardTitle by style {
        margin(20.px, 0.px, 8.px)
        fontSize(16.px)
    }
    val cardText by style {
        margin(0.px)
        color(mutedText)
        fontSize(14.px)
        lineHeight("1.5")
    }
    val code by style {
        overflowX("auto")
        padding(22.px)
        backgroundColor(Color("#05070d"))
        property("border-left", "4px solid $accentColor")
        borderRadius(8.px)
        lineHeight("1.6")
    }
    val footer by style {
        display(DisplayStyle.Block)
        marginTop(18.px)
        color(Color("#66708a"))
        textAlign("center")
    }

    init {
        "body" style {
            margin(0.px)
            backgroundColor(background)
            color(text)
            fontFamily("Inter", "ui-sans-serif", "system-ui", "sans-serif")
        }
    }
}

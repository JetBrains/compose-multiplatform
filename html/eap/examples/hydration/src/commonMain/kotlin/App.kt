import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

private data class Feature(val symbol: String, val title: String, val text: String)

private val features = listOf(
    Feature("◐", "Useful before JavaScript", "The server response already contains the experience."),
    Feature("◎", "DOM stays put", "Hydration adopts existing nodes, then adds interactivity."),
    Feature("↗", "State crosses the boundary", "Public initial state travels beside the markup."),
)

private val codeExample = """
    // Common, shared
    @Composable
    fun HydrationDemo(initialCount: Int) {
        Main {
            Counter(initialCount)
        }
    }

    @Composable
    fun Counter(initialCount: Int) {
        var count by remember(initialCount) { mutableStateOf(initialCount) }

        Article {
            Div(attrs = { classes(PageStyles.counter) }) {
                Div {
                    Text(count.toString())
                }
                Button(attrs = {
                    type(ButtonType.Button)
                    onClick { count++ }
                }) { Text("+ Send a signal") }
            }
        }
    }
    
    // JVM
    renderHydratedDocument {
        Html(attrs = { lang("en") }) {
            Body {
                HydrationRoot(42, Int::toString) { initialCount ->
                    HydrationDemo(initialCount)
                }
                Script(attrs = { type(ScriptType.Module); src("app.js") })
            }
        }
    }

    // Browser
    hydrateRoot(String::toInt) { initialCount ->
        HydrationDemo(initialCount)
    }
""".trimIndent()

@Composable
fun HydrationDemo(initialCount: Int) {
    Main(attrs = { classes(PageStyles.page) }) {
        Span(attrs = { classes(PageStyles.tag) }) { Text("COMPOSE HTML · HYDRATION") }
        H1(attrs = { classes(PageStyles.hero) }) {
            Text("HTML now. ")
            Span(attrs = { classes(PageStyles.accent) }) { Text("Interaction next.") }
        }
        P(attrs = { classes(PageStyles.lead) }) {
            Text("Render on the server, then bring the same DOM to life.")
        }

        Counter(initialCount)

        Div(attrs = { classes(PageStyles.grid) }) {
            features.forEach { feature ->
                Article(attrs = { classes(PageStyles.card) }) {
                    Span(attrs = { classes(PageStyles.icon) }) { Text(feature.symbol) }
                    H2 { Text(feature.title) }
                    P { Text(feature.text) }
                }
            }
        }

        H2(attrs = { classes(PageStyles.codeTitle) }) { Text("The interactive hand-off") }
        Pre(attrs = { classes(PageStyles.code) }) { Code { Text(codeExample) } }
        Small(attrs = { classes(PageStyles.footer) }) {
            Text("First rendered on the JVM · Hydrated in the browser · Shared UI")
        }
    }
}

@Composable
private fun Counter(initialCount: Int) {
    var count by remember(initialCount) { mutableStateOf(initialCount) }

    Article(attrs = {
        classes(PageStyles.demo)
        if (count != initialCount) {
            classes(if (count % 2 == 0) PageStyles.bloomEven else PageStyles.bloomOdd)
        }
    }) {
        Div(attrs = { classes(PageStyles.counter) }) {
            Div {
                B(attrs = { classes(PageStyles.number) }) { Text(count.toString()) }
                Span(attrs = { classes(PageStyles.counterLabel) }) {
                    Text("The answer to life, the universe, and everything")
                }
            }
            Button(attrs = {
                classes(PageStyles.button)
                type(ButtonType.Button)
                onClick { count++ }
            }) { Text("+ Disturb the universe") }
        }
    }
}

object PageStyles : StyleSheet() {
    private val ink = Color("#f7f8fc")
    private val muted = Color("#9ba6bd")
    private val lime = Color("#b8ff65")
    private val cyan = Color("#72e6ff")
    private val surface = Color("#111827")
    private val lineColor = Color("#263248")

    val page by style {
        maxWidth(900.px); property("margin", "0 auto"); padding(72.px, 24.px, 40.px)
    }
    val tag by style {
        color(lime); fontSize(12.px); fontWeight(700); letterSpacing(0.14.em)
    }
    val hero by style {
        margin(18.px, 0.px, 14.px); property("font-size", "clamp(46px, 8vw, 80px)")
        lineHeight("0.95"); letterSpacing((-0.05).em)
    }
    val accent by style { color(lime) }
    val lead by style {
        maxWidth(650.px); color(Color("#b3bdd1")); fontSize(18.px); lineHeight("1.6")
    }
    val demo by style {
        margin(40.px, 0.px, 18.px); padding(30.px); borderRadius(22.px)
        border(1.px, LineStyle.Solid, lineColor)
        background("radial-gradient(circle at 90% 0%, #18333d, $surface 45%)")
    }
    val boxBloomOdd by keyframes {
        from { property("transform", "translateY(0) scale(1)"); property("box-shadow", "0 0 0 transparent") }
        36.percent { property("transform", "translateY(-3px) scale(1.006)"); property("box-shadow", "0 16px 48px -16px #72e6ff38") }
        to { property("transform", "translateY(0) scale(1)"); property("box-shadow", "0 0 0 transparent") }
    }
    val boxBloomEven by keyframes {
        from { property("transform", "translateY(0) scale(1)"); property("box-shadow", "0 0 0 transparent") }
        36.percent { property("transform", "translateY(-3px) scale(1.006)"); property("box-shadow", "0 16px 48px -16px #b8ff6538") }
        to { property("transform", "translateY(0) scale(1)"); property("box-shadow", "0 0 0 transparent") }
    }
    val bloomOdd by style { animation(boxBloomOdd) { duration(780.ms); timingFunction(AnimationTimingFunction.cubicBezier(0.4, 0.0, 0.2, 1.0)) } }
    val bloomEven by style { animation(boxBloomEven) { duration(780.ms); timingFunction(AnimationTimingFunction.cubicBezier(0.4, 0.0, 0.2, 1.0)) } }
    val counter by style {
        display(DisplayStyle.Flex); alignItems(AlignItems.End); justifyContent(JustifyContent.SpaceBetween)
        gap(20.px)
    }
    val number by style { display(DisplayStyle.Block); color(lime); fontSize(52.px); lineHeight("1") }
    val counterLabel by style { display(DisplayStyle.Block); marginTop(7.px); color(muted); fontSize(13.px) }
    val button by style {
        padding(13.px, 18.px); color(Color("#071006")); backgroundColor(lime)
        border(0.px); borderRadius(10.px); fontWeight(800); cursor("pointer")
        property("transition", "transform .15s, box-shadow .15s")
    }
    val grid by style {
        display(DisplayStyle.Grid); gridTemplateColumns("repeat(auto-fit, minmax(210px, 1fr))")
        gap(14.px); marginBottom(44.px)
    }
    val card by style {
        padding(20.px); backgroundColor(surface); border(1.px, LineStyle.Solid, lineColor)
        borderRadius(16.px)
        self + " h2" style { margin(18.px, 0.px, 8.px); fontSize(16.px) }
        self + " p" style { margin(0.px); color(muted); fontSize(14.px); lineHeight("1.5") }
    }
    val icon by style {
        display(DisplayStyle.Flex); width(38.px); height(38.px)
        alignItems(AlignItems.Center); justifyContent(JustifyContent.Center)
        color(cyan); border(1.px, LineStyle.Solid, cyan); borderRadius(10.px)
        fontFamily("ui-monospace", "monospace"); fontSize(20.px); fontWeight(700); lineHeight("1")
    }
    val codeTitle by style { marginBottom(14.px) }
    val code by style {
        overflowX("auto"); padding(22.px); backgroundColor(Color("#05070d"))
        property("border-left", "4px solid $cyan"); borderRadius(8.px); lineHeight("1.6")
    }
    val footer by style {
        display(DisplayStyle.Block); marginTop(18.px); color(Color("#69748c")); textAlign("center")
    }

    init {
        "body" style {
            margin(0.px); color(ink); backgroundColor(Color("#090d18"))
            fontFamily("Inter", "ui-sans-serif", "system-ui", "sans-serif")
        }
        ".$button:hover" style {
            property("transform", "translateY(-2px)")
            property("box-shadow", "0 10px 30px #b8ff6533")
        }
        ".$button:active" style { property("transform", "translateY(1px) scale(.96)") }
    }
}

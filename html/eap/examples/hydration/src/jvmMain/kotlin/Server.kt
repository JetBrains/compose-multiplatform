import org.jetbrains.compose.web.HydrationRoot
import org.jetbrains.compose.web.attributes.ScriptType
import org.jetbrains.compose.web.attributes.src
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderHydratedDocument
import java.io.File

fun main(args: Array<String>) {
    val output = File(args.single())
    output.parentFile.mkdirs()
    output.writeText(renderHydratedDocument {
        Html(attrs = { lang("en") }) {
            Head {
                Meta(attrs = { attr("charset", "utf-8") })
                Meta(attrs = {
                    attr("name", "viewport")
                    attr("content", "width=device-width, initial-scale=1")
                })
                Title { Text("Compose HTML hydration") }
                Style(PageStyles)
            }
            Body {
                HydrationRoot(42, Int::toString) { initialCount ->
                    HydrationDemo(initialCount)
                }
                Script(attrs = { type(ScriptType.Module); src("app.js") })
            }
        }
    })
    println("Rendered ${output.toURI()}")
}

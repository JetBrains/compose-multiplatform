
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.html.respondHtml
import io.ktor.http.content.resource
import io.ktor.http.content.static
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.script
import kotlinx.html.title

var jsFileName = "web-deploy-getting-started.js"
var jsFileMapName = "$jsFileName.map"

fun Application.main() {

    install(ContentNegotiation)

    routing {

        get("/") {
            call.respondHtml {
                head {
                    title("web-deploy-getting-started")
                }
                body {
                    div { id = "root" } // Required by Compose Web
                    script(src = "/static/$jsFileName") {}
                }
            }
        }

        static("/static") {
            resource(jsFileName)
            resource(jsFileMapName)
        }
    }
}

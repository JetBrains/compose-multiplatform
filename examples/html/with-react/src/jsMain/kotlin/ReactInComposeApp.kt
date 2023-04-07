import androidx.compose.runtime.*
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import react.RBuilder
import react.dom.render
import react.dom.unmountComponentAtNode

/**
 * @param key - when UseReactEffect is invoked with a new [key], compose forces react to render with a new content.
 * @param content - the builder for the content managed by React
 */
@Composable
private fun ElementScope<HTMLElement>.UseReactEffect(
    key: Any?,
    content: RBuilder.() -> Unit
) {
    DisposableEffect(key) {
        render(scopeElement) {
            content()
        }
        onDispose {  }
    }

    DisposableEffect(Unit) {
        onDispose {
            unmountComponentAtNode(scopeElement)
        }
    }
}

@Composable
fun YoutubeReactPlayerWrapper(videoUrl: String) {
    if (videoUrl.isEmpty()) return
    Div({
        style {
            width(50.percent)
        }
    }) {
        UseReactEffect(key = videoUrl) {
            reactPlayer {
                attrs.url = videoUrl
            }
        }
    }
}

private val videos = listOf(
    "https://www.youtube.com/watch?v=UryyHq45Y_8",
    "https://www.youtube.com/watch?v=698I_AH8h6s",
    "https://www.youtube.com/watch?v=F8jj7e-_jFA"
)

fun reactInComposeAppExample() {
    var videoUrl by mutableStateOf("")

    renderComposable(rootElementId = "root") {

        A(href = "${window.location.origin}?app=reactApp") { Text("GO TO COMPOSE IN REACT EXAMPLE") }

        Div {
            videos.forEachIndexed { ix, url ->
                Button(
                    attrs = {
                        onClick { videoUrl = url }
                        style  {
                            margin(10.px)
                        }
                    }
                ) { Text("Video ${ix + 1}") }
            }

            Button(
                attrs = {
                    onClick {
                        videoUrl = ""
                        style {
                            margin(10.px)
                        }
                    }
                },
            ) { Text("Reset") }

            YoutubeReactPlayerWrapper(videoUrl)
        }
    }
}

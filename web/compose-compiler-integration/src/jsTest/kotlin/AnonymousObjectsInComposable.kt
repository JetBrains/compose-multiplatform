import androidx.compose.runtime.Composable
import kotlinx.browser.document
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposableInBody
import kotlin.test.Test
import kotlin.test.assertEquals

class AnonymousObjectsInComposable {

    @Test
    // Issue: content.Abc$composable_z540rc_k$ is not a function
    // https://github.com/JetBrains/compose-jb/issues/2549
    fun passingFunctionReference() {
        renderComposableInBody {
            val content: HasComposable2 = createHasComposable()
            content.Abc()
        }

        assertEquals("<div>Abc</div>", document.body!!.firstElementChild!!.outerHTML)
    }
}

@Composable
internal fun createHasComposable(): HasComposable2 {
    return object : HasComposable2 {
        @Composable
        override fun Abc() {
            Div {
                Text("Abc")
            }
        }
    }
}

internal interface HasComposable2 {
    @Composable
    fun Abc()
}
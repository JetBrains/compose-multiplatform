import androidx.compose.runtime.Composable
import kotlinx.browser.document
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposableInBody
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposableLambdaCalls {

    @Composable
    private fun ComposableSomeText(someText : () -> String) {
        Text(someText())
    }

    private fun someText(): String {
        return "SomeText"
    }

    @Test
    fun passingFunctionReference() {
        renderComposableInBody {
            Div {
                ComposableSomeText(::someText)
            }
        }

        assertEquals("<div>SomeText</div>", document.body!!.firstElementChild!!.outerHTML)
    }

    @Test
    fun passingAnonymousLambda() {
        renderComposableInBody {
            Div {
                ComposableSomeText { "Text1" }
            }
        }

        assertEquals("<div>Text1</div>", document.body!!.firstElementChild!!.outerHTML)
    }

    @Test
    fun callingComposableLambdaWithoutArguments() {
        val l: @Composable () -> Unit = {
            Text("TextA")
        }

        renderComposableInBody {
            Div {
                l()
            }
        }

        assertEquals("<div>TextA</div>", document.body!!.firstElementChild!!.outerHTML)
    }

    @Test
    fun invokingNullComposableLambdaWithoutArguments() {
        val l: (@Composable () -> Unit)? = null

        renderComposableInBody {
            Div {
                l?.invoke()
            }
        }

        assertEquals("<div></div>", document.body!!.firstElementChild!!.outerHTML)
    }

    @Test
    fun invokingNullComposableLambdaWithArguments() {
        val l: (@Composable (Int) -> Unit)? = null

        var someIntInvoked = false

        fun someInt(): Int {
            someIntInvoked = true
            return 10
        }

        renderComposableInBody {
            Div {
                l?.invoke(someInt())
            }
        }

        assertEquals("<div></div>", document.body!!.firstElementChild!!.outerHTML)
        assertEquals(false, someIntInvoked, message = "someInt() should never be invoked as `l` is null")
    }

    @Test
    fun invokingComposableLambdaWithArguments() {
        val l: (@Composable (Int) -> Unit) = {
            Text("Text$it")
        }

        var someIntInvoked = false

        fun someInt(): Int {
            someIntInvoked = true
            return 10
        }

        renderComposableInBody {
            Div {
                l.invoke(someInt())
            }
        }

        assertEquals("<div>Text10</div>", document.body!!.firstElementChild!!.outerHTML)
        assertEquals(true, someIntInvoked)
    }

    @Test
    fun invokingComposableLambdaWithFunctionReferenceAsArgument() {
        val l: (@Composable (() -> String) -> Unit) = {
            Text("Text-${it()}")
        }

        renderComposableInBody {
            Div {
                l.invoke(::someText)
            }
        }

        assertEquals("<div>Text-SomeText</div>", document.body!!.firstElementChild!!.outerHTML)
    }

    @Test
    fun invokingComposableLambdaFromAnotherModule() {
        renderComposableInBody {
            Div {
                GlobalComposableLambdaToShowText {
                    "SuperText"
                }
            }
        }

        assertEquals("<div>SuperText</div>", document.body!!.firstElementChild!!.outerHTML)
    }
}

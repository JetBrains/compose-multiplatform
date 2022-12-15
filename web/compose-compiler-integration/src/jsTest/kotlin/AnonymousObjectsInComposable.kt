import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    fun testComposableInAnonymousObject() {
        renderComposableInBody {
            val content: HasComposable2 = createHasComposable()
            content.Abc()
        }

        assertEquals("<div>Abc</div>", document.body!!.firstElementChild!!.outerHTML)
    }

    @Test
    // Issue:
    // java.lang.IllegalArgumentException: Could not find local implementation for Abc$composable
    // at androidx.compose.compiler.plugins.kotlin.lower.decoys.DecoyTransformBase$DefaultImpls.getComposableForDecoy(DecoyTransformBase.kt:110)
    fun testLocalClassWithComposable() {
        renderComposableInBody {
            HasLocalClassWithComposable()
        }

        assertEquals("<div>Abc2</div>", document.body!!.firstElementChild!!.outerHTML)
    }

    @Test
    // Issue:
    // abc3.Abc$composable_z540rc_k$ is not a function
    fun testConstructorWithComposable() {
        renderComposableInBody {
            TestConstructor { return@TestConstructor 111 }.otherComposable!!.invoke()
        }

        assertEquals("<div>Abc223-111</div>", document.body!!.firstElementChild!!.outerHTML)
    }
}

@Composable
internal fun HasLocalClassWithComposable() {
    class Abc : HasComposable2 {
        @Composable
        override fun Abc() {
            Div { Text("Abc2") }
        }
    }

    val abc = remember { Abc() }
    abc.Abc()
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

class TestConstructor constructor() {

    var otherComposable: (@Composable () -> Unit)? = null
    constructor(retInt: @Composable () -> Int): this() {
        otherComposable = {
            val abc3: HasComposable2 = object : HasComposable2 {
                @Composable
                override fun Abc() {
                    Div {
                        val i = retInt()
                        Text("Abc223-$i")
                    }
                }
            }
            abc3.Abc()
        }
    }
}
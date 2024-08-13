import androidx.compose.runtime.Composable
import com.example.common.TextContainerNode
import com.example.common.TextLeafNode
import com.example.common.composeText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class Tests {

    @Test
    fun passingFunctionReference() = runTest {
        val root = composeText {
            ComposableSomeText(::someText)
        }

        assertEquals("root:{SomeText}", root.dump())
    }

    @Test
    fun passingAnonymousLambda() = runTest {
        val root = composeText {
            ComposableSomeText { "passingAnonymousLambda" }
        }

        assertEquals("root:{passingAnonymousLambda}", root.dump())
    }

    @Test
    fun callingComposableLambdaWithoutArguments() {
        val l: @Composable () -> Unit = {
            TextLeafNode("TextA")
        }

        val root = composeText {
            l()
        }

        assertEquals("root:{TextA}", root.dump())
    }

    @Test
    fun invokingNullComposableLambdaWithoutArguments() {
        val l: (@Composable () -> Unit)? = null

        val root = composeText {
            l?.invoke()
        }

        assertEquals("root:{}", root.dump())
    }

    @Test
    fun invokingNullComposableLambdaWithArguments() {
        val l: (@Composable (Int) -> Unit)? = null

        var someIntInvoked = false

        fun someInt(): Int {
            someIntInvoked = true
            return 10
        }

        val root = composeText {
            l?.invoke(someInt())
        }
        assertEquals("root:{}", root.dump())
        assertFalse(someIntInvoked)
    }

    @Test
    fun invokingComposableLambdaWithArguments() {
        val l: (@Composable (Int) -> Unit) = {
            TextLeafNode("Text$it")
        }

        var someIntInvoked = false

        fun someInt(): Int {
            someIntInvoked = true
            return 2023
        }

        val root = composeText {
            l.invoke(someInt())
        }

        assertEquals("root:{Text2023}", root.dump())
        assertTrue(someIntInvoked)
    }

    @Test
    fun invokingComposableLambdaWithFunctionReferenceAsArgument() {
        val l: (@Composable (() -> String) -> Unit) = {
            TextLeafNode("Text-${it()}")
        }

        val root = composeText {
            l(::someText)
        }

        assertEquals("root:{Text-SomeText}", root.dump())
    }

    @Test
    fun testComposableGetter() {
        val root = composeText {
            TextLeafNode("Value = $composableInt")
        }
        assertEquals("root:{Value = 100}", root.dump())
    }

    @Test
    fun testComposableAlwaysReturnsNull() {
        val root = composeText {
            val v = ComposableAlwaysReturnsNull()
            TextLeafNode("Value = ${v ?: "null"}")
        }
        assertEquals("root:{Value = null}", root.dump())
    }

    @Test
    fun testComposableAlwaysReturnsNullUnit() {
        val root = composeText {
            val v = ComposableAlwaysReturnsNullUnit()
            TextLeafNode("Value = ${v ?: "null"}")
        }
        assertEquals("root:{Value = null}", root.dump())
    }

    @Test
    fun testComposableAlwaysReturnsUnit() {
        val root = composeText {
            val v = ComposableAlwaysReturnsUnit().let {
                if (it == Unit) "Unit" else it.toString()
            }
            TextLeafNode("Value = $v")
        }
        assertEquals("root:{Value = Unit}", root.dump())
    }
}

private fun someText(): String {
    return "SomeText"
}

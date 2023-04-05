import androidx.compose.runtime.*
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.promise
import kotlinx.coroutines.yield
import kotlinx.dom.appendElement
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.compose.web.renderComposableInBody
import org.w3c.dom.*
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the issues:
 * - https://github.com/JetBrains/compose-jb/issues/2539
 * - https://github.com/JetBrains/compose-jb/issues/2535
 */
class StabilityTests {

    private fun runBlockingTest(block: suspend CoroutineScope.() -> Unit) = MainScope().promise {
        block()
    }

    @AfterTest
    fun reset() {
        counter = 0
    }

    @Test
    fun test_TakeStableSealedInterface() = runBlockingTest {
        var scope: RecomposeScope? = null
        var recomposeCounter = 0

        val state = mutableStateOf<StableSealedInterface>(StableSealedInterface.A)
        renderComposableInBody {
            scope = currentRecomposeScope
            TakeStableSealedInterface(state.value)
            recomposeCounter++
        }

        assertEquals(1, counter)
        assertEquals(1, recomposeCounter)

        scope!!.invalidate()
        while (recomposeCounter < 2) yield()
        assertEquals(1, counter)
        assertEquals(2, recomposeCounter)

        scope!!.invalidate()
        while (recomposeCounter < 3) yield()
        assertEquals(1, counter)
        assertEquals(3, recomposeCounter)

        state.value = StableSealedInterface.B
        while (recomposeCounter < 4) yield()
        assertEquals(2, counter)
        assertEquals(4, recomposeCounter)
    }

    @Test
    fun test_TakeStableDataClass() = runBlockingTest {
        var scope: RecomposeScope? = null
        var recomposeCounter = 0

        val state = mutableStateOf(StableDataClass(100))
        renderComposableInBody {
            scope = currentRecomposeScope
            TakeStableDataClass(state.value)
            recomposeCounter++
        }

        assertEquals(1, counter)
        assertEquals(1, recomposeCounter)

        scope!!.invalidate()
        while (recomposeCounter < 2) yield()
        assertEquals(1, counter)
        assertEquals(2, recomposeCounter)

        scope!!.invalidate()
        while (recomposeCounter < 3) yield()
        assertEquals(1, counter)
        assertEquals(3, recomposeCounter)

        state.value = StableDataClass(200)
        while (recomposeCounter < 4) yield()
        assertEquals(2, counter)
        assertEquals(4, recomposeCounter)
    }

    @Test
    fun test_TakeStableClass() = runBlockingTest {
        var scope: RecomposeScope? = null
        var recomposeCounter = 0

        val state = mutableStateOf(StableClass(1000))
        renderComposableInBody {
            scope = currentRecomposeScope
            TakeStableClass(state.value)
            recomposeCounter++
        }

        assertEquals(1, counter)
        assertEquals(1, recomposeCounter)

        scope!!.invalidate()
        while (recomposeCounter < 2) yield()
        assertEquals(1, counter)
        assertEquals(2, recomposeCounter)

        scope!!.invalidate()
        while (recomposeCounter < 3) yield()
        assertEquals(1, counter)
        assertEquals(3, recomposeCounter)

        state.value = StableClass(300)
        while (recomposeCounter < 4) yield()
        assertEquals(2, counter)
        assertEquals(4, recomposeCounter)
    }

    @Test
    fun test_TakeTakeStableTypedClass_String() = runBlockingTest {
        var scope: RecomposeScope? = null
        var recomposeCounter = 0

        val state = mutableStateOf(StableTypedClass("1000"))
        renderComposableInBody {
            scope = currentRecomposeScope
            TakeStableTypedClass(state.value)
            recomposeCounter++
        }

        assertEquals(1, counter)
        assertEquals(1, recomposeCounter)

        scope!!.invalidate()
        while (recomposeCounter < 2) yield()
        assertEquals(1, counter)
        assertEquals(2, recomposeCounter)

        scope!!.invalidate()
        while (recomposeCounter < 3) yield()
        assertEquals(1, counter)
        assertEquals(3, recomposeCounter)

        state.value = StableTypedClass("300")
        while (recomposeCounter < 4) yield()
        assertEquals(2, counter)
        assertEquals(4, recomposeCounter)
    }

    @Test
    fun test_TakeTakeStableTypedClass2_String() = runBlockingTest {
        var scope: RecomposeScope? = null
        var recomposeCounter = 0

        val state = mutableStateOf(StableTypedClass("1500"))
        renderComposableInBody {
            scope = currentRecomposeScope
            TakeStableTypedClass2(state.value)
            recomposeCounter++
        }

        assertEquals(1, counter)
        assertEquals(1, recomposeCounter)

        scope!!.invalidate()
        while (recomposeCounter < 2) yield()
        assertEquals(1, counter)
        assertEquals(2, recomposeCounter)

        scope!!.invalidate()
        while (recomposeCounter < 3) yield()
        assertEquals(1, counter)
        assertEquals(3, recomposeCounter)

        state.value = StableTypedClass("3020")
        while (recomposeCounter < 4) yield()
        assertEquals(2, counter)
        assertEquals(4, recomposeCounter)
    }


    @Test // issue https://github.com/JetBrains/compose-jb/issues/2535
    fun test_remembers_correct_attrs() = runBlockingTest {
        val root = document.body!!.appendElement("div") {
            id = "root"
        }
        renderComposable(root) {
            val words = remember { mutableStateListOf<String>() }
            words.map { P { Text(it) }}
            Button(attrs = { onClick { words.add("foo") } }) { Text("foo") }
            Button(attrs = { onClick { words.add("abc") } }) { Text("abc") }
            Button(attrs = { onClick { words.add("bar") } }) { Text("bar") }
        }

        fun fooButton() = root.children.asList().let { it[it.size - 3] } as HTMLElement
        fun abcButton() = root.children.asList().let { it[it.size - 2] } as HTMLElement
        fun barButton() = root.children.asList().let { it[it.size - 1] } as HTMLElement
        fun lastPText() = root.children.asList().let { it[it.size - 4] } as HTMLElement

        assertEquals(3, root.children.length)

        repeat(3) {
            fooButton().click()
            while (root.children.length < 4 + it) yield()
            assertEquals("foo", lastPText().innerText)
        }
        repeat(3) {
            abcButton().click()
            while (root.children.length < 7 + it) yield()
            assertEquals("abc", lastPText().innerText)
        }
        repeat(3) {
            barButton().click()
            while (root.children.length < 10 + it) yield()
            assertEquals("bar", lastPText().innerText)
        }

        val result = root.children.asList().take(9).map { it.innerHTML }.joinToString(",")
        assertEquals("foo,foo,foo,abc,abc,abc,bar,bar,bar", result)

        document.body!!.removeChild(root)
    }
}

private var counter = 0

@Composable
private fun TakeStableSealedInterface(a: StableSealedInterface) {
    println("A = $a")
    counter++
}

@Composable
private fun TakeStableDataClass(a: StableDataClass) {
    println("A = $a")
    counter++
}

@Composable
private fun TakeStableClass(a: StableClass) {
    println("A = $a")
    counter++
}
@Composable
private fun TakeStableTypedClass(a: StableTypedClass<String>) {
    println("A = $a")
    counter++
}

@Composable
private fun <T> TakeStableTypedClass2(a: StableTypedClass<T>) {
    println("A = $a")
    counter++
}

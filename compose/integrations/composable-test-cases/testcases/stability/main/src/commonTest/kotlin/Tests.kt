import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.example.common.TextLeafNode
import com.example.common.composeText
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class Tests {

    /**
     *  Here we use an unstable parameter, and therefore
     *  we expect the Composable function will NOT skip body execution.
     */
    @Ignore
    @Test
    fun testUnstableParameter() = runTest {
        val i = UnstableDataClassWithPrivateVar(0)
        val job = Job()

        val state = mutableStateOf(0)
        val root = composeText(coroutineContext + job) {
            UseUnstableDataClassInstance(i)
            TextLeafNode("state=" + state.value.toString())
        }

        assertEquals("root:{UnstableDataClassWithPrivateVar(i=0), state=0}", root.dump())
        assertEquals(1, i.getI())
        state.value += 1

        testScheduler.advanceUntilIdle()
        assertEquals("root:{UnstableDataClassWithPrivateVar(i=1), state=1}", root.dump())
        assertEquals(2, i.getI())
    }

    @Ignore
    @Test
    fun testUnstableParameterOfLocalType() = runTest {
        val i = LocalUnstableDataClassWithPrivateVar(0)
        val job = Job()

        val state = mutableStateOf(0)
        val root = composeText(coroutineContext + job) {
            UseLocalUnstableDataClassWithPrivateVar(i)
            TextLeafNode("state=" + state.value.toString())
        }

        assertEquals("root:{LocalUnstableDataClassWithPrivateVar(i=0), state=0}", root.dump())
        assertEquals(1, i.getI())
        state.value += 1

        testScheduler.advanceUntilIdle()
        assertEquals("root:{LocalUnstableDataClassWithPrivateVar(i=1), state=1}", root.dump())
        assertEquals(2, i.getI())
    }

    @Test
    fun testStableParameter() = runTest {
        val i = StableDataClassWithPrivateVal(0)
        val job = Job()

        val state = mutableStateOf(0)
        val root = composeText(coroutineContext + job) {
            UseStableDataClassWithPrivateVar(i)
            TextLeafNode("state=" + state.value.toString())
        }

        assertEquals("root:{StableDataClassWithPrivateVal(i=0), counter=0, state=0}", root.dump())
        assertEquals(1, counter)

        state.value += 1
        testScheduler.advanceUntilIdle()
        assertEquals("root:{StableDataClassWithPrivateVal(i=0), counter=0, state=1}", root.dump())
        assertEquals(1, counter)
    }

    @BeforeTest
    fun before() {
        counter = 0
    }
}

private var counter = 0

@Composable
fun UseUnstableDataClassInstance(i: UnstableDataClassWithPrivateVar) {
    TextLeafNode(i.toString())
    i.inc()
}

@Composable
fun UseStableDataClassWithPrivateVar(i: StableDataClassWithPrivateVal) {
    TextLeafNode(i.toString())
    TextLeafNode("counter=$counter")
    counter++
}


/**
 * Same as [UnstableDataClassWithPrivateVar] but defined in the same module that a function which uses it
 */
data class LocalUnstableDataClassWithPrivateVar(private var i: Int) {
    fun inc() { i++ }
    fun getI() = i
}

@Composable
fun UseLocalUnstableDataClassWithPrivateVar(i: LocalUnstableDataClassWithPrivateVar) {
    TextLeafNode(i.toString())
    i.inc()
}

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import benchmark.AddItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
class BenchmarkTests {

    private fun flowRepeat(count: Int, calculate: suspend () -> Duration): Flow<Duration> {
        return flow {
            repeat(count) {
                emit(calculate())
            }
        }
    }

    private fun runBenchmark(
        name: String,
        repeat: Int = 5,
        run: suspend TestScope.() -> Duration
    ) = runTest {
        val durations = flowRepeat(count = repeat) {
            run()
        }.toList()

        val avgMs = durations.map { it.toInt(DurationUnit.MILLISECONDS) }.average()

        println("#$name:$avgMs;")
    }

    @Test
    fun add1000Items() = runBenchmark("add1000Items") {
        val addItemsCount = mutableStateOf(0)

        val composition = composition {
            AddItems(addItemsCount.value)
        }

        assertEquals(0, root.childElementCount)

        val duration = measureTime {
            addItemsCount.value = 1000
            waitForAnimationFrame()
        }

        assertEquals(1000, root.childElementCount)
        composition.dispose()

        duration
    }

    @Test
    fun remove1000Items() = runBenchmark("remove1000Items") {
        val addItemsCount = mutableStateOf(1000)

        val composition = composition {
            AddItems(addItemsCount.value)
        }

        assertEquals(1000, root.childElementCount)

        val duration = measureTime {
            addItemsCount.value = 0
            waitForAnimationFrame()
        }

        assertEquals(0, root.childElementCount)
        composition.dispose()

        duration
    }

    @Test
    fun changeEvery10thItem() = runBenchmark("changeEvery10thItem") {
        val items = mutableStateListOf<String>()
        items.addAll(generateSequence(0) { it + 1 }.map { it.toString() }.take(1000))

        val composition = composition {
            AddItems(items)
        }

        assertEquals(1000, root.childElementCount)
        assertEquals("1", root.children[1]!!.firstChild!!.textContent)
        assertEquals("10", root.children[10]!!.firstChild!!.textContent)

        val duration = measureTime {
            repeat(items.size) {
                if (it % 10 == 0) items[it] = "${items[it]}-$it"
            }
            waitForAnimationFrame()
        }

        assertEquals(1000, root.childElementCount)
        assertEquals("1", root.children[1]!!.firstChild!!.textContent)
        assertEquals("10-10", root.children[10]!!.firstChild!!.textContent)

        composition.dispose()

        duration
    }
}
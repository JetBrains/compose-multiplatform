package org.jetbrains.compose.web.tests.benchmarks

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi
import org.jetbrains.compose.web.testutils.TestScope
import org.jetbrains.compose.web.testutils.runTest
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
@ComposeWebExperimentalTestsApi
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

        val avgMs = durations.map { it.toInt(DurationUnit.MILLISECONDS) }.average().toInt()

        val browserName = window.navigator.userAgent.lowercase().let {
            when {
                it.contains("chrome") -> "chrome"
                it.contains("firefox") -> "firefox"
                else -> "unknown_browser"
            }
        }

        println("#$name[$browserName]:$avgMs;")
    }

    private suspend fun TestScope.addNItems(n: Int): Duration {
        val addItemsCount = mutableStateOf(0)

        composition {
            AddItems(addItemsCount.value)
        }

        assertEquals(0, root.childElementCount)

        val duration = measureTime {
            addItemsCount.value = n
            waitForRecompositionComplete()
        }

        assertEquals(n, root.childElementCount)

        return duration
    }

    @Test // add1kItems overrides default `repeat` value (was - 5, now - 2) to avoid getting swallowed on CI
    fun add1kItems() = runBenchmark(name = "add1000Items", repeat = 2) {
        addNItems(1000)
    }

    @Test
    fun add100Items() = runBenchmark("add100Items") {
        addNItems(100)
    }

    @Test
    fun add200Items() = runBenchmark("add200Items") {
        addNItems(200)
    }

    @Test
    fun add500Items() = runBenchmark("add500Items") {
        addNItems(500)
    }

    @Test
    fun remove1000Items() = runBenchmark("remove1000Items") {
        val addItemsCount = mutableStateOf(1000)

        composition {
            AddItems(addItemsCount.value)
        }

        assertEquals(1000, root.childElementCount)

        val duration = measureTime {
            addItemsCount.value = 0
            waitForRecompositionComplete()
        }

        assertEquals(0, root.childElementCount)

        duration
    }

    @Test
    fun changeEvery10thItem() = runBenchmark("changeEvery10thItem") {
        val items = mutableStateListOf<String>()
        items.addAll(generateSequence(0) { it + 1 }.map { it.toString() }.take(1000))

        composition {
            AddItems(items)
        }

        assertEquals(1000, root.childElementCount)
        assertEquals("1", root.children[1]!!.firstChild!!.textContent)
        assertEquals("10", root.children[10]!!.firstChild!!.textContent)

        val duration = measureTime {
            repeat(items.size) {
                if (it % 10 == 0) items[it] = "${items[it]}-$it"
            }
            waitForRecompositionComplete()
        }

        assertEquals(1000, root.childElementCount)
        assertEquals("1", root.children[1]!!.firstChild!!.textContent)
        assertEquals("10-10", root.children[10]!!.firstChild!!.textContent)

        duration
    }
}

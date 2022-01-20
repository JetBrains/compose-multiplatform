import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalCoroutinesApi::class)
class RunAddNItems {
    /**
     * Notes:
     * These experiments show much higher numbers on macos.
     * But if same experiments run as an app - runReleaseExecutableMacos...,
     * then it shows much better results.
     */

    @OptIn(ExperimentalTime::class)
    @Test
    fun add10ComposableItems() = _runTest {
        addNComposableItems(10)
    }

    @Test
    fun add100ComposableItems() = _runTest {
        addNComposableItems(100)
    }

    @Test
    fun add500ComposableItems() = _runTest {
        addNComposableItems(500)
    }

    @Test
    fun add1kComposableItems() = _runTest {
        addNComposableItems(1000)
    }

    @Test
    fun add2kComposableItems() = _runTest {
        addNComposableItems(2000)
    }
}

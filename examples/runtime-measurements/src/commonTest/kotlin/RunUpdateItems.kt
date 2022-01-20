import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class RunUpdateItems {
    /**
     * Notes:
     * These experiments show much higher numbers on macos.
     * But if same experiments run as an app - runReleaseExecutableMacos...,
     * then it shows much better results.
     */

    @OptIn(ExperimentalTime::class)
    @Test
    fun update10ComposableItems() = _runTest {
        updateEveryXth(preAddedItemsCount = 10)
    }

    @Test
    fun update100ComposableItems() = _runTest {
        updateEveryXth(preAddedItemsCount = 100)
    }

    @Test
    fun update500ComposableItems() = _runTest {
        updateEveryXth(preAddedItemsCount = 500)
    }

    @Test
    fun update1kComposableItems() = _runTest {
        updateEveryXth(preAddedItemsCount = 1000)
    }

    @Test
    fun update2kComposableItems() = _runTest {
        updateEveryXth(preAddedItemsCount = 2000)
    }
}

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class RunRemoveAllItems {
    /**
     * Notes:
     * These experiments show much higher numbers on macos.
     * But if same experiments run as an app - runReleaseExecutableMacos...,
     * then it shows much better results.
     */

    @OptIn(ExperimentalTime::class)
    @Test
    fun removeAll10ComposableItems() = _runTest {
        removeAllItems(preAddedItemsCount = 10)
    }

    @Test
    fun removeAll100ComposableItems() = _runTest {
        removeAllItems(preAddedItemsCount = 100)
    }

    @Test
    fun removeAll500ComposableItems() = _runTest {
        removeAllItems(preAddedItemsCount = 500)
    }

    @Test
    fun removeAll1kComposableItems() = _runTest {
        removeAllItems(preAddedItemsCount = 1000)
    }

    @Test
    fun removeAll2kComposableItems() = _runTest {
        removeAllItems(preAddedItemsCount = 2000)
    }
}

import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentRecomposeScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi
import org.jetbrains.compose.web.testutils.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ComposeWebExperimentalTestsApi::class)
class TestsForTestUtils {

    @Test
    fun waitForRecompositionComplete_suspends_and_continues_properly() = runTest {
        var recomposeScope: RecomposeScope? = null

        composition {
            recomposeScope = currentRecomposeScope
        }
        delay(100) // to let the initial composition complete

        var waitForRecompositionCompleteContinued = false

        val job = launch {
            waitForRecompositionComplete()
            waitForRecompositionCompleteContinued = true
        }

        delay(100) // to check that `waitForRecompositionComplete` is suspended after delay
        assertEquals(false, waitForRecompositionCompleteContinued)

        delay(100)
        // we made no changes during 100 ms, so `waitForRecompositionComplete` should remain suspended
        assertEquals(false, waitForRecompositionCompleteContinued)

        recomposeScope!!.invalidate() // force recomposition
        job.join()

        assertEquals(true, waitForRecompositionCompleteContinued)
    }

    @Test
    fun waitForChanges_suspends_and_continues_properly() = runTest {
        var waitForChangesContinued = false

        var recomposeScope: RecomposeScope? = null
        var showText = ""

        composition {
            recomposeScope = currentRecomposeScope

            SideEffect {
                root.innerText = showText
            }
        }

        assertEquals("<div></div>", root.outerHTML)

        val job = launch {
            waitForChanges(root)
            waitForChangesContinued = true
        }

        delay(100) // to check that `waitForChanges` is suspended after delay
        assertEquals(false, waitForChangesContinued)

        // force recomposition and check that `waitForChanges` remains suspended as no changes occurred
        recomposeScope!!.invalidate()
        waitForRecompositionComplete()
        assertEquals(false, waitForChangesContinued)

        // Make changes and check that `waitForChanges` continues
        showText = "Hello World!"
        recomposeScope!!.invalidate()
        waitForRecompositionComplete()

        job.join()
        assertEquals(true, waitForChangesContinued)
        assertEquals("<div>Hello World!</div>", root.outerHTML)
    }

    @Test
    fun waitForChanges_cancels_with_timeout() = runTest {

        var cancelled = false

        val job = launch {
            try {
                withTimeout(1000) {
                    waitForChanges(root)
                }
            } catch (t: TimeoutCancellationException) {
                cancelled = true
                throw t
            }
        }

        delay(100) // to check that `waitForChanges` is suspended after delay
        assertEquals(false, cancelled)

        delay(1000) // to check that `waitForChanges` is cancelled after timeout
        assertEquals(true, cancelled)

        job.join()
    }

    @Test
    fun waitForRecompositionComplete_cancels_with_timeout() = runTest {

        var cancelled = false

        val job = launch {
            try {
                withTimeout(1000) {
                    waitForRecompositionComplete()
                }
            } catch (t: TimeoutCancellationException) {
                cancelled = true
                throw t
            }
        }

        delay(100) // to check that `waitForRecompositionComplete` is suspended after delay
        assertEquals(false, cancelled)

        delay(1000) // to check that `waitForRecompositionComplete` is cancelled after timeout
        assertEquals(true, cancelled)

        job.join()
    }
}

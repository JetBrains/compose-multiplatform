package org.jetbrains.compose.resources

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertTrue

//See: https://youtrack.jetbrains.com/issue/CMP-6615#focus=Comments-27-14272705.0-0
@OptIn(ExperimentalTestApi::class, DelicateCoroutinesApi::class)
class MutexHandoffDeadlockReproducer {

    @Test
    fun reproducer() {
        val stop = AtomicBoolean(false)
        val environment = getTestEnvironment()
        val res = TestStringResource("app_name")

        //1. Use AsyncCache.Mutext from background thread
        repeat(Runtime.getRuntime().availableProcessors().coerceAtLeast(2)) {
            GlobalScope.launch(Dispatchers.Default) {
                while (!stop.get()) {
                    getString(environment, res)
                }
            }
        }

        //The thread is a daemon one: it stays parked forever, so it must not keep the JVM alive.
        val uiThread = AtomicReference<Thread>()
        var recompositionsCounter by mutableStateOf(0)
        thread(isDaemon = true) {
            runComposeUiTest {
                setContent {
                    uiThread.set(Thread.currentThread())
                    CompositionLocalProvider(LocalComposeEnvironment provides TestComposeEnvironment) {
                        //2. the composition and a getString on the main thread.
                        LaunchedEffect(Unit) {
                            while (!stop.get()) getString(environment, res)
                        }
                        key(recompositionsCounter) {
                            //3. the composition and a blocking stringResource on the main thread.
                            Text(stringResource(res))
                            recompositionsCounter++ //keep recomposing and re-reading the resource
                        }
                    }
                }
            }
        }

        val deadlockedStackTrace = awaitTheParkedUiThread(uiThread)
        stop.set(true)

        assertTrue(deadlockedStackTrace != null)
        println(
            "The UI thread has been parked by a resource reading for more than ${DEADLOCK_TIMEOUT_MS}ms:\n" +
                    deadlockedStackTrace.take(10).joinToString("\n") { "\tat $it" }
        )
    }

    /**
     * @return the stack trace of the UI thread if it stays in `ResourceAwaiter.await`
     * for [DEADLOCK_TIMEOUT_MS] without a single break, or null otherwise.
     */
    private fun awaitTheParkedUiThread(uiThread: AtomicReference<Thread>): Array<StackTraceElement>? {
        val finishAt = System.currentTimeMillis() + TEST_BUDGET_MS
        var parkedSince = System.currentTimeMillis()
        while (System.currentTimeMillis() < finishAt) {
            Thread.sleep(1)
            val stackTrace = uiThread.get()?.stackTrace ?: continue
            val isParkedByAReading = stackTrace.any {
                it.className.endsWith("ResourceAwaiter") && it.methodName == "await"
            }
            if (!isParkedByAReading) {
                parkedSince = System.currentTimeMillis()
            } else if (System.currentTimeMillis() - parkedSince > DEADLOCK_TIMEOUT_MS) {
                return stackTrace
            }
        }
        return null
    }

    private companion object {
        const val DEADLOCK_TIMEOUT_MS = 2_000L
        const val TEST_BUDGET_MS = 5_000L
    }
}

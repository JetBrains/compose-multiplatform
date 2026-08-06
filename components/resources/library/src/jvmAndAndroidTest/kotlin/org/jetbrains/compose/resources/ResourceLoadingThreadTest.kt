package org.jetbrains.compose.resources

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds

class ResourceLoadingThreadTest {

    @Test
    fun testResourceIsLoadedOnTheCallingThread() {
        val callerThread = Thread.currentThread()
        val cache = AsyncCache<String, Thread>()

        val loadingThread = runResourceBlocking {
            cache.getOrLoad("key") { Thread.currentThread() }
        }

        assertSame(callerThread, loadingThread, "A resource must be loaded on the calling thread")
    }

    @Test
    fun testTheSharedRequestIsNotCancelledByTheFirstCaller() = runTest {
        val cache = AsyncCache<String, String>()
        val allowLoadToFinish = CompletableDeferred<Unit>()
        lateinit var result: String

        val firstCaller = GlobalScope.launch(Dispatchers.Default) {
            cache.getOrLoad("key") {
                allowLoadToFinish.await()
                "a value"
            }
        }
        val secondCaller = GlobalScope.launch(Dispatchers.Default) {
            result = cache.getOrLoad("key") {
               error("must reuse the shared request")
           }
        }

        delay(100.milliseconds)
        firstCaller.cancel()
        allowLoadToFinish.complete(Unit)
        secondCaller.join()

        assertEquals("a value", result)
    }

    @Test
    fun testWithContextInsideAReaderDoesNotDeadlock() = runTest {
        val cache = AsyncCache<String, String>()

        val value = withTimeoutOrNull(100.milliseconds)  {
            runResourceBlocking {
                cache.getOrLoad("key") {
                    //a custom resource reader is free to move its IO to another dispatcher
                    withContext(Dispatchers.IO) { delay(50); "a value" }
                }
            }
        }

        assertEquals("a value", value)
    }
}

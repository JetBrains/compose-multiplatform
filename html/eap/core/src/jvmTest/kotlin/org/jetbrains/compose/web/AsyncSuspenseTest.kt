/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.jetbrains.compose.web.dom.*
import kotlin.test.*

class AsyncSuspenseTest {
    private val options get() = AsyncHtmlRenderOptions(hydratable = false, identifierPrefix = "test")

    @Test fun readyStringMatchesSynchronousRendering() = runBlocking<Unit> {
        withTimeout(5000) {
            assertEquals("<div>ready</div>", composeHtmlToStringAsync(options) {
                Suspense(fallback = { error("unexpected fallback") }) { Div { Text("ready") } }
            })
        }
    }

    @Test fun produceStateRunsAndContentIsRetained() = runBlocking<Unit> {
        withTimeout(5000) {
            var starts = 0
            var disposed = 0
            var remembers = 0
            val html = composeHtmlToStringAsync(options) {
                Suspense(fallback = { Text("loading") }) {
                    val token = remember { remembers++; "retained" }
                    DisposableEffect(Unit) { onDispose { disposed++ } }
                    val loaded by produceState(false) { starts++; delay(20); value = true }
                    if (!loaded) SuspensePending() else Text(token)
                }
            }
            assertEquals("retained", html)
            assertEquals(1, remembers)
            assertEquals(1, starts)
            assertEquals(1, disposed)
        }
    }

    @Test fun removedPendingMarkersDoNotLeaveBoundaryPending() = runBlocking<Unit> {
        withTimeout(5000) {
            assertEquals("done", composeHtmlToStringAsync(options) {
                Suspense(fallback = { Text("loading") }) {
                    var pending by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) { delay(10); pending = false }
                    if (pending) { SuspensePending(); SuspensePending() } else Text("done")
                }
            })
        }
    }

    @Test fun independentSiblingLoadersStartBeforeEitherFinishes() = runBlocking<Unit> {
        withTimeout(5000) {
            val started = Channel<Unit>(2)
            val release = CompletableDeferred<Unit>()
            val result = async {
                composeHtmlToStringAsync(options) {
                    Suspense(fallback = { Text("loading") }) {
                        repeat(2) { index ->
                            val ready by produceState(false) { started.send(Unit); release.await(); value = true }
                            if (!ready) SuspensePending() else Text(index.toString())
                        }
                    }
                }
            }
            repeat(2) { started.receive() }
            release.complete(Unit)
            assertEquals("01", result.await())
        }
    }

    @Test fun completionCancelsUnrelatedInfiniteEffects() = runBlocking<Unit> {
        withTimeout(5000) {
            val cancelled = CompletableDeferred<Unit>()
            val html = composeHtmlToStringAsync(options) {
                var pending by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    try { pending = false; awaitCancellation() } finally { cancelled.complete(Unit) }
                }
                if (pending) SuspensePending() else Text("done")
            }
            assertEquals("done", html)
            cancelled.await()
        }
    }

    @Test fun streamReturnsShellAndRevealsSiblingsInCompletionOrder() = runBlocking<Unit> {
        withTimeout(5000) {
            val first = CompletableDeferred<Unit>()
            val second = CompletableDeferred<Unit>()
            val chunks = Channel<String>(Channel.UNLIMITED)
            val stream = composeHtmlToStreamAsync(options) {
                Text("header")
                Suspense(fallback = { Text("first loading") }) { Gated(first, "first ready") }
                Suspense(fallback = { Text("second loading") }) { Gated(second, "second ready") }
            }
            val collecting = launch { stream.chunks.collect(chunks::send) }
            val shell = chunks.receive()
            assertContains(shell, "header")
            assertContains(shell, "first loading")
            assertContains(shell, "second loading")
            second.complete(Unit)
            val secondPatch = chunks.receive()
            assertContains(secondPatch, "second ready")
            assertFalse(secondPatch.contains("first ready"))
            first.complete(Unit)
            assertContains(chunks.receive(), "first ready")
            assertContains(chunks.receive(), ".complete(")
            collecting.join()
        }
    }

    @Test fun unboundedPendingDelaysShell() = runBlocking<Unit> {
        withTimeout(5000) {
            val release = CompletableDeferred<Unit>()
            val started = CompletableDeferred<Unit>()
            val request = this@runBlocking
            val rendering = async {
                request.composeHtmlToStreamAsync(options) {
                    val ready by produceState(false) { started.complete(Unit); release.await(); value = true }
                    if (!ready) SuspensePending() else Text("shell")
                }
            }
            started.await()
            assertFalse(rendering.isCompleted)
            release.complete(Unit)
            val stream = rendering.await()
            assertContains(stream.chunks.toList().first(), "shell")
        }
    }

    @Test fun nestedBoundaryAppearsWhenItsPendingParentReveals() = runBlocking<Unit> {
        withTimeout(5000) {
            val outer = CompletableDeferred<Unit>()
            val inner = CompletableDeferred<Unit>()
            val chunks = Channel<String>(Channel.UNLIMITED)
            val stream = composeHtmlToStreamAsync(options) {
                Suspense(fallback = { Text("outer loading") }) {
                    Gated(outer, "outer ready")
                    Suspense(fallback = { Text("inner loading") }) { Gated(inner, "inner ready") }
                }
            }
            val collecting = launch { stream.chunks.collect(chunks::send) }
            assertContains(chunks.receive(), "outer loading")
            outer.complete(Unit)
            val outerPatch = chunks.receive()
            assertContains(outerPatch, "outer ready")
            assertContains(outerPatch, "inner loading")
            inner.complete(Unit)
            assertContains(chunks.receive(), "inner ready")
            chunks.receive()
            collecting.join()
        }
    }

    @Test fun fallbackPendingDelaysShellWithoutAnOuterBoundary() = runBlocking<Unit> {
        withTimeout(5000) {
            val release = CompletableDeferred<Unit>()
            val started = CompletableDeferred<Unit>()
            val request = this@runBlocking
            val rendering = async {
                request.composeHtmlToStreamAsync(options) {
                    Suspense(fallback = { SuspensePending() }) {
                        val ready by produceState(false) { started.complete(Unit); release.await(); value = true }
                        if (!ready) SuspensePending() else Text("done")
                    }
                }
            }
            started.await()
            assertFalse(rendering.isCompleted)
            release.complete(Unit)
            assertContains(rendering.await().chunks.toList().first(), "done")
        }
    }

    @Test fun cancellingUnconsumedStreamDisposesContentAndProducer() = runBlocking<Unit> {
        withTimeout(5000) {
            val started = CompletableDeferred<Unit>()
            val stopped = CompletableDeferred<Unit>()
            val disposed = CompletableDeferred<Unit>()
            val stream = composeHtmlToStreamAsync(options) {
                Suspense(fallback = { Text("loading") }) {
                    DisposableEffect(Unit) { onDispose { disposed.complete(Unit) } }
                    LaunchedEffect(Unit) {
                        started.complete(Unit)
                        try { awaitCancellation() } finally { stopped.complete(Unit) }
                    }
                    SuspensePending()
                }
            }
            started.await()
            stream.cancel()
            stopped.await()
            disposed.await()
        }
    }

    @Test fun failedConsumerCancelsProducer() = runBlocking<Unit> {
        withTimeout(5000) {
            val stopped = CompletableDeferred<Unit>()
            val stream = composeHtmlToStreamAsync(options) {
                Suspense(fallback = { Text("loading") }) {
                    LaunchedEffect(Unit) { try { awaitCancellation() } finally { stopped.complete(Unit) } }
                    SuspensePending()
                }
            }
            assertFailsWith<IllegalArgumentException> {
                stream.chunks.collect { throw IllegalArgumentException("sink failed") }
            }
            stopped.await()
        }
    }

    @Test fun producerFailureBeforeShellIsReportedAndThrown() = runBlocking<Unit> {
        withTimeout(5000) {
            val errors = mutableListOf<Throwable>()
            val failure = IllegalStateException("producer failed")
            val result = runCatching {
                composeHtmlToStringAsync(AsyncHtmlRenderOptions(onError = errors::add)) {
                    LaunchedEffect(Unit) { throw failure }
                    SuspensePending()
                }
            }
            assertRenderFailure(failure, result.exceptionOrNull())
            assertEquals(1, errors.size)
            assertRenderFailure(failure, errors.single())
        }
    }

    @Test fun producerFailureAfterShellFailsStream() = runBlocking<Unit> {
        withTimeout(5000) {
            val release = CompletableDeferred<Unit>()
            val errors = mutableListOf<Throwable>()
            val failure = IllegalStateException("late failure")
            val stream = composeHtmlToStreamAsync(AsyncHtmlRenderOptions(onError = errors::add)) {
                Suspense(fallback = { Text("loading") }) {
                    LaunchedEffect(Unit) { release.await(); throw failure }
                    SuspensePending()
                }
            }
            val chunks = mutableListOf<String>()
            val result = runCatching {
                stream.chunks.collect {
                    chunks += it
                    release.complete(Unit)
                }
            }
            assertRenderFailure(failure, result.exceptionOrNull())
            assertContains(chunks.last(), ".fail()")
            assertFalse(chunks.last().contains("late failure"))
            assertEquals(1, errors.size)
            assertRenderFailure(failure, errors.single())
        }
    }

    @Test fun requestCancellationCancelsRender() = runBlocking<Unit> {
        withTimeout(5000) {
            val stopped = CompletableDeferred<Unit>()
            val request = CoroutineScope(coroutineContext + Job())
            val stream = request.composeHtmlToStreamAsync(options) {
                Suspense(fallback = { Text("loading") }) {
                    LaunchedEffect(Unit) { try { awaitCancellation() } finally { stopped.complete(Unit) } }
                    SuspensePending()
                }
            }
            request.cancel()
            stopped.await()
            assertFailsWith<CancellationException> { stream.chunks.toList() }
        }
    }

    @Test fun streamHasOneConsumerAndEscapesScriptDataAndNonce() = runBlocking<Unit> {
        withTimeout(5000) {
            val release = CompletableDeferred<Unit>()
            val options = AsyncHtmlRenderOptions(nonce = "a\"<&", identifierPrefix = "safe")
            val stream = composeHtmlToStreamAsync(options) {
                Suspense(fallback = { Text("loading") }) { Gated(release, "</script><script>bad</script>") }
            }
            val chunks = mutableListOf<String>()
            stream.chunks.collect { chunks += it; release.complete(Unit) }
            assertTrue(chunks.all { it.contains("nonce=\"a&quot;&lt;&amp;\"") })
            assertContains(chunks[1], "\\u0026")
            assertFalse(chunks[1].contains("</script><script>bad"))
            assertFailsWith<IllegalStateException> { stream.chunks.collect {} }
        }
    }

    @Test fun failureIsReportedEvenWhenConsumerHasNotStarted() = runBlocking<Unit> {
        withTimeout(5000) {
            val release = CompletableDeferred<Unit>()
            val reported = CompletableDeferred<Throwable>()
            val problem = IllegalStateException("stalled sink")
            val stream = composeHtmlToStreamAsync(AsyncHtmlRenderOptions(onError = { reported.complete(it) })) {
                Suspense(fallback = { Text("loading") }) {
                    LaunchedEffect(Unit) { release.await(); throw problem }
                    SuspensePending()
                }
            }
            release.complete(Unit)
            assertRenderFailure(problem, reported.await())
            val collected = runCatching { stream.chunks.toList() }
            assertRenderFailure(problem, collected.exceptionOrNull())
        }
    }

    @Test fun timedOutStringCancelsLoaderWithoutReportingError() = runBlocking<Unit> {
        val started = CompletableDeferred<Unit>()
        val stopped = CompletableDeferred<Unit>()
        val errors = mutableListOf<Throwable>()
        val rendering = async {
            withTimeout(100) {
                composeHtmlToStringAsync(AsyncHtmlRenderOptions(onError = errors::add)) {
                    LaunchedEffect(Unit) {
                        started.complete(Unit)
                        try { awaitCancellation() } finally { stopped.complete(Unit) }
                    }
                    SuspensePending()
                }
            }
        }
        started.await()
        assertFailsWith<TimeoutCancellationException> { rendering.await() }
        withTimeout(5000) { stopped.await() }
        assertTrue(errors.isEmpty())
    }

    @Test fun concurrentRequestsHaveIndependentCompositionAndState() = runBlocking<Unit> {
        withTimeout(5000) {
            val results = (1..12).map { index -> async(Dispatchers.Default) {
                composeHtmlToStringAsync(options) {
                    Suspense(fallback = {}) {
                        val ready by produceState(false) { delay(index.toLong()); value = true }
                        if (!ready) SuspensePending() else Text(index.toString())
                    }
                }
            } }.awaitAll()
            assertEquals((1..12).map(Int::toString), results)
        }
    }

    @Test fun invalidTableReplacementFailsInsteadOfRelyingOnParserRecovery() = runBlocking<Unit> {
        withTimeout(5000) {
            val release = CompletableDeferred<Unit>()
            val errors = mutableListOf<Throwable>()
            val stream = composeHtmlToStreamAsync(AsyncHtmlRenderOptions(onError = errors::add)) {
                Table { Tbody { Suspense(fallback = { Tr { Td { Text("loading") } } }) {
                    val ready by produceState(false) { release.await(); value = true }
                    if (!ready) SuspensePending() else Div { Text("invalid row") }
                } } }
            }
            val result = runCatching { stream.chunks.collect { release.complete(Unit) } }
            assertIs<IllegalArgumentException>(result.exceptionOrNull())
            assertContains(result.exceptionOrNull()!!.message!!, "cannot be serialized directly inside <tbody>")
            assertEquals(1, errors.size)
        }
    }

    @Test fun finalHydrationStateIsTransferredAfterPatches() = runBlocking<Unit> {
        withTimeout(5000) {
            val release = CompletableDeferred<Unit>()
            val stream = composeHtmlToStreamAsync(AsyncHtmlRenderOptions(identifierPrefix = "hydration", validateStrictly = true)) {
                var ready by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { release.await(); ready = true }
                HydrationRoot(ready, Boolean::toString) {
                    Suspense(fallback = { Text("loading") }) {
                        if (!it) SuspensePending() else Text("done")
                    }
                }
            }
            val chunks = mutableListOf<String>()
            stream.chunks.collect { chunks += it; release.complete(Unit) }
            assertContains(chunks.first(), "data-compose-stream-pending=\"hydration\"")
            assertContains(chunks.last(), "[null,\"true\"]")
        }
    }

    @Test fun documentClosingTagsFollowCompletionAndFailureScripts() = runBlocking<Unit> {
        withTimeout(5000) {
            for (fail in listOf(false, true)) {
                val release = CompletableDeferred<Unit>()
                val stream = composeHtmlToStreamAsync(options) {
                    Html {
                        Head { Title { Text("test") } }
                        Body {
                            Suspense(fallback = { Text("loading") }) {
                                val ready by produceState(false) {
                                    release.await()
                                    if (fail) error("private failure")
                                    value = true
                                }
                                if (!ready) SuspensePending() else Text("ready")
                            }
                        }
                    }
                }
                val chunks = mutableListOf<String>()
                val result = runCatching {
                    stream.chunks.collect {
                        chunks += it
                        release.complete(Unit)
                    }
                }
                assertFalse(chunks.first().contains("</body>"))
                assertFalse(chunks.first().contains("</html>"))
                assertTrue(chunks.last().endsWith("</script></body></html>"))
                assertContains(chunks.last(), if (fail) ".fail()" else ".complete(")
                assertEquals(fail, result.isFailure)
            }
        }
    }

    @Test fun strictStreamingRejectsChangedShellAndRevealedContent() = runBlocking<Unit> {
        withTimeout(5000) {
            for (changeShell in listOf(true, false)) {
                val first = CompletableDeferred<Unit>()
                val last = CompletableDeferred<Unit>()
                var label by mutableStateOf("original")
                val stream = composeHtmlToStreamAsync(AsyncHtmlRenderOptions(validateStrictly = true)) {
                    if (changeShell) Text(label)
                    Suspense(fallback = { Text("first loading") }) {
                        val ready by produceState(false) {
                            first.await()
                            value = true
                        }
                        if (!ready) SuspensePending() else Text(if (changeShell) "first ready" else label)
                    }
                    Suspense(fallback = { Text("last loading") }) { Gated(last, "last ready") }
                }
                var shell = true
                val chunks = mutableListOf<String>()
                val result = runCatching {
                    stream.chunks.collect { chunk ->
                        chunks += chunk
                        if (shell) {
                            shell = false
                            first.complete(Unit)
                        } else if (chunk.contains(".replace(")) {
                            label = "changed"
                            last.complete(Unit)
                        }
                    }
                }
                assertIs<IllegalStateException>(result.exceptionOrNull())
                assertContains(result.exceptionOrNull()!!.message!!, "changed after it was streamed")
                assertContains(chunks.last(), ".fail()")
                assertFalse(chunks.last().contains(".complete("))
            }
        }
    }

    @Test fun pendingFallbackBoundariesCanDisappearWhenTheirParentReveals() = runBlocking<Unit> {
        withTimeout(5000) {
            val release = CompletableDeferred<Unit>()
            val stream = composeHtmlToStreamAsync(AsyncHtmlRenderOptions(validateStrictly = true)) {
                Suspense(fallback = {
                    Suspense(fallback = { Text("nested fallback") }) { SuspensePending() }
                }) { Gated(release, "parent ready") }
            }
            val chunks = mutableListOf<String>()
            stream.chunks.collect {
                chunks += it
                release.complete(Unit)
            }
            assertContains(chunks.first(), "nested fallback")
            assertEquals(1, chunks.drop(1).count { it.contains(".replace(") })
            assertContains(chunks.last(), ".complete(")
        }
    }

    @Test fun externalSnapshotWritesWakeTheOwningRender() = runBlocking<Unit> {
        withTimeout(5000) {
            var pending by mutableStateOf(true)
            val stream = composeHtmlToStreamAsync(options) {
                Suspense(pending = pending, fallback = { Text("loading") }) { Text("ready") }
            }
            val chunks = mutableListOf<String>()
            stream.chunks.collect { chunk ->
                chunks += chunk
                withContext(Dispatchers.Default) { pending = false }
            }
            assertContains(chunks[1], "ready")
        }
    }

    @Test fun serverFramesAreMonotonicWithoutDisplayRefreshDelays() = runBlocking<Unit> {
        withTimeout(1000) {
            val frames = mutableListOf<Long>()
            val html = composeHtmlToStringAsync(options) {
                var pending by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    repeat(100) { frames += withFrameNanos { it } }
                    pending = false
                }
                if (pending) SuspensePending() else Text("done")
            }
            assertEquals("done", html)
            assertEquals(100, frames.size)
            assertTrue(frames.zipWithNext().all { (a, b) -> b > a })
        }
    }

    @Test fun multithreadedRequestSerializesEffectsAndCancellationCleanup() = runBlocking<Unit> {
        withTimeout(5000) {
            val requestJob = Job()
            val request = CoroutineScope(Dispatchers.Default + requestJob)
            val active = java.util.concurrent.atomic.AtomicInteger()
            val maximum = java.util.concurrent.atomic.AtomicInteger()
            val stopped = java.util.concurrent.atomic.AtomicInteger()
            val started = Channel<Unit>(Channel.UNLIMITED)
            fun work() {
                val running = active.incrementAndGet()
                maximum.updateAndGet { maxOf(it, running) }
                try { Thread.sleep(1) } finally { active.decrementAndGet() }
            }
            try {
                request.composeHtmlToStreamAsync(options) {
                    Suspense(fallback = { Text("loading") }) {
                        repeat(24) { index ->
                            LaunchedEffect(index) {
                                work()
                                started.send(Unit)
                                try { awaitCancellation() } finally {
                                    work()
                                    stopped.incrementAndGet()
                                }
                            }
                        }
                        SuspensePending()
                    }
                }
                repeat(24) { started.receive() }
            } finally {
                request.cancel()
                requestJob.join()
            }
            assertEquals(24, stopped.get())
            assertEquals(1, maximum.get())
        }
    }
    private fun assertRenderFailure(expected: Throwable, actual: Throwable?) {
        // Coroutine stack-trace recovery can copy exceptions across suspension points.
        assertIs<IllegalStateException>(actual)
        assertEquals(expected.message, actual.message)
    }

}

@Composable
private fun Gated(gate: CompletableDeferred<Unit>, label: String) {
    val ready by produceState(false) { gate.await(); value = true }
    if (!ready) SuspensePending() else Text(label)
}

/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.jetbrains.compose.web.dom.Body
import org.jetbrains.compose.web.dom.Html
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.junit.Assume.assumeNotNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertSame
import kotlin.test.assertNotSame
import kotlin.test.assertNotNull

class ComposeHtmlConcurrencyTest {
    @Test
    fun concurrentCallsWithTheSameKeyHaveIndependentStorage() {
        assertIndependentRenders { "concurrent-shared-key" }
    }

    @Test
    fun concurrentCallsWithDifferentKeysHaveIndependentStorage() {
        // Equal hashes also exercise concurrent insertion into the renderer cache.
        val keys = listOf("AaAa", "AaBB", "BBAa", "BBBB", "AaAaAa", "BBAaAa")
        assertIndependentRenders { "concurrent-${keys[it]}" }
    }

    @Test
    fun concurrentUnkeyedCallsHaveIndependentStorage() {
        assertIndependentRenders { null }
    }

    private fun assertIndependentRenders(keyForWorker: (Int) -> String?) {
        val state = mutableStateOf("outside")
        val overlap = CyclicBarrier(6)
        runConcurrently(6) { worker ->
            repeat(10) { iteration ->
                val request = "request-$worker-$iteration"
                val effects = mutableListOf<String>()
                val thread = Thread.currentThread()
                val hydratable = iteration % 2 == 0
                val content: @Composable () -> Unit = {
                    CompositionLocalProvider(LocalRequest provides request) {
                        val remembered = remember { request }
                        state.value = request
                        DisposableEffect(Unit) {
                            assertEquals(thread, Thread.currentThread())
                            effects.add("enter $request")
                            onDispose {
                                assertEquals(thread, Thread.currentThread())
                                effects.add("dispose $request")
                            }
                        }
                        SideEffect {
                            assertEquals(thread, Thread.currentThread())
                            effects.add("side $request")
                        }
                        Div(attrs = { id(request) }) {
                            Text(remembered)
                            overlap.await(10, TimeUnit.SECONDS)
                            assertEquals(request, LocalRequest.current)
                            assertEquals(request, state.value)
                            Text("<&>😀")
                            if (iteration % 2 == 0) Span { Text(request) }
                        }
                    }
                }
                val boundary = if (hydratable) "<!--c-->" else ""
                val child = if (iteration % 2 == 0) "<span>$request</span>" else ""
                assertEquals(
                    "<div id=\"$request\">$request${boundary}&lt;&amp;&gt;😀$child</div>",
                    render((worker + iteration) % 3, keyForWorker(worker), hydratable, content),
                )
                assertEquals(listOf("enter $request", "side $request", "dispose $request"), effects)
                assertEquals("outside", state.value)
            }
        }
        assertEquals("outside", state.value)
    }

    @Test
    fun concurrentDocumentsShareAKeyAcrossStringStreamAndWriterWithIndependentState() {
        val key = "concurrent-documents"
        val state = mutableStateOf("outside")
        val overlap = CyclicBarrier(6)
        runConcurrently(6) { worker ->
            repeat(10) { iteration ->
                val request = "request-$worker-$iteration"
                val strict = iteration % 2 == 0
                var disposals = 0
                val content: @Composable () -> Unit = {
                    val remembered = remember { request }
                    state.value = request
                    DisposableEffect(Unit) { onDispose { disposals++ } }
                    Html {
                        Body {
                            HydrationRoot(remembered, { it }, hydrationId = "app") {
                                overlap.await(10, TimeUnit.SECONDS)
                                assertEquals(request, state.value)
                                assertEquals(request, composeHtmlToString(key = key) { Text(request) })
                                Text(remembered)
                            }
                        }
                    }
                }
                val actual = when ((worker + iteration) % 3) {
                    0 -> renderHydratedDocument(strict, key, content)
                    1 -> buildString {
                        renderHydratedDocumentToStream({ append(it) }, 1, strict, key, content)
                    }
                    else -> StringWriter().also {
                        renderHydratedDocumentToStream(it, 1, strict, key, content)
                    }.toString()
                }
                val validation = if (strict) " data-compose-hydration-validation=\"on\"" else ""
                assertEquals(
                    "<!doctype html><html><body><div data-compose-hydration-root=\"app\">$request</div>" +
                        "<script data-compose-hydration-state=\"escaped-text-v1\" data-compose-hydration-for=\"app\"" +
                        "$validation type=\"text/plain\">$request</script></body></html>",
                    actual,
                )
                assertEquals(1, disposals)
                assertEquals("outside", state.value)
            }
        }
    }

    @Test
    fun blockedSinkDoesNotBlockOtherCallsEvenWithTheSameKey() {
        for (otherKey in listOf("blocked-sink", "independent-sink")) {
            val sinkEntered = CountDownLatch(1)
            val otherRenderFinished = CountDownLatch(1)
            runConcurrently(2) { worker ->
                if (worker == 0) {
                    val chunks = mutableListOf<String>()
                    composeHtmlToStream(
                        sink = {
                            chunks.add(it)
                            sinkEntered.countDown()
                            assertTrue(otherRenderFinished.await(10, TimeUnit.SECONDS))
                        },
                        chunkSize = 1,
                        key = "blocked-sink",
                    ) { Text("first") }
                    assertEquals(listOf("first"), chunks)
                } else {
                    assertTrue(sinkEntered.await(10, TimeUnit.SECONDS))
                    try {
                        assertEquals("second", composeHtmlToString(key = otherKey) { Text("second") })
                    } finally {
                        otherRenderFinished.countDown()
                    }
                }
            }
        }
    }

    @Test
    fun concurrentNestedRendersWithOppositeKeyOrderDoNotDeadlock() {
        val overlap = CyclicBarrier(2)
        runConcurrently(2) { worker ->
            val outerKey = "nested-$worker"
            val innerKey = "nested-${1 - worker}"
            val html = composeHtmlToString(key = outerKey) {
                Text("before")
                overlap.await(10, TimeUnit.SECONDS)
                val inner = composeHtmlToString(key = innerKey) { Span { Text(innerKey) } }
                Text(inner)
                Text("after")
            }
            assertEquals("before<!--c-->&lt;span&gt;$innerKey&lt;/span&gt;<!--c-->after", html)
        }
    }

    @Test
    fun failedCallsDoNotCorruptConcurrentCallsAndTheirThreadCanReuseTheKey() {
        for (failure in listOf("content", "sink", "disposal")) {
            val overlap = CyclicBarrier(2)
            runConcurrently(2) { worker ->
                val key = "concurrent-failure-$failure"
                if (worker == 0) {
                    val cause = IllegalStateException(failure)
                    val caught = assertFailsWith<IllegalStateException> {
                        composeHtmlToStream(
                            sink = { if (failure == "sink") throw cause },
                            chunkSize = 1,
                            key = key,
                        ) {
                            DisposableEffect(Unit) {
                                onDispose { if (failure == "disposal") throw cause }
                            }
                            overlap.await(10, TimeUnit.SECONDS)
                            if (failure == "content") throw cause
                            Text("failing")
                        }
                    }
                    assertEquals(cause, caught)
                    assertEquals("recovered", composeHtmlToString(key = key) { Text("recovered") })
                } else {
                    assertEquals("<div>healthy</div>", composeHtmlToString(key = key) {
                        Div {
                            overlap.await(10, TimeUnit.SECONDS)
                            Text("healthy")
                        }
                    })
                }
            }
        }
    }

    @Test
    fun renderingStorageCanBeReusedOnAnotherThread() {
        val key = "cross-thread-reuse"
        val first = AtomicReference<HtmlRenderer>()
        repeat(2) { request ->
            val executor = Executors.newSingleThreadExecutor()
            try {
                executor.submit {
                    assertEquals("request-$request", composeHtmlToString(key = key) { Text("request-$request") })
                    val pool = assertNotNull(getHtmlRendererPool(key))
                    val renderer = pool.borrow()
                    if (request == 0) first.set(renderer) else assertSame(first.get(), renderer)
                    pool.recycle(renderer)
                }.get(10, TimeUnit.SECONDS)
            } finally {
                executor.shutdownNow()
                assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
            }
        }
    }

    @Test
    fun virtualThreadsReuseStorageBetweenRequests() {
        val factory = runCatching { Executors::class.java.getMethod("newVirtualThreadPerTaskExecutor") }.getOrNull()
        assumeNotNull(factory) // Run on JDK 21+, skip on older test runtimes.
        val executor = factory!!.invoke(null) as ExecutorService
        val first = AtomicReference<HtmlRenderer>()
        var previousThread: Thread? = null
        try {
            repeat(3) { request ->
                executor.submit {
                    assertNotSame(previousThread, Thread.currentThread())
                    previousThread = Thread.currentThread()
                    val key = "virtual-thread-reuse"
                    assertEquals("request-$request", composeHtmlToString(key = key) { Text("request-$request") })
                    val pool = assertNotNull(getHtmlRendererPool(key))
                    val renderer = pool.borrow()
                    if (request == 0) first.set(renderer) else assertSame(first.get(), renderer)
                    pool.recycle(renderer)
                }.get(10, TimeUnit.SECONDS)
            }
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
        }
    }

    @Test
    fun registryKeyLimitIsRespectedDuringConcurrentRegistration() {
        for (limit in listOf(0, 1, 4, 32)) {
            val pools = HtmlRendererPools(maxKeys = limit, maxIdleRenderers = 1)
            val admitted = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
            runConcurrently(8) { worker ->
                repeat(100) { request ->
                    val key = "template-$worker-$request"
                    if (pools[key] != null) admitted.add(key)
                }
            }
            assertEquals(limit, admitted.size)
            admitted.forEach { assertNotNull(pools[it]) }
            assertEquals(null, pools["overflow"])
        }
    }

    @Test
    fun fullRegistryKeepsKnownKeysAvailableDuringConcurrentMisses() {
        val pools = HtmlRendererPools(maxKeys = 2, maxIdleRenderers = 1)
        // These keys collide, so known and unknown lookups also share map buckets.
        val first = assertNotNull(pools["AaAa"])
        val second = assertNotNull(pools["AaBB"])
        runConcurrently(8) {
            repeat(1000) {
                assertSame(first, pools["AaAa"])
                assertEquals(null, pools["BBAa"])
                assertSame(second, pools["AaBB"])
                assertEquals(null, pools["BBBB"])
            }
        }
    }

    @Test
    fun concurrentRegistrationOfOneKeyDoesNotConsumeOtherKeySlots() {
        val pools = HtmlRendererPools(maxKeys = 8, maxIdleRenderers = 1)
        runConcurrently(8) { assertNotNull(pools["shared"]) }
        repeat(7) { assertNotNull(pools["other-$it"]) }
        assertEquals(null, pools["overflow"])
    }

    @Test
    fun concurrentReturnsRespectTheIdleLimitAndBorrowersHaveExclusiveStorage() {
        val pool = HtmlRendererPool(maxIdleRenderers = 4)
        val borrowed = Array(8) { AtomicReference<HtmlRenderer>() }
        val overlap = CyclicBarrier(8)
        repeat(10) {
            runConcurrently(8) { worker ->
                val renderer = pool.borrow()
                borrowed[worker].set(renderer)
                overlap.await(10, TimeUnit.SECONDS)
                assertEquals(8, borrowed.map { it.get() }.toSet().size)
                overlap.await(10, TimeUnit.SECONDS)
                pool.recycle(renderer)
            }
            val retained = List(8) { pool.borrow() }
            assertEquals(4, retained.count { renderer -> borrowed.any { it.get() === renderer } })
            retained.forEach(pool::recycle)
        }
    }

    @Test
    fun overlappingBorrowAndReturnCyclesKeepExclusiveOwnership() {
        for (capacity in listOf(1, 4, 8)) {
            val pool = HtmlRendererPool(maxIdleRenderers = capacity)
            val active = ConcurrentHashMap.newKeySet<HtmlRenderer>()
            runConcurrently(12) {
                repeat(5000) { iteration ->
                    val renderer = pool.borrow()
                    assertTrue(active.add(renderer), "A renderer was borrowed by two workers")
                    if (iteration % 8 == 0) Thread.yield()
                    assertTrue(active.remove(renderer))
                    pool.recycle(renderer)
                }
            }
            assertTrue(active.isEmpty())
        }
    }

    private fun render(
        mode: Int,
        key: String?,
        hydratable: Boolean,
        content: @Composable () -> Unit,
    ): String = when (mode) {
        0 -> composeHtmlToString(hydratable, key, validateStrictly = true, content = content)
        1 -> buildString {
            composeHtmlToStream(sink = { append(it) }, chunkSize = 1, hydratable, key, content = content)
        }
        else -> StringWriter().also {
            composeHtmlToStream(it, chunkSize = 1, hydratable, key, content = content)
        }.toString()
    }

    private fun runConcurrently(workers: Int, action: (Int) -> Unit) {
        val executor = Executors.newFixedThreadPool(workers)
        val start = CyclicBarrier(workers)
        try {
            val futures = (0 until workers).map { worker ->
                executor.submit {
                    start.await(10, TimeUnit.SECONDS)
                    action(worker)
                }
            }
            futures.forEach { it.get(20, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
        }
    }
}

private val LocalRequest = staticCompositionLocalOf { "outside" }

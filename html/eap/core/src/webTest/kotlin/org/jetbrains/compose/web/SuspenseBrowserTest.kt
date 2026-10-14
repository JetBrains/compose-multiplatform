/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class,
    org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web

import androidx.compose.runtime.*
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLElement
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.internal.runtime.browserDocument
import kotlin.test.*

class SuspenseBrowserTest {
    @Test fun readyContentAppearsImmediatelyWithoutFallback() {
        val root = browserDocument.createElement("div") as HTMLElement
        var fallbacks = 0
        val composition = renderComposable(root) {
            Suspense(fallback = { fallbacks++; Text("loading") }) { Span { Text("ready") } }
        }
        try {
            assertEquals("ready", root.textContent)
            assertEquals(0, fallbacks)
            assertEquals(1, root.children.length)
            assertEquals("span", root.firstElementChild!!.localName)
        } finally { composition.dispose() }
    }

    @Test fun initialFallbackRetainsLoaderAndRememberedState() = MainScope().promise {
        val root = browserDocument.createElement("div") as HTMLElement
        val release = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        var remembers = 0
        var disposed = 0
        val composition = renderComposable(root) {
            Text("before")
            Suspense(fallback = { Text("loading") }) {
                val token = remember { remembers++; "ready" }
                DisposableEffect(Unit) { onDispose { disposed++ } }
                val loaded by produceState(false) { started.complete(Unit); release.await(); value = true }
                if (!loaded) SuspensePending() else Span { Text(token) }
            }
            Text("after")
        }
        try {
            assertEquals("beforeloadingafter", root.textContent)
            started.await()
            assertEquals(0, disposed)
            release.complete(Unit)
            delay(100)
            assertEquals("beforereadyafter", root.textContent)
            assertEquals(1, remembers)
        } finally { composition.dispose() }
        assertEquals(1, disposed)
    }

    @Test fun multipleMarkersClearIndependently() = MainScope().promise {
        val root = browserDocument.createElement("div") as HTMLElement
        var first by mutableStateOf(true)
        var second by mutableStateOf(true)
        val composition = renderComposable(root) {
            Suspense(fallback = { Text("loading") }) {
                if (first) SuspensePending() else Text("first")
                if (second) SuspensePending() else Text("second")
            }
        }
        try {
            assertEquals("loading", root.textContent)
            first = false
            delay(100)
            assertEquals("loading", root.textContent)
            second = false
            delay(100)
            assertEquals("firstsecond", root.textContent)
        } finally { composition.dispose() }
    }

    @Test fun hiddenContentDefersDomReferencesUntilTheRangeIsShown() = MainScope().promise {
        val root = browserDocument.createElement("div") as HTMLElement
        browserDocument.body!!.appendChild(root)
        var pending by mutableStateOf(true)
        var effects = 0
        var disposals = 0
        var loaderStarts = 0
        val composition = renderComposable(root) {
            Suspense(pending = pending, fallback = { Text("loading") }) {
                LaunchedEffect(Unit) {
                    loaderStarts++
                    awaitCancellation()
                }
                Span(attrs = { ref { element ->
                    assertTrue(root.contains(element))
                    effects++
                    onDispose { disposals++ }
                } }) {
                    DisposableRefEffect(Unit) { element ->
                        assertTrue(root.contains(element))
                        effects++
                        onDispose { disposals++ }
                    }
                    DomSideEffect {
                        assertTrue(root.contains(it))
                        effects++
                    }
                    Text("ready")
                }
            }
        }
        try {
            assertEquals("loading", root.textContent)
            assertEquals(0, effects)
            delay(100)
            assertEquals(1, loaderStarts)
            pending = false
            delay(100)
            assertEquals("ready", root.textContent)
            assertEquals(3, effects)
        } finally {
            composition.dispose()
            root.remove()
        }
        assertEquals(2, disposals)
    }

    @Test fun nestedReadyContentDefersReferencesWhileItsParentIsHidden() = MainScope().promise {
        val root = browserDocument.createElement("div") as HTMLElement
        browserDocument.body!!.appendChild(root)
        var pending by mutableStateOf(true)
        var effects = 0
        val composition = renderComposable(root) {
            Suspense(pending = pending, fallback = { Text("loading") }) {
                Suspense(fallback = {}) {
                    Span(attrs = { ref { element ->
                        assertTrue(root.contains(element))
                        effects++
                        onDispose { }
                    } }) { Text("ready") }
                }
            }
        }
        try {
            delay(100)
            assertEquals(0, effects)
            pending = false
            delay(100)
            assertEquals("ready", root.textContent)
            assertEquals(1, effects)
        } finally {
            composition.dispose()
            root.remove()
        }
    }

    @Test fun switchingBackToPendingPreservesDomAndState() = MainScope().promise {
        val root = browserDocument.createElement("div") as HTMLElement
        var pending by mutableStateOf(false)
        var remembers = 0
        val composition = renderComposable(root) {
            Suspense(pending = pending, fallback = { Span { Text("loading") } }) {
                remember { remembers++ }
                Span { Text("ready") }
            }
        }
        val ready = root.firstElementChild
        try {
            pending = true
            delay(100)
            assertEquals("loading", root.textContent)
            pending = false
            delay(100)
            assertEquals("ready", root.textContent)
            assertSame(ready, root.firstElementChild)
            assertEquals(1, remembers)
        } finally { composition.dispose() }
    }

    @Test fun keyedRangesMoveAndRemoveWithOrdinarySiblings() = MainScope().promise {
        val root = browserDocument.createElement("div") as HTMLElement
        var items by mutableStateOf(listOf("a", "b", "c"))
        val composition = renderComposable(root) {
            Text("before")
            items.forEach { label -> key(label) {
                Suspense(fallback = {}) { Span { Text(label) }; Text(label.uppercase()) }
            } }
            Text("after")
        }
        val first = root.firstElementChild
        try {
            assertEquals("beforeaAbBcCafter", root.textContent)
            items = listOf("c", "a", "b")
            delay(100)
            assertEquals("beforecCaAbBafter", root.textContent)
            items = listOf("a")
            delay(100)
            assertEquals("beforeaAafter", root.textContent)
            assertSame(first, root.firstElementChild)
        } finally { composition.dispose() }
    }

    @Test fun nestedFallbackPendingBelongsToOuterBoundary() = MainScope().promise {
        val root = browserDocument.createElement("div") as HTMLElement
        var fallbackPending by mutableStateOf(true)
        val composition = renderComposable(root) {
            Suspense(fallback = { Text("outer") }) {
                Text("prefix")
                Suspense(fallback = { if (fallbackPending) SuspensePending(); Text("inner") }) {
                    SuspensePending()
                }
            }
        }
        try {
            assertEquals("outer", root.textContent)
            fallbackPending = false
            delay(100)
            assertEquals("prefixinner", root.textContent)
        } finally { composition.dispose() }
    }

    @Test fun completedSsrBoundaryHydratesAndRetainsContentAcrossFallback() = MainScope().promise {
        val root = browserDocument.createElement("div") as HTMLElement
        var pending by mutableStateOf(false)
        val content: @Composable () -> Unit = {
            Text("before")
            Suspense(pending = pending, fallback = { Text("loading") }) { Span { Text("ready") } }
            Text("after")
        }
        root.innerHTML = composeHtmlToString(content = content)
        val ready = root.firstElementChild
        val composition = hydrateComposable(root, validateStrictly = true, onHydrationMismatch = { throw it }) { content() }
        try {
            assertSame(ready, root.firstElementChild)
            pending = true
            delay(100)
            assertEquals("beforeloadingafter", root.textContent)
            pending = false
            delay(100)
            assertEquals("beforereadyafter", root.textContent)
            assertSame(ready, root.firstElementChild)
        } finally { composition.dispose() }
    }

    @Test fun initiallyPendingClientLoaderUsesHydrationMismatchRecovery() = MainScope().promise {
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Suspense(fallback = {}) { Span { Text("ready") } }
        }
        val release = CompletableDeferred<Unit>()
        var mismatches = 0
        val composition = hydrateComposable(root, validateStrictly = true, onHydrationMismatch = { mismatches++ }) {
            Suspense(fallback = { Text("loading") }) {
                val ready by produceState(false) {
                    release.await()
                    value = true
                }
                if (!ready) SuspensePending() else Span { Text("ready") }
            }
        }
        try {
            assertEquals(1, mismatches)
            assertEquals("loading", root.textContent)
            release.complete(Unit)
            delay(100)
            assertEquals("ready", root.textContent)
        } finally { composition.dispose() }
    }

    @Test fun failedHydrationDefersDomEffectsAcrossBoundary() {
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Suspense(fallback = {}) { Button { Text("button") } }
            Span { Text("extra") }
        }
        val serverHtml = root.innerHTML
        var effects = 0
        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, validateStrictly = true, onHydrationMismatch = { throw it }) {
                Suspense(fallback = {}) {
                    Button(attrs = { ref { effects++; onDispose { } } }) {
                        DisposableRefEffect(Unit) { effects++; onDispose { } }
                        DomSideEffect { effects++ }
                        Text("button")
                    }
                }
            }
        }
        assertEquals(0, effects)
        assertEquals(serverHtml, root.innerHTML)
    }

    @Test fun standalonePatcherParsesTableRowsAndPreservesHydrationBoundaries() = MainScope().promise {
        withTimeout(5000) {
            val root = browserDocument.createElement("div") as HTMLElement
            browserDocument.body!!.appendChild(root)
            val release = CompletableDeferred<Unit>()
            val stream = composeHtmlToStreamAsync(AsyncHtmlRenderOptions()) {
                Table { Tbody {
                    Suspense(fallback = { Tr { Td { Text("loading") } } }) {
                        val ready by produceState(false) { release.await(); value = true }
                        if (!ready) SuspensePending() else Tr { Td { Text("</script><b>ready</b>") } }
                    }
                } }
            }
            try {
                stream.chunks.collect { chunk ->
                    appendChunk(root, chunk)
                    if (root.textContent!!.contains("loading")) release.complete(Unit)
                }
                assertEquals("</script><b>ready</b>", root.textContent)
                assertEquals(1, root.querySelectorAll("tbody > tr").length)
                assertEquals(0, root.querySelectorAll("b").length)
                val row = root.querySelector("tr")
                val composition = hydrateComposable(root, validateStrictly = true, onHydrationMismatch = { throw it }) {
                    Table { Tbody { Suspense(fallback = {}) { Tr { Td { Text("</script><b>ready</b>") } } } } }
                }
                try { assertSame(row, root.querySelector("tr")) } finally { composition.dispose() }
            } finally { stream.cancel(); root.remove() }
        }
    }

    @Test fun standalonePatcherPreservesSvgNamespace() = MainScope().promise {
        withTimeout(5000) {
            val namespace = "http://www.w3.org/2000/svg"
            setWindowIntProperty("SuspenseSvgScriptRuns", 0)
            val root = browserDocument.createElement("div") as HTMLElement
            browserDocument.body!!.appendChild(root)
            val release = CompletableDeferred<Unit>()
            val stream = composeHtmlToStreamAsync(AsyncHtmlRenderOptions()) {
                TagElementNS<Element>("svg", namespace, null) {
                    TagElementNS<Element>("g", namespace, null) {
                        Suspense(fallback = { TagElementNS<Element>("circle", namespace, { attr("id", "loading") }, null) }) {
                            val ready by produceState(false) { release.await(); value = true }
                            if (!ready) SuspensePending()
                            else {
                                TagElementNS<Element>("circle", namespace, { attr("id", "ready") }, null)
                                TagElementNS<Element>("script", namespace, null) { Text("window.SuspenseSvgScriptRuns = 1;") }
                            }
                        }
                    }
                }
            }
            try {
                stream.chunks.collect { chunk -> appendChunk(root, chunk); release.complete(Unit) }
                val circle = root.querySelector("#ready")!!
                assertEquals(namespace, circle.namespaceURI)
                assertEquals("g", circle.parentElement!!.localName)
                assertEquals(0, root.querySelectorAll("#loading").length)
                assertEquals(0, getWindowIntProperty("SuspenseSvgScriptRuns"))
                assertEquals(namespace, root.querySelector("script")!!.namespaceURI)
            } finally { stream.cancel(); root.remove(); clearWindowIntProperty("SuspenseSvgScriptRuns") }
        }
    }

    @Test fun scriptsInReplacementContentRemainInert() = MainScope().promise {
        withTimeout(5000) {
            val root = browserDocument.createElement("div") as HTMLElement
            browserDocument.body!!.appendChild(root)
            setWindowIntProperty("SuspenseHtmlScriptRuns", 0)
            val release = CompletableDeferred<Unit>()
            val stream = composeHtmlToStreamAsync(AsyncHtmlRenderOptions()) {
                Suspense(fallback = { Text("loading") }) {
                    val ready by produceState(false) { release.await(); value = true }
                    if (!ready) SuspensePending()
                    else Script(content = InlineScript("window.SuspenseHtmlScriptRuns = 1;"))
                }
            }
            try {
                stream.chunks.collect { chunk -> appendChunk(root, chunk); release.complete(Unit) }
                assertEquals(0, getWindowIntProperty("SuspenseHtmlScriptRuns"))
                assertEquals("window.SuspenseHtmlScriptRuns = 1;", root.querySelector("script")!!.textContent)
            } finally { stream.cancel(); root.remove(); clearWindowIntProperty("SuspenseHtmlScriptRuns") }
        }
    }

    @Test fun streamedRootRejectsEarlyHydrationAndTransfersFinalState() = MainScope().promise {
        withTimeout(5000) {
            val root = browserDocument.createElement("div") as HTMLElement
            browserDocument.body!!.appendChild(root)
            val release = CompletableDeferred<Unit>()
            val stream = composeHtmlToStreamAsync(AsyncHtmlRenderOptions()) {
                var ready by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { release.await(); ready = true }
                HydrationRoot(ready, Boolean::toString, hydrationId = "suspense-test") {
                    Suspense(fallback = { Text("loading") }) { if (!it) SuspensePending() else Span { Text("ready") } }
                }
            }
            try {
                var shell = true
                stream.chunks.collect { chunk ->
                    appendChunk(root, chunk)
                    if (shell) {
                        shell = false
                        assertFailsWith<HydrationStateException> {
                            hydrateRoot(String::toBoolean, within = root, hydrationId = "suspense-test") { }
                        }
                        release.complete(Unit)
                    }
                }
                var initialState = false
                val span = root.querySelector("span")
                val composition = hydrateRoot(String::toBoolean, within = root, hydrationId = "suspense-test",
                    onHydrationMismatch = { throw it }) {
                    initialState = it
                    Suspense(fallback = {}) { Span { Text(if (it) "ready" else "wrong") } }
                }
                try {
                    assertTrue(initialState)
                    assertSame(span, root.querySelector("span"))
                } finally { composition.dispose() }
            } finally { stream.cancel(); root.remove() }
        }
    }

    @Test fun standalonePatcherIndexesNestedReplacementsAndIndependentStreams() = MainScope().promise {
        withTimeout(5000) {
            val root = browserDocument.createElement("div") as HTMLElement
            browserDocument.body!!.appendChild(root)
            val outer = CompletableDeferred<Unit>()
            val inner = CompletableDeferred<Unit>()
            val other = CompletableDeferred<Unit>()
            val completedStreams = mutableListOf<String>()
            val onComplete: (kotlinx.browser.dom.events.Event) -> Unit = {
                completedStreams += streamEventIdentifierPrefix(it)
            }
            browserDocument.addEventListener("compose-stream-complete", onComplete)
            val first = composeHtmlToStreamAsync(AsyncHtmlRenderOptions(identifierPrefix = "first")) {
                Suspense(fallback = { Text("outer loading") }) {
                    val ready by produceState(false) {
                        outer.await()
                        value = true
                    }
                    if (!ready) SuspensePending()
                    Span { Text("outer ready") }
                    Suspense(fallback = { Text("inner loading") }) {
                        val nestedReady by produceState(false) {
                            inner.await()
                            value = true
                        }
                        if (!nestedReady) SuspensePending() else Span { Text("inner ready") }
                    }
                }
            }
            val second = composeHtmlToStreamAsync(AsyncHtmlRenderOptions(identifierPrefix = "second")) {
                Suspense(fallback = { Text("other loading") }) {
                    val ready by produceState(false) {
                        other.await()
                        value = true
                    }
                    if (!ready) SuspensePending() else Span { Text("other ready") }
                }
            }
            try {
                val a = launch {
                    first.chunks.collect { chunk ->
                        appendChunk(root, chunk)
                        if (root.textContent!!.contains("inner loading")) inner.complete(Unit)
                        else outer.complete(Unit)
                    }
                }
                val b = launch {
                    second.chunks.collect {
                        appendChunk(root, it)
                        other.complete(Unit)
                    }
                }
                a.join()
                b.join()
                assertContains(root.textContent!!, "outer readyinner ready")
                assertContains(root.textContent!!, "other ready")
                assertFalse(root.textContent!!.contains("loading"))
                assertEquals(listOf("first", "second"), completedStreams.sorted())
            } finally {
                first.cancel()
                second.cancel()
                root.remove()
                browserDocument.removeEventListener("compose-stream-complete", onComplete)
            }
        }
    }

    @Test fun postShellFailureSignalsThePageAndKeepsHydrationBlocked() = MainScope().promise {
        withTimeout(5000) {
            val root = browserDocument.createElement("div") as HTMLElement
            browserDocument.body!!.appendChild(root)
            val release = CompletableDeferred<Unit>()
            var errors = 0
            val listener: (kotlinx.browser.dom.events.Event) -> Unit = { errors++ }
            browserDocument.addEventListener("compose-stream-error", listener)
            val stream = composeHtmlToStreamAsync(AsyncHtmlRenderOptions()) {
                HydrationRoot(false, Boolean::toString, hydrationId = "failed-stream") {
                    Suspense(fallback = { Text("loading") }) {
                        LaunchedEffect(Unit) {
                            release.await()
                            error("private producer failure")
                        }
                        SuspensePending()
                    }
                }
            }
            try {
                assertFailsWith<IllegalStateException> {
                    stream.chunks.collect {
                        appendChunk(root, it)
                        release.complete(Unit)
                    }
                }
                assertEquals(1, errors)
                assertEquals("loading", root.querySelector("[$HydrationRootAttribute]")!!.textContent)
                assertTrue(root.querySelector("[$HydrationRootAttribute]")!!.hasAttribute(StreamErrorAttribute))
                val failure = assertFailsWith<HydrationStateException> {
                    hydrateRoot(String::toBoolean, within = root, hydrationId = "failed-stream") { }
                }
                assertContains(failure.message!!, "streaming failed")
            } finally {
                stream.cancel()
                root.remove()
                browserDocument.removeEventListener("compose-stream-error", listener)
            }
        }
    }
}

// insertAdjacentHTML leaves scripts inert. Recreate only the stream patch scripts.
private fun appendChunk(root: Element, chunk: String) {
    root.insertAdjacentHTML("beforeend", chunk)
    val scripts = root.querySelectorAll("script[data-compose-stream-script]:not([data-test-executed])")
    for (index in 0 until scripts.length) {
        val original = scripts.item(index) as Element
        original.setAttribute("data-test-executed", "true")
        val executable = browserDocument.createElement("script")
        original.getAttribute("nonce")?.let { executable.setAttribute("nonce", it) }
        executable.textContent = original.textContent
        browserDocument.body!!.appendChild(executable)
        executable.remove()
        original.remove()
    }
}

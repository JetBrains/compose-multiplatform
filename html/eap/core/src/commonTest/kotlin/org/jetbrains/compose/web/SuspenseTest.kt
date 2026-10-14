/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import org.jetbrains.compose.web.dom.*
import kotlin.test.*

class SuspenseTest {
    @Test fun readyContentDoesNotComposeFallback() {
        var fallbacks = 0
        assertEquals("<span>ready</span>", composeHtmlToString(hydratable = false) {
            Suspense(fallback = { fallbacks++; Text("loading") }) { Span { Text("ready") } }
        })
        assertEquals(0, fallbacks)
    }

    @Test fun pendingContentContinuesThroughSiblingsAndDisposesOnce() {
        var composed = 0
        var disposed = 0
        var effects = 0
        assertEquals("loading", composeHtmlToString(hydratable = false) {
            Suspense(fallback = { Text("loading") }) {
                SuspensePending()
                composed++
                DisposableEffect(Unit) { onDispose { disposed++ } }
                LaunchedEffect(Unit) { effects++ }
                Text("hidden")
            }
        })
        assertEquals(1, composed)
        assertEquals(1, disposed)
        assertEquals(0, effects)
    }

    @Test fun nestedBoundariesContainPendingState() {
        assertEquals("beforeinnerafter", composeHtmlToString(hydratable = false) {
            Suspense(fallback = { Text("outer") }) {
                Text("before")
                Suspense(fallback = { Text("inner") }) { SuspensePending() }
                Text("after")
            }
        })
    }

    @Test fun fallbackPendingBelongsToOuterBoundary() {
        assertEquals("outer", composeHtmlToString(hydratable = false) {
            Suspense(fallback = { Text("outer") }) {
                Suspense(fallback = { SuspensePending(); Text("inner") }) { SuspensePending() }
            }
        })
    }

    @Test fun siblingBoundariesRevealIndependently() {
        assertEquals("firstsecond", composeHtmlToString(hydratable = false) {
            Suspense(fallback = { Text("first") }) { SuspensePending() }
            Suspense(fallback = { error("must not compose") }) { Text("second") }
        })
    }

    @Test fun boundaryCanWrapTableRows() {
        assertEquals("<table><tbody><tr><td>loading</td></tr></tbody></table>", composeHtmlToString(hydratable = false) {
            Table { Tbody { Suspense(fallback = { Tr { Td { Text("loading") } } }) { SuspensePending() } } }
        })
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString { Table { Suspense(fallback = {}) { Tr { Td { Text("invalid") } } } } }
        }
    }

    @Test fun synchronousStreamingMatchesStringIncludingNestedHydrationMarkers() {
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            Text("a")
            Suspense(fallback = {}) {
                Text("b")
                Suspense(fallback = {}) { Text("c") }
                Text("d")
            }
            Text("e")
        }
        val html = composeHtmlToString(content = content)
        val chunks = mutableListOf<String>()
        composeHtmlToStream(chunks::add, chunkSize = 1, content = content)
        assertEquals(html, chunks.joinToString(""))
        assertEquals(2, Regex("<!--compose-suspense-->").findAll(html).count())
    }

    @Test fun keyedSynchronousRendersResetTheirBoundaryState() {
        for (pending in listOf(true, false, true, false)) {
            assertEquals(if (pending) "loading" else "ready", composeHtmlToString(hydratable = false, key = "suspense-test") {
                Suspense(pending = pending, fallback = { Text("loading") }) { Text("ready") }
            })
        }
    }

    @Test fun explicitPendingKeepsContentComposed() {
        var composed = 0
        assertEquals("loading", composeHtmlToString(hydratable = false) {
            Suspense(pending = true, fallback = { Text("loading") }) { composed++; Text("hidden") }
        })
        assertEquals(1, composed)
    }
}

/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.dom.Body
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Head
import org.jetbrains.compose.web.dom.Html
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Title
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HydratedDocumentStreamTest {
    @Test
    fun documentRenderersUseSinglePassMovableContentWithFreshRememberedState() {
        var remembered = 0
        val content: @Composable () -> Unit = {
            val movable = remember {
                movableContentOf { Div { Text("render ${remember { ++remembered }}") } }
            }
            Html { Body { movable() } }
        }
        assertEquals(
            "<!doctype html><html><body><div>render 1</div></body></html>",
            renderHydratedDocument(content = content),
        )
        val chunks = mutableListOf<String>()
        renderHydratedDocumentToStream(chunks::add, chunkSize = 1, content = content)
        assertEquals("<!doctype html><html><body><div>render 2</div></body></html>", chunks.joinToString(""))
        assertEquals(2, remembered)
    }

    @Test
    fun defaultChunkSizeEmitsAt2048CodeUnitsDuringComposition() {
        val text = "x".repeat(2048)
        val chunks = mutableListOf<String>()
        renderHydratedDocumentToStream(sink = chunks::add) {
            Html {
                Body {
                    Text(text)
                    assertEquals(listOf("<!doctype html><html><body>$text"), chunks)
                }
            }
        }
        assertEquals(listOf("<!doctype html><html><body>$text", "</body></html>"), chunks)
    }

    @Test
    fun matchesCompleteStringForMultipleRootsAndAdversarialStateAtEveryChunkSize() {
        val state = "😀<value>&lt;</script>\r\u0000</value>"
        val content: @Composable () -> Unit = {
            Html {
                Head { Title { Text("Streamed <page>") } }
                Body {
                    HydrationRoot(state, { it }, rootAttrs = { classes("application") }, hydrationId = "application") {
                        Text("first<&>")
                        Text("adjacent😀")
                    }
                    HydrationRoot(42, { it.toString() }, hydrationId = "counter") { Text(it.toString()) }
                }
            }
        }
        for (strict in listOf(false, true)) {
            val expected = renderHydratedDocument(strict, content)
            for (size in listOf(1, 4, 32, 2048, 4096, Int.MAX_VALUE)) {
                val chunks = mutableListOf<String>()
                renderHydratedDocumentToStream(chunks::add, size, strict, content)
                val rendered = chunks.joinToString("")
                assertEquals(expected, rendered)
                assertTrue(rendered.startsWith("<!doctype html><html>"))
                assertTrue(chunks.all { it.isNotEmpty() })
                assertContains(rendered, "first&lt;&amp;&gt;<!--c-->adjacent😀")
                assertContains(rendered, "&lt;value>&amp;lt;&lt;/script>&#13;&#0;&lt;/value>")
                assertEquals(strict, HydrationValidationAttribute in rendered)
            }
        }
    }

    @Test
    fun duplicateNamedRootsFailOnlyInStrictMode() {
        val content: @Composable () -> Unit = {
            Html {
                Body {
                    HydrationRoot(Unit, { "first" }, hydrationId = "cart") {}
                    HydrationRoot(Unit, { "second" }, hydrationId = "cart") {}
                }
            }
        }
        val failure = assertFailsWith<IllegalArgumentException> {
            renderHydratedDocumentToStream({}, chunkSize = 1, validateStrictly = true, content = content)
        }
        assertContains(failure.message.orEmpty(), "Duplicate Compose hydrationId \"cart\"")

        val chunks = mutableListOf<String>()
        renderHydratedDocumentToStream(chunks::add, chunkSize = 1, validateStrictly = false, content = content)
        assertEquals(renderHydratedDocument(validateStrictly = false, content = content), chunks.joinToString(""))
    }

    @Test
    fun emitsBeforeCompositionFinishesAndSerializesEachStateExactlyOnceBeforeItsContent() {
        val chunks = mutableListOf<String>()
        val calls = mutableListOf<String>()
        renderHydratedDocumentToStream(chunks::add, chunkSize = 1) {
            Html {
                Body {
                    HydrationRoot("snapshot", {
                        calls += "serialize:$it"
                        it
                    }) {
                        calls += "content:$it"
                        Text(it)
                        assertTrue(chunks.joinToString("").endsWith("snapshot"))
                    }
                    assertTrue(chunks.joinToString("").endsWith("snapshot</script>"))
                }
            }
        }
        assertEquals(listOf("serialize:snapshot", "content:snapshot"), calls)
        assertTrue(chunks.size > 1)
        assertEquals("<!doctype html>", chunks.first())
    }

    @Test
    fun staticDocumentsDoNotShipHydrationStateAndLargeTargetsProduceOneChunk() {
        val chunks = mutableListOf<String>()
        renderHydratedDocumentToStream(chunks::add, chunkSize = Int.MAX_VALUE) {
            Html { Body { Text("Static page") } }
        }
        assertEquals(listOf("<!doctype html><html><body>Static page</body></html>"), chunks)
        assertFalse(HydrationStateAttribute in chunks.single())
    }

    @Test
    fun validatesDocumentShapeInBothModesIncludingWhenOutputHasAlreadyBeenSent() {
        val invalidDocuments = listOf<@Composable () -> Unit>(
            {},
            { Text("fragment") },
            { Div() },
            { Html(); Html() },
            { Html(); Text("trailing text") },
        )
        for (strict in listOf(false, true)) {
            for (size in listOf(1, Int.MAX_VALUE)) {
                for (content in invalidDocuments) {
                    val chunks = mutableListOf<String>()
                    val failure = assertFailsWith<IllegalArgumentException> {
                        renderHydratedDocumentToStream(chunks::add, size, strict, content)
                    }
                    assertContains(failure.message.orEmpty(), "exactly one html element")
                    if (size == Int.MAX_VALUE) assertTrue(chunks.isEmpty())
                }
            }
        }
        val partial = mutableListOf<String>()
        assertFailsWith<IllegalArgumentException> {
            renderHydratedDocumentToStream(partial::add, chunkSize = 1) { Div() }
        }
        assertEquals("<!doctype html><div></div>", partial.joinToString(""))
    }

    @Test
    fun strictModeValidatesContentAndTheDefaultModeIsTransported() {
        val invalidClasses: @Composable () -> Unit = {
            Html { Body { HydrationRoot(Unit, { "null" }) { Div({ classes("two words") }) } } }
        }
        assertFailsWith<IllegalArgumentException> {
            renderHydratedDocumentToStream({}, validateStrictly = true, content = invalidClasses)
        }
        renderHydratedDocumentToStream({}, validateStrictly = false, content = invalidClasses)

        val chunks = mutableListOf<String>()
        renderHydratedDocumentToStream(chunks::add) {
            Html { Body { HydrationRoot(Unit, { "null" }) {} } }
        }
        assertEquals(
            defaultHtmlValidationMode() == HtmlValidationMode.Strict,
            HydrationValidationAttribute in chunks.joinToString(""),
        )
    }

    @Test
    fun rejectsInvalidChunkSizesBeforeComposingOrEmitting() {
        val chunks = mutableListOf<String>()
        var composed = false
        for (size in listOf(0, -1)) {
            assertFailsWith<IllegalArgumentException> {
                renderHydratedDocumentToStream(chunks::add, size) { composed = true; Html() }
            }
        }
        assertFalse(composed)
        assertTrue(chunks.isEmpty())
    }

    @Test
    fun discardsSnapshotChangesAndDisposesTheCompositionEvenWhenTheSinkFails() {
        for (fail in listOf(false, true)) {
            val sharedState = mutableStateOf("initial")
            var disposals = 0
            val sinkFailure = IllegalStateException("sink failed")
            val chunks = mutableListOf<String>()
            val render = {
                renderHydratedDocumentToStream(
                    sink = { if (fail) throw sinkFailure else chunks.add(it) },
                    chunkSize = Int.MAX_VALUE,
                ) {
                    DisposableEffect(Unit) { onDispose { disposals++ } }
                    sharedState.value = "rendered"
                    Html { Body { HydrationRoot(sharedState.value, { it }) { Text(it) } } }
                }
            }
            if (fail) assertSame(sinkFailure, assertFailsWith<IllegalStateException> { render() })
            else render()
            assertEquals("initial", sharedState.value)
            assertEquals(1, disposals)
            if (!fail) assertContains(chunks.single(), "rendered</script>")
        }
    }

    @Test
    fun rejectsHydrationRootOutsideTheDocumentRenderer() {
        assertFailsWith<IllegalStateException> {
            composeHtmlToStream({}) { HydrationRoot(Unit, { "null" }) {} }
        }
    }
}

/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class DirectStringRenderingTest {
    @Test
    fun keyedRendersMatchFreshRendersAcrossChangingRequests() {
        for (strict in listOf(false, true)) {
            for (hydratable in listOf(false, true)) {
                for (request in listOf(0, 1, 2, 0, 3, 1)) {
                    val content: @Composable () -> Unit = {
                        Div(attrs = {
                            id("request-$request")
                            attr("data-text", "a\"&<>'\n\t\ré😀")
                            if (request % 2 == 0) {
                                classes("one", "two")
                                style { color(Color.red) }
                            }
                        }) {
                            for (item in (0..request).reversed()) key(item) {
                                TagElement<Element>(if (item % 2 == 0) "span" else "strong", null) {
                                    Text("$item &<>\ré😀")
                                    Text("")
                                    Text("tail")
                                }
                            }
                            TagElement<Element>("textarea", null) { Text("\nfirst"); Text("second") }
                            TagElement<Element>("script", null) { Text("const x = '<&';") }
                            TagElement<Element>("noscript", null) { Span { Text("fallback") } }
                            TagElementNS<Element>("svg", "http://www.w3.org/2000/svg", { attr("viewBox", "0 0 1 1") }, null)
                        }
                    }
                    val expected = composeHtmlToString(hydratable = hydratable, validateStrictly = strict, content = content)
                    assertEquals(expected, composeHtmlToString(hydratable, "direct-changing", strict, content))
                }
            }
        }
    }

    @Test
    fun sameContentLambdaClearsAttributesAndPreviousOutput() {
        var value = ""
        var decorated = true
        val content: @Composable () -> Unit = {
            Div(attrs = if (decorated) { { id("decorated") } } else null) { Text(value) }
        }
        for ((index, text) in listOf("Unicode é😀 &<>".repeat(10_000), "short", "", "again").withIndex()) {
            value = text
            decorated = index % 2 == 0
            assertEquals(
                composeHtmlToString(content = content),
                composeHtmlToString(key = "direct-same-lambda", content = content),
            )
        }
    }

    @Test
    fun preservesLocalsSnapshotsAndEffectLifecycle() {
        val local = staticCompositionLocalOf { "default" }
        val state = mutableStateOf("outside")
        var remembered = 0
        var launched = false
        val effects = mutableListOf<String>()
        repeat(3) { request ->
            val html = composeHtmlToString(key = "direct-effects") {
                state.value = "$request"
                val value = remember { ++remembered }
                CompositionLocalProvider(local provides "local-$request") {
                    Div {
                        Text("${local.current}:${state.value}:$value")
                        DisposableEffect(Unit) {
                            effects.add("enter $request")
                            onDispose { effects.add("dispose $request") }
                        }
                        SideEffect { effects.add("side $request") }
                        LaunchedEffect(Unit) { launched = true }
                    }
                }
            }
            assertEquals("<div>local-$request:$request:${request + 1}</div>", html)
            assertEquals("outside", state.value)
        }
        assertEquals((0..2).flatMap { listOf("enter $it", "side $it", "dispose $it") }, effects)
        assertFalse(launched)
    }

    @Test
    fun movableContentHasTheExistingOneShotRendererLimitation() {
        val movable = movableContentOf { Span { Text("moved") } }
        val content: @Composable () -> Unit = { Div { movable() }; P { Text("after") } }
        // The cancelled SSR recomposer has never inserted deferred movable content.
        assertEquals("<div></div><p>after</p>", composeHtmlToString(content = content))
        assertEquals("<div></div><p>after</p>", composeHtmlToString(key = "movable", content = content))
    }

    @Test
    fun rejectsInvalidHtmlAndRecoversAfterFailure() {
        val invalid: List<@Composable () -> Unit> = listOf(
            { Div { Text("bad\u0000text") } },
            { Div({ attr("data-value", "bad\u0000attribute") }) },
            { TagElement<Element>("script", null) { Text("</scr"); Text("ipt>") } },
            { TagElement<Element>("style", null) { Span { Text("child") } } },
            { Table { Tr { Td { Text("missing tbody") } } } },
        )
        for (content in invalid) {
            assertFailsWith<IllegalArgumentException> { composeHtmlToString(content = content) }
            assertFailsWith<IllegalArgumentException> { composeHtmlToString(key = "direct-failure", content = content) }
            assertEquals("<div>ok</div>", composeHtmlToString(key = "direct-failure") { Div { Text("ok") } })
        }
    }

    @Test
    fun capturedCompositionLocalsAndCompositionContextsRemainSupported() {
        val language = staticCompositionLocalOf { "en" }
        val content: @Composable () -> Unit = {
            CompositionLocalProvider(language provides "de") {
                val captured = currentCompositionLocalContext
                rememberCompositionContext()
                CompositionLocalProvider(language provides "fr") {
                    CompositionLocalProvider(captured) {
                        P { Text(language.current) }
                    }
                }
            }
        }
        assertEquals("<p>de</p>", composeHtmlToString(content = content))
        repeat(2) {
            assertEquals("<p>de</p>", composeHtmlToString(key = "captured-locals", content = content))
        }
    }

    @Test
    fun nestedRenderRestoresContextAndSameKeyReentryIsRejected() {
        assertEquals("<div>outer<!--c-->&lt;span&gt;inner&lt;/span&gt;<!--c-->tail</div>",
            composeHtmlToString(key = "outer") {
                Div {
                    Text("outer")
                    Text(composeHtmlToString(key = "inner") { Span { Text("inner") } })
                    Text("tail")
                }
            })
        assertFailsWith<IllegalStateException> {
            composeHtmlToString(key = "reentrant") {
                composeHtmlToString(key = "reentrant") { Text("bad") }
            }
        }
        assertEquals("ok", composeHtmlToString(key = "reentrant") { Text("ok") })
    }

}

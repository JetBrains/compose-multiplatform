/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web

import androidx.compose.runtime.*
import kotlinx.browser.dom.Element
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.web.dom.*
import kotlin.test.*

class SinglePassRenderingTest {
    @Test
    fun changingKeyedRequestsMatchFreshRendersAcrossModesAndNamespaces() {
        for (strict in listOf(false, true)) for (hydratable in listOf(false, true)) {
            for (request in listOf(0, 1, 7, 2, 0, 5, 1)) {
                val content: @Composable () -> Unit = {
                    Div(attrs = { if (request % 2 == 0) classes("one", "two"); attr("data-x", "<&\"é😀") }) {
                        for (item in (0..request).reversed()) key(item) {
                            TagElement<Element>(if (item % 2 == 0) "span" else "strong", null) {
                                Text("$item &<>\ré😀"); Text(""); Text("tail")
                            }
                        }
                        TagElement<Element>("textarea", null) { Text("\nfirst"); Text("second") }
                        TagElement<Element>("script", null) { Text("const x = '<&';") }
                        TagElement<Element>("noscript", null) { Span { Text("fallback") } }
                        TagElementNS<Element>("svg", "http://www.w3.org/2000/svg", { attr("viewBox", "0 0 1 1") }, null)
                    }
                }
                val expected = composeHtmlToString(hydratable, validateStrictly = strict, content = content)
                assertEquals(expected, composeHtmlToString(hydratable, "changing", strict, content))
            }
        }
    }

    @Test
    fun streamingMatchesStringRendering() {
        val chunks = mutableListOf<String>()
        composeHtmlToStream(sink = chunks::add, chunkSize = 8, key = "common-stream") {
            Div { Text("portable <&> output") }
        }
        assertEquals(
            composeHtmlToString(key = "common-string") { Div { Text("portable <&> output") } },
            chunks.joinToString(""),
        )
        assertTrue(chunks.size > 1)
    }

    @Test
    fun stringRenderingDoesNotRequireAPositiveChunkSize() {
        for (size in listOf(0, -1)) {
            assertEquals("<div>complete</div>", composeHtmlString(chunkSize = size) {
                Div { Text("complete") }
            })
        }
    }

    @Test
    fun nestedComputedDefaultAndNullableLocalsRestoreTheirScopes() {
        val number = compositionLocalOf { 2 }
        val twice = compositionLocalWithComputedDefaultOf { number.currentValue * 2 }
        val nullable = staticCompositionLocalOf<String?> { "default" }
        val content: @Composable () -> Unit = {
            Text("${number.current}/${twice.current}/${nullable.current};")
            CompositionLocalProvider(number provides 3, nullable provides null) {
                Text("${number.current}/${twice.current}/${nullable.current};")
                CompositionLocalProvider(number providesDefault 99, twice providesComputed { number.currentValue * 10 }) {
                    Text("${number.current}/${twice.current};")
                    CompositionLocalProvider(number provides 7) { Text("${twice.current};") }
                }
                Text("${twice.current};")
            }
            Text("${number.current}/${twice.current}/${nullable.current}")
        }
        assertEquals(
            "2/4/default;<!--c-->3/6/null;<!--c-->3/30;<!--c-->70;<!--c-->6;<!--c-->2/4/default",
            composeHtmlToString(content = content),
        )
    }

    @Test
    fun sameLambdaFreshRememberAndSnapshotIsolation() {
        val state = mutableStateOf("outside")
        var value = ""
        var calls = 0
        val content: @Composable () -> Unit = {
            val remembered = remember { ++calls }
            state.value = value
            Div { Text("${state.value}:$remembered") }
        }
        for ((i, next) in listOf("😀<&".repeat(10_000), "short", "", "next").withIndex()) {
            value = next
            val expected = composeHtmlToString { Div { Text("$value:${i + 1}") } }
            assertEquals(expected, composeHtmlToString(key = "same-lambda", content = content))
            assertEquals("outside", state.value)
        }
        assertEquals(4, calls)
    }

    @Test
    fun observersAndEffectsPreserveLifecycleOrdering() {
        val events = mutableListOf<String>()
        composeHtmlToString {
            remember { Observer("outer", events) }
            SideEffect { events.add("side outer") }
            Div {
                remember { Observer("inner", events) }
                DisposableEffect(Unit) { events.add("effect"); onDispose { events.add("dispose") } }
                SideEffect { events.add("side inner") }
            }
        }
        assertEquals(
            listOf(
                "remember outer", "remember inner", "effect", "side outer", "side inner",
                "dispose", "forget inner", "forget outer",
            ),
            events,
        )
    }

    @Test
    fun failedCompositionAbandonsAndPreservesOriginalException() {
        val events = mutableListOf<String>()
        val original = IllegalArgumentException("user failure")
        val cleanup = IllegalStateException("cleanup failure")
        val thrown = assertFailsWith<IllegalArgumentException> {
            composeHtmlToString(key = "failed-composition") {
                remember { Observer("one", events) }
                remember { object : RememberObserver {
                    override fun onRemembered() { error("should not run") }
                    override fun onForgotten() { error("should not run") }
                    override fun onAbandoned() { throw cleanup }
                } }
                SideEffect { error("should not run") }
                throw original
            }
        }
        assertSame(original, thrown)
        assertTrue(cleanup in thrown.suppressedExceptions)
        assertEquals(listOf("abandon one"), events)
        assertEquals("<p>recovered</p>", composeHtmlToString(key = "failed-composition") { P { Text("recovered") } })
    }

    @Test
    fun failedSideEffectStillDisposesEveryObserver() {
        val events = mutableListOf<String>()
        assertFailsWith<IllegalStateException> {
            composeHtmlToString {
                remember { Observer("one", events) }
                remember { Observer("two", events) }
                SideEffect { error("side effect failure") }
            }
        }
        assertEquals(listOf("remember one", "remember two", "forget two", "forget one"), events)
    }

    @Test
    fun unsupportedOperationsFailExplicitlyWithoutRetryingUserCode() {
        var executions = 0
        assertFailsWith<UnsupportedOperationException> {
            composeHtmlToString { executions++; rememberCompositionContext() }
        }
        assertEquals(1, executions)
        assertFailsWith<UnsupportedOperationException> { composeHtmlToString { currentCompositionLocalContext } }
        assertFailsWith<IllegalStateException> { composeHtmlToString { currentRecomposeScope } }
        assertFailsWith<IllegalStateException> { composeHtmlToStream(sink = {}) { currentRecomposeScope } }
    }

    @Test
    fun rememberedMovableContentRendersInEitherParent() {
        for (column in listOf(true, false, true)) for (stream in listOf(false, true)) {
            for (renderKey in listOf(null, "remembered-movable")) {
                var creations = 0
                var bodyExecutions = 0
                val content: @Composable () -> Unit = {
                    val someContent = remember {
                        creations++
                        movableContentOf {
                            bodyExecutions++
                            Div { Text("Text in a Movable Div") }
                        }
                    }
                    P { Text("before") }
                    // Separate call sites model the Column/Row branches without requiring
                    // Compose UI layout components in a Compose HTML test.
                    if (column) {
                        Div(attrs = { classes("column") }) { someContent() }
                    } else {
                        Div(attrs = { classes("row") }) { someContent() }
                    }
                    P { Text("after") }
                }
                val html = if (stream) {
                    val chunks = mutableListOf<String>()
                    composeHtmlToStream(sink = chunks::add, chunkSize = 8, key = renderKey, content = content)
                    assertTrue(chunks.size > 1)
                    chunks.joinToString("")
                } else {
                    composeHtmlToString(key = renderKey, content = content)
                }
                val parentClass = if (column) "column" else "row"
                assertEquals(
                    "<p>before</p><div class=\"$parentClass\"><div>Text in a Movable Div</div></div><p>after</p>",
                    html,
                )
                assertEquals(1, creations)
                assertEquals(1, bodyExecutions)
            }
        }
    }

    @Test
    fun rememberingMovableContentWithoutInvokingItDoesNotFail() {
        var creations = 0
        val html = composeHtmlToString {
            remember {
                creations++
                movableContentOf { error("unused movable content must not execute") }
            }
            Div { Text("ordinary content") }
        }
        assertEquals("<div>ordinary content</div>", html)
        assertEquals(1, creations)
    }

    @Test
    fun nestedParameterizedMovableContentPreservesLocalsAndEffectLifecycle() {
        val local = staticCompositionLocalOf { "default" }
        fun render(movable: Boolean): Pair<String, List<String>> {
            val events = mutableListOf<String>()
            var remembers = 0
            val content: @Composable () -> Unit = {
                val leaf: @Composable (String) -> Unit = { value ->
                    val number = remember { ++remembers }
                    val label = "${local.current}:$value:$number"
                    remember { Observer(label, events) }
                    DisposableEffect(Unit) {
                        events.add("effect $label")
                        onDispose { events.add("dispose $label") }
                    }
                    SideEffect { events.add("side $label") }
                    Span { Text(label); Text("<&") }
                }
                val child = if (movable) remember { movableContentOf(leaf) } else leaf
                val parent: @Composable (String, Int) -> Unit = { value, count ->
                    Div { repeat(count) { child(value) } }
                }
                val outer = if (movable) remember { movableContentOf(parent) } else parent
                CompositionLocalProvider(local provides "first") { outer("A", 2) }
                CompositionLocalProvider(local provides "second") { outer("B", 1) }
                P { Text(local.current) }
            }
            val html = composeHtmlToString(content = content)
            assertEquals(3, remembers)
            return html to events
        }
        assertEquals(render(movable = false), render(movable = true))
    }

    @Test
    fun movableContentSupportsReceiverAndNullableParameters() {
        val receiver = movableContentWithReceiverOf<String> { Text(this) }
        val nullable = movableContentOf<String?> { Text(it ?: "null") }
        assertEquals("<div>receiver<!--c-->null</div>", composeHtmlToString {
            Div { receiver("receiver"); nullable(null) }
        })
    }

    @Test
    fun failedMovableContentAbandonsAndAllowsRecovery() {
        val events = mutableListOf<String>()
        val original = IllegalArgumentException("movable failure")
        val movable = movableContentOf {
            remember { Observer("movable", events) }
            SideEffect { error("failed content must not run effects") }
            Div { throw original }
        }
        assertSame(original, assertFailsWith<IllegalArgumentException> {
            composeHtmlToString(key = "failed-movable") { Div { movable() } }
        })
        assertEquals(listOf("abandon movable"), events)
        assertEquals("<p>recovered</p>", composeHtmlToString(key = "failed-movable") {
            P { Text("recovered") }
        })
    }

    @Test
    fun movableContentHashesIgnoreParentsAndRestoreOuterHash() {
        val hashes = mutableListOf<Long>()
        val movable = movableContentOf { hashes.add(currentCompositeKeyHashCode.toLong()) }
        composeHtmlToString {
            val outerHash = currentCompositeKeyHashCode
            Div { movable() }
            assertEquals(outerHash, currentCompositeKeyHashCode)
            P { movable() }
            assertEquals(outerHash, currentCompositeKeyHashCode)
        }
        assertEquals(2, hashes.size)
        assertEquals(hashes[0], hashes[1])
    }

    @Test
    fun compositeHashesAreStableAndDistinguishKeyedChildren() {
        fun hashes(order: List<Int>): Map<Int, Long> {
            val result = mutableMapOf<Int, Long>()
            composeHtmlToString {
                for (item in order) key(item) { result[item] = currentCompositeKeyHashCode.toLong() }
            }
            return result
        }
        val first = hashes(listOf(1, 2, 3))
        assertEquals(3, first.values.toSet().size)
        assertEquals(first, hashes(listOf(3, 2, 1)))
    }

    @Test
    @OptIn(InternalComposeApi::class)
    fun compositeHashesMatchRuntimeKeyRules() {
        fun hashes(composer: Composer): List<Long> {
            val hashes = mutableListOf<Long>()
            val parent = composer.compositeKeyHashCode.toLong()
            // Remove the runtime's own root groups to compare the same child groups.
            val parentContribution = (parent shl 6) or (parent ushr 58)
            fun record() {
                hashes.add(composer.compositeKeyHashCode.toLong() xor parentContribution)
            }
            val keys = listOf(
                null, HashKey.First, HashKey.Second, "key", 7,
                composer.joinKey(HashKey.Second, HashKey.First),
                composer.joinKey(composer.joinKey(HashKey.First, 7), HashKey.Second),
            )
            for (key in keys) {
                composer.startMovableGroup(91, key)
                record()
                composer.endMovableGroup()
                composer.startReplaceGroup(92)
                record()
                composer.endReplaceGroup()
            }
            for (key in listOf(null, 7, HashKey.Second, Composer.Empty)) {
                composer.startReusableGroup(207, key)
                record()
                composer.endReusableGroup()
            }
            composer.startReusableGroup(93, "auxiliary")
            record()
            composer.endReusableGroup()
            assertEquals(parent, composer.compositeKeyHashCode.toLong())
            return hashes
        }
        val recomposer = Recomposer(Dispatchers.Unconfined)
        val composition = ControlledComposition(object : AbstractApplier<Unit>(Unit) {
            override fun insertTopDown(index: Int, instance: Unit) {}
            override fun insertBottomUp(index: Int, instance: Unit) {}
            override fun remove(index: Int, count: Int) {}
            override fun move(from: Int, to: Int, count: Int) {}
            override fun onClear() {}
        }, recomposer)
        try {
            var expected = emptyList<Long>()
            composition.setContent { expected = hashes(currentComposer) }
            assertEquals(expected, hashes(SinglePassComposer()))
        } finally {
            composition.dispose()
            recomposer.cancel()
        }
    }

    @Test
    fun compositeHashesRemainStableAcrossRepeatedKeyedRenders() {
        val content: @Composable () -> Unit = {
            Text(currentCompositeKeyHashCode.toLong().toString())
            for (item in listOf(1, 2, 3)) key(item) {
                Div { Text(currentCompositeKeyHashCode.toLong().toString()) }
            }
        }
        val expected = composeHtmlToString(key = "stable-request-hashes", content = content)
        repeat(5) {
            assertEquals(expected, composeHtmlToString(key = "stable-request-hashes", content = content))
        }
    }

    @Test
    fun nestedRenderRestoresContextWithTheSameOrDifferentKey() {
        for (innerKey in listOf("inner", "outer")) {
            assertEquals("<div>outer<!--c-->&lt;span&gt;inner&lt;/span&gt;<!--c-->tail</div>",
                composeHtmlToString(key = "outer") {
                    Div {
                        Text("outer")
                        Text(composeHtmlToString(key = innerKey) { Span { Text("inner") } })
                        Text("tail")
                    }
                })
        }
        assertEquals("ok", composeHtmlToString(key = "outer") { Text("ok") })
    }

    private enum class HashKey { First, Second }

    private class Observer(val name: String, val events: MutableList<String>) : RememberObserver {
        override fun onRemembered() { events.add("remember $name") }
        override fun onForgotten() { events.add("forget $name") }
        override fun onAbandoned() { events.add("abandon $name") }
    }
}

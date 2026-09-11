/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.CSSUnitValue
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.ms
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.css.value
import org.jetbrains.compose.web.css.variable
import org.jetbrains.compose.web.css.keywords.auto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ComposeHtmlToStringTest {
    @Test
    fun rcdataEscapesTextWithoutHydrationBoundaryComments() {
        for (tag in listOf("title", "textarea")) {
            val html = composeHtmlToString {
                TagElement<kotlinx.browser.dom.Element>(tag, null) {
                    Text("A & B\r")
                    Text("")
                    Text("</$tag>")
                }
            }
            assertEquals("<$tag>A &amp; B&#13;&lt;/$tag&gt;</$tag>", html)
        }
    }

    @Test
    fun doesNotStartCompositionScopedCoroutines() {
        val launchedEffectStarted = CompletableDeferred<Unit>()
        val scopeCoroutineStarted = CompletableDeferred<Unit>()
        var sideEffectRan = false

        val html = composeHtmlToString {
            // Unconfined makes an incorrectly active scope execute immediately,
            // so the regression check does not depend on thread scheduling.
            val scope = rememberCoroutineScope { Dispatchers.Unconfined }
            LaunchedEffect(Unit) {
                launchedEffectStarted.complete(Unit)
            }
            SideEffect {
                sideEffectRan = true
                scope.launch { scopeCoroutineStarted.complete(Unit) }
            }
            Div { Text("initial content") }
        }

        assertEquals("<div>initial content</div>", html)
        assertEquals(true, sideEffectRan)
        assertFalse(scopeCoroutineStarted.isCompleted)
        assertFalse(launchedEffectStarted.isCompleted)
    }

    @Test
    fun runsAndDisposesSynchronousEffects() {
        val effects = mutableListOf<String>()

        val html = composeHtmlToString {
            DisposableEffect(Unit) {
                effects.add("enter")
                onDispose { effects.add("dispose") }
            }
            SideEffect { effects.add("side effect") }
            Text("content")
        }

        assertEquals("content", html)
        assertEquals(listOf("enter", "side effect", "dispose"), effects)
    }

    @Test
    fun eventListenersAreNotSerialized() {
        val html = composeHtmlToString {
            Button(attrs = { onClick { } }) {
                Text("Click")
            }
        }

        assertEquals("<button>Click</button>", html)
    }

    /* Negative test: Tests if `allowHydrationMismatch()` does not change serialization of HTML. */
    @Test
    fun hydrationMismatchAllowanceIsNotSerialized() {
        val html = composeHtmlToString {
            Span(attrs = {
                allowHydrationMismatch()
                id("timestamp")
            }) {
                Text("12:00")
            }
        }

        assertEquals("<span id=\"timestamp\">12:00</span>", html)
    }

    @Test
    fun rendersNestedElementsAndRootSiblings() {
        val html = composeHtmlToString {
            Div {
                Text("before")
                Span { Text("inside") }
            }
            Text("after")
        }

        assertEquals(
            "<div>before<span>inside</span></div>after",
            html
        )
    }

    @Test
    fun rendersAttributesStylesAndEscapedText() {
        val html = composeHtmlToString {
            Div({
                id("root")
                classes("first", "second")
                style {
                    property("color", "red")
                    property("display", "block", important = true)
                }
                attr("title", "Tom & \"Jerry\"")
            }) {
                Text("Tom & Jerry <3")
            }
        }

        assertEquals(
            "<div id=\"root\" title=\"Tom &amp; &quot;Jerry&quot;\" " +
                "class=\"first second\" " +
                "style=\"color: red; display: block !important\">" +
                "Tom &amp; Jerry &lt;3</div>",
            html
        )
    }

    @Test
    fun rendersTypedCssUnitValuesInInlineStyles() {
        val html = composeHtmlToString {
            Div({
                style {
                    property("width", 320.px)
                    property("transition-duration", 250.ms)
                }
            })
        }

        assertEquals(
            "<div style=\"width: 320px; transition-duration: 250ms\"></div>",
            html,
        )
    }

    @Test
    fun rendersCommonCssPropertyHelpers() {
        val html = composeHtmlToString {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    color(Color.rebeccapurple)
                    opacity(50.percent)
                    width(320.px)
                    height(auto)
                    padding(8.px, 16.px)
                    margin(4.px, 8.px, 12.px, 16.px)
                }
            })
        }

        assertEquals(
            "<div style=\"display: flex; color: rebeccapurple; opacity: 0.5; width: 320px; height: auto; " +
                "padding: 8px 16px; margin: 4px 8px 12px 16px\"></div>",
            html,
        )
    }

    @Test
    fun rendersCssVariableAssignmentsAndReferences() {
        val html = composeHtmlToString {
            Div({
                style {
                    InlineVariables.spacing(16.px)
                    width(InlineVariables.spacing.value(8.px))
                }
            })
        }

        assertEquals(
            "<div style=\"width: var(--spacing, 8px); --spacing: 16px\"></div>",
            html,
        )
    }

    private object InlineVariables {
        val spacing by variable<CSSUnitValue>()
    }
}

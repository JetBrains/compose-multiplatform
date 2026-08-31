package org.jetbrains.compose.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.dom.Style
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.css.CSSStyleRule
import org.w3c.dom.css.CSSStyleSheet
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds

class HydrationStyleTest {
    @Test
    fun emptyStyleIsReused() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString { Style {} }
        val serverStyle = root.firstChild

        val composition = hydrateComposable(root) { Style {} }

        try {
            assertSame(serverStyle, root.firstChild)
            assertEquals("<style></style>", root.innerHTML)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun serverRenderedStyleIsReusedAndUpdatesAfterHydration() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        val target = document.createElement("div") as HTMLElement
        target.id = "hydration-style-handoff-target"
        var background by mutableStateOf(Color.green)
        document.body!!.appendChild(root)
        document.body!!.appendChild(target)
        root.innerHTML = composeHtmlToString {
            Style {
                "#${target.id}" style {
                    backgroundColor(background)
                }
            }
        }
        val serverStyle = root.firstChild as HTMLStyleElement
        val serverSheet = serverStyle.sheet as CSSStyleSheet

        assertEquals("rgb(0, 128, 0)", window.getComputedStyle(target).backgroundColor)

        val composition = hydrateComposable(root) {
            Style {
                "#${target.id}" style {
                    backgroundColor(background)
                }
            }
        }

        try {
            assertSame(serverStyle, root.firstChild)
            assertEquals(0, serverStyle.childNodes.length)
            assertEquals("", serverStyle.textContent)
            val hydratedSheet = serverStyle.sheet as CSSStyleSheet
            assertNotSame(serverSheet, hydratedSheet)
            assertEquals(
                "#${target.id} { background-color: green; }",
                hydratedSheet.cssRules.item(0)?.cssText,
            )
            assertEquals("rgb(0, 128, 0)", window.getComputedStyle(target).backgroundColor)

            background = Color.red
            delay(100.milliseconds)

            assertSame(serverStyle, root.firstChild)
            assertEquals(0, serverStyle.childNodes.length)
            assertSame(hydratedSheet, serverStyle.sheet)
            assertEquals(
                "#${target.id} { background-color: red; }",
                (serverStyle.sheet as CSSStyleSheet).cssRules.item(0)?.cssText,
            )
            assertEquals("rgb(255, 0, 0)", window.getComputedStyle(target).backgroundColor)
        } finally {
            composition.dispose()
            root.parentNode?.removeChild(root)
            target.parentNode?.removeChild(target)
        }
    }

    @Test
    fun detachedStyleKeepsSerializedCssCurrent() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        var textColor by mutableStateOf(Color.green)
        root.innerHTML = composeHtmlToString {
            Style {
                "body" style {
                    color(textColor)
                }
            }
        }
        val serverStyle = root.firstChild as HTMLStyleElement

        val composition = hydrateComposable(root) {
            Style {
                "body" style {
                    color(textColor)
                }
            }
        }

        try {
            assertSame(serverStyle, root.firstChild)
            assertEquals("body { color: green;}", serverStyle.textContent)

            textColor = Color.red
            delay(100.milliseconds)

            assertEquals("body { color: red;}", serverStyle.textContent)

            document.body!!.appendChild(root)
            textColor = Color.blue
            delay(100.milliseconds)

            assertEquals(0, serverStyle.childNodes.length)
            val hydratedSheet = serverStyle.sheet as CSSStyleSheet
            assertEquals(
                "body { color: blue; }",
                hydratedSheet.cssRules.item(0)?.cssText,
            )

            textColor = Color.green
            delay(100.milliseconds)

            assertSame(hydratedSheet, serverStyle.sheet)
            assertEquals(
                "body { color: green; }",
                hydratedSheet.cssRules.item(0)?.cssText,
            )
        } finally {
            composition.dispose()
            root.parentNode?.removeChild(root)
        }
    }

    @Test
    fun styleAddedAfterHydrationUsesCssom() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        var showStyle by mutableStateOf(false)
        document.body!!.appendChild(root)

        val composition = hydrateComposable(root) {
            if (showStyle) {
                Style {
                    "body" style {
                        color(Color.green)
                        property("content", "\"</style>\"")
                    }
                }
            }
        }

        try {
            showStyle = true
            delay(100.milliseconds)

            val style = root.firstChild as HTMLStyleElement
            val rule = (style.sheet as CSSStyleSheet).cssRules.item(0).unsafeCast<CSSStyleRule>()
            assertEquals(0, style.childNodes.length)
            assertEquals("green", rule.style.getPropertyValue("color"))
            assertEquals("\"</style>\"", rule.style.getPropertyValue("content"))
        } finally {
            composition.dispose()
            root.parentNode?.removeChild(root)
        }
    }

    @Test
    fun styleTextMismatchLeavesServerDomUntouched() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Style {
                "body" style {
                    color(Color.red)
                }
            }
        }
        val serverHtml = root.innerHTML
        val serverStyle = root.firstChild
        val serverCssText = serverStyle?.firstChild

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Style {
                    "body" style {
                        color(Color.blue)
                    }
                }
            }
        }

        assertContains(failure.message.orEmpty(), "expected raw text")
        assertSame(serverStyle, root.firstChild)
        assertSame(serverCssText, serverStyle?.firstChild)
        assertEquals(serverHtml, root.innerHTML)
    }
}
